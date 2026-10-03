package com.example.doubtbridge;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doubtbridge.adapters.DoubtAdapter;
import com.example.doubtbridge.models.Doubt;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class BookmarkActivity extends AppCompatActivity {

    RecyclerView rv;
    LinearLayout emptyLayout;
    ImageButton btnBack;

    List<Doubt>  list = new ArrayList<>();
    Set<String>  bIds = new HashSet<>();
    DoubtAdapter adapter;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_bookmark);

        rv          = findViewById(R.id.recyclerView);
        emptyLayout = findViewById(R.id.emptyView);
        btnBack     = findViewById(R.id.btnBack);

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Start hidden — show after first data load
        rv.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.GONE);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DoubtAdapter(list, false, bIds);
        rv.setAdapter(adapter);

        loadBookmarks();
    }

    private void loadBookmarks() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showEmpty(true); return;
        }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Real-time listener, ALL bookmarks, filter by uid in-memory
        // No composite index needed
        FirebaseFirestore.getInstance()
            .collection("bookmarks")
            .addSnapshotListener((snap, error) -> {
                if (error != null || snap == null) { showEmpty(true); return; }

                list.clear();
                bIds.clear();

                for (DocumentSnapshot doc : snap.getDocuments()) {
                    // Only this user's bookmarks
                    if (!uid.equals(doc.getString("userId"))) continue;

                    String doubtId = doc.getString("doubtId");
                    String title   = doc.getString("title");

                    // Skip if no title or doubtId (shouldn't happen but be safe)
                    if (title == null || title.isEmpty()) continue;
                    if (doubtId == null || doubtId.isEmpty()) continue;

                    // Build a Doubt from stored bookmark data
                    Doubt d = new Doubt();
                    d.setId(doubtId);                                    // ← doubt's own ID
                    d.setTitle(title);
                    d.setDescription(doc.getString("description"));
                    d.setUserName(doc.getString("userName"));
                    d.setSubject(doc.getString("subject"));
                    d.setDepartment(doc.getString("department"));
                    d.setClassName(doc.getString("className"));
                    String st = doc.getString("status");
                    d.setStatus(st != null ? st : "pending");
                    Long ts = doc.getLong("timestamp");
                    if (ts != null) d.setTimestamp(ts);

                    list.add(d);
                    bIds.add(doubtId);  // ← same ID so star check works
                }

                // Sort newest bookmark first
                list.sort((a, c) -> Long.compare(c.getTimestamp(), a.getTimestamp()));

                // ✅ Set bookmarked IDs FIRST, then notify — so stars are yellow on first render
                adapter.setBookmarkedIds(new HashSet<>(bIds));

                showEmpty(list.isEmpty());
            });
    }

    private void showEmpty(boolean empty) {
        emptyLayout.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
