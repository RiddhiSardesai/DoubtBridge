package com.example.doubtbridge;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doubtbridge.adapters.NotificationAdapter;
import com.example.doubtbridge.models.NotificationModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class NotificationsActivity extends AppCompatActivity {

    MaterialToolbar toolbar;
    RecyclerView rv;
    View emptyView;
    List<NotificationModel> list = new ArrayList<>();
    NotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_notifications);

        toolbar   = findViewById(R.id.toolbar);
        rv        = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(list);
        rv.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Real-time listener — fetch all, filter by userId in-memory
        // This avoids ALL composite index requirements
        FirebaseFirestore.getInstance()
            .collection("notifications")
            .addSnapshotListener((snap, error) -> {
                if (error != null || snap == null) {
                    emptyView.setVisibility(View.VISIBLE);
                    return;
                }

                list.clear();
                List<String> toMarkRead = new ArrayList<>();

                for (QueryDocumentSnapshot doc : snap) {
                    // Filter by this user
                    String docUserId = doc.getString("userId");
                    if (!uid.equals(docUserId)) continue;

                    NotificationModel n = new NotificationModel();
                    n.setId(doc.getId());
                    n.setUserId(uid);
                    n.setTitle(nvl(doc.getString("title"), "New notification"));
                    n.setMessage(nvl(doc.getString("message"), ""));
                    n.setType(nvl(doc.getString("type"), "answer"));
                    n.setDoubtId(doc.getString("doubtId"));
                    // Read isRead field safely — may be null for old docs
                    Boolean read = doc.getBoolean("isRead");
                    n.setRead(Boolean.TRUE.equals(read));
                    Long ts = doc.getLong("timestamp");
                    n.setTimestamp(ts != null ? ts : 0L);

                    list.add(n);

                    // Collect unread IDs to mark read
                    if (!n.isRead()) toMarkRead.add(doc.getId());
                }

                // Sort: newest first
                Collections.sort(list, (a, c) -> Long.compare(c.getTimestamp(), a.getTimestamp()));

                emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                rv.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
                adapter.notifyDataSetChanged();

                // Mark all as read in Firestore (batch)
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                WriteBatch batch = db.batch();
                for (String id : toMarkRead)
                    batch.update(db.collection("notifications").document(id), "isRead", true);
                if (!toMarkRead.isEmpty()) batch.commit();
            });
    }

    private static String nvl(String s, String def) { return s != null && !s.isEmpty() ? s : def; }
}
