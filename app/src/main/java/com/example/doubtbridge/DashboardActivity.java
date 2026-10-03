package com.example.doubtbridge;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doubtbridge.adapters.DoubtAdapter;
import com.example.doubtbridge.models.Doubt;
import com.example.doubtbridge.utils.AppConstants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class DashboardActivity extends AppCompatActivity {

    RecyclerView rv;
    ExtendedFloatingActionButton fab;
    EditText searchInput;
    Spinner deptFilter, classFilter;
    TabLayout tabLayout;
    TextView emptyView, tvUserName, notifBadge, profileAvatar;
    MaterialButton btnViewNotes, btnBookmarks, btnProfile, btnLogout;

    FirebaseFirestore db;
    FirebaseAuth auth;
    List<Doubt> all     = new ArrayList<>();
    List<Doubt> display = new ArrayList<>();
    Set<String> bIds    = new HashSet<>();
    DoubtAdapter adapter;

    String curDept = "All Departments", curClass = "All Classes", q = "";
    int tab = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_student_dashboard);

        // Toolbar for overflow menu
        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        rv            = findViewById(R.id.recyclerView);
        fab           = findViewById(R.id.fab);
        searchInput   = findViewById(R.id.searchInput);
        deptFilter    = findViewById(R.id.deptFilter);
        classFilter   = findViewById(R.id.classFilter);
        tabLayout     = findViewById(R.id.tabLayout);
        emptyView     = findViewById(R.id.emptyView);
        tvUserName    = findViewById(R.id.userName);
        notifBadge    = findViewById(R.id.notifBadge);
        profileAvatar = findViewById(R.id.profileAvatar);
        btnViewNotes  = findViewById(R.id.btnViewNotes);
        btnBookmarks  = findViewById(R.id.btnBookmarks);
        // btnProfile and btnLogout removed from here as they are now in toolbar

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoubtAdapter(display, false, bIds);
        rv.setAdapter(adapter);

        // ── Navigation buttons (always visible on screen) ──
        btnViewNotes.setOnClickListener(v -> startActivity(new Intent(this, ViewNotesActivity.class)));
        btnBookmarks.setOnClickListener(v -> startActivity(new Intent(this, BookmarkActivity.class)));

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

        // Profile avatar circle → Profile
        if (profileAvatar != null)
            profileAvatar.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));

        // ── Load user data ──
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();

            // User name + avatar letter
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                if (!doc.exists()) return;
                String name = doc.getString("name");
                if (name != null && !name.isEmpty()) {
                    tvUserName.setText(name);
                    if (profileAvatar != null)
                        profileAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
                }
            });

            // Bookmark IDs — real-time, filter by uid in-memory
            db.collection("bookmarks").addSnapshotListener((snap, e) -> {
                if (snap == null) return;
                bIds.clear();
                for (DocumentSnapshot d : snap.getDocuments())
                    if (uid.equals(d.getString("userId"))) {
                        String did = d.getString("doubtId");
                        if (did != null) bIds.add(did);
                    }
                adapter.setBookmarkedIds(new HashSet<>(bIds));
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

        // ── Tabs ──
        tabLayout.addTab(tabLayout.newTab().setText("All"));
        tabLayout.addTab(tabLayout.newTab().setText("Answered"));
        tabLayout.addTab(tabLayout.newTab().setText("Pending"));
        tabLayout.addTab(tabLayout.newTab().setText("⭐ Important"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t)   { tab = t.getPosition(); filter(); }
            @Override public void onTabUnselected(TabLayout.Tab t) {}
            @Override public void onTabReselected(TabLayout.Tab t) {}
        });

        // ── Spinners ──
        deptFilter.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, AppConstants.DEPARTMENTS));
        deptFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                curDept = AppConstants.DEPARTMENTS[pos]; filter();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        classFilter.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, AppConstants.CLASSES));
        classFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                curClass = AppConstants.CLASSES[pos]; filter();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        // ── Search ──
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int bc, int c) {
                q = s.toString().toLowerCase().trim(); filter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fab.setOnClickListener(v -> startActivity(new Intent(this, AskDoubtActivity.class)));
        loadDoubts();
    }

    private void loadDoubts() {
        db.collection("doubts").addSnapshotListener((value, error) -> {
            if (value == null) return;
            all.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                Doubt d = doc.toObject(Doubt.class);
                if (d != null) { d.setId(doc.getId()); all.add(d); }
            }
            Collections.sort(all, (a, c) -> Long.compare(c.getTimestamp(), a.getTimestamp()));
            filter();
        });
    }

    private void filter() {
        display.clear();
        for (Doubt d : all) {
            boolean t = true;
            if      (tab == 1) t = "answered".equals(d.getStatus());
            else if (tab == 2) t = d.getStatus() == null || "pending".equals(d.getStatus());
            else if (tab == 3) t = d.isImportant();
            boolean dp = curDept.startsWith("All") ||
                (d.getDepartment() != null && d.getDepartment().equalsIgnoreCase(curDept));
            boolean cl = curClass.startsWith("All") ||
                (d.getClassName() != null && d.getClassName().equalsIgnoreCase(curClass));
            boolean sq = q.isEmpty() ||
                (d.getTitle() != null && d.getTitle().toLowerCase().contains(q)) ||
                (d.getDescription() != null && d.getDescription().toLowerCase().contains(q));
            if (t && dp && cl && sq) display.add(d);
        }
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(display.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Redundancy removed: Navigation is handled by on-screen buttons and profile avatar
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
