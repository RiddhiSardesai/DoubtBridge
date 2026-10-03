package com.example.doubtbridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doubtbridge.adapters.DoubtAdapter;
import com.example.doubtbridge.models.Doubt;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class TeacherDashboardActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    DoubtAdapter adapter;
    List<Doubt> allDoubts  = new ArrayList<>();
    List<Doubt> displayList = new ArrayList<>();
    FirebaseFirestore db;
    FirebaseAuth auth;
    MaterialToolbar toolbar;
    TabLayout tabLayout;
    TextView emptyView, tvName, tvPendingCount, tvAnsweredCount, tvTotalCount, notifBadge, profileAvatar;
    com.google.android.material.button.MaterialButton btnUploadNotes, btnAnalytics, btnProfile, btnLogout;
    int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_dashboard);

        toolbar        = findViewById(R.id.toolbar);
        recyclerView   = findViewById(R.id.recyclerView);
        tabLayout      = findViewById(R.id.tabLayout);
        emptyView      = findViewById(R.id.emptyView);
        tvName         = findViewById(R.id.tvName);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvAnsweredCount = findViewById(R.id.tvAnsweredCount);
        tvTotalCount   = findViewById(R.id.tvTotalCount);
        btnUploadNotes = findViewById(R.id.btnUploadNotes);
        btnAnalytics   = findViewById(R.id.btnAnalytics);
        // btnProfile and btnLogout removed from here
        notifBadge     = findViewById(R.id.notifBadge);
        profileAvatar  = findViewById(R.id.profileAvatar);

        btnUploadNotes.setOnClickListener(v -> startActivity(new Intent(this, UploadNotesActivity.class)));
        btnAnalytics.setOnClickListener(v -> startActivity(new Intent(this, AnalyticsActivity.class)));

        // Logout from toolbar
        View btnLogoutIcon = findViewById(R.id.btnLogoutIcon);
        if (btnLogoutIcon != null) {
            btnLogoutIcon.setOnClickListener(v -> {
                auth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finishAffinity();
            });
        }

        // Notification bell
        View nb = findViewById(R.id.btnNotifications);
        if (nb != null) nb.setOnClickListener(v -> startActivity(new Intent(this, NotificationsActivity.class)));

        // Profile avatar circle
        if (profileAvatar != null)
            profileAvatar.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setSupportActionBar(toolbar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoubtAdapter(displayList, true);
        recyclerView.setAdapter(adapter);

        // Load user data
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();

            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    if (name != null && !name.isEmpty()) {
                        tvName.setText("Hello, " + name + " 👩‍🏫");
                        if (profileAvatar != null)
                            profileAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    }
                }
            });

            // Notification badge
            db.collection("notifications").addSnapshotListener((snap, e) -> {
                if (snap == null || notifBadge == null) return;
                int unread = 0;
                for (QueryDocumentSnapshot d : snap)
                    if (uid.equals(d.getString("userId")) && Boolean.FALSE.equals(d.getBoolean("isRead")))
                        unread++;
                if (unread > 0) {
                    notifBadge.setVisibility(View.VISIBLE);
                    notifBadge.setText(unread > 9 ? "9+" : String.valueOf(unread));
                } else {
                    notifBadge.setVisibility(View.GONE);
                }
            });
        }

        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("⏳ Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("✅ Answered"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t)   { currentTab = t.getPosition(); applyFilter(); }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });

        loadAllDoubts();
    }

    private void loadAllDoubts() {
        db.collection("doubts").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener((value, error) -> {
                if (value != null) {
                    allDoubts.clear();
                    int pending = 0, answered = 0;
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Doubt d = doc.toObject(Doubt.class);
                        if (d != null) {
                            d.setId(doc.getId());
                            allDoubts.add(d);
                            if ("answered".equals(d.getStatus())) answered++;
                            else pending++;
                        }
                    }
                    tvPendingCount.setText(String.valueOf(pending));
                    tvAnsweredCount.setText(String.valueOf(answered));
                    tvTotalCount.setText(String.valueOf(allDoubts.size()));
                    applyFilter();
                }
            });
    }

    private void applyFilter() {
        displayList.clear();
        for (Doubt d : allDoubts) {
            if (currentTab == 0) displayList.add(d);
            else if (currentTab == 1 && (d.getStatus() == null || "pending".equals(d.getStatus()))) displayList.add(d);
            else if (currentTab == 2 && "answered".equals(d.getStatus())) displayList.add(d);
        }
        adapter.notifyDataSetChanged();
        if (emptyView != null)
            emptyView.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Redundancy removed: Navigation is handled by on-screen buttons and profile avatar
        return false;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
