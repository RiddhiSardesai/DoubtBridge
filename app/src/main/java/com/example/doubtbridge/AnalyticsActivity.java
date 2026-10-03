package com.example.doubtbridge;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsActivity extends AppCompatActivity {

    TextView tvTotalDoubts, tvPending, tvAnswered, tvTopSubject, tvTopQuestion,
             tvAutoAnswered, tvManualAnswered, tvEngagementRate;
    MaterialToolbar toolbar;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        toolbar = findViewById(R.id.toolbar);
        tvTotalDoubts = findViewById(R.id.tvTotalDoubts);
        tvPending = findViewById(R.id.tvPending);
        tvAnswered = findViewById(R.id.tvAnswered);
        tvTopSubject = findViewById(R.id.tvTopSubject);
        tvTopQuestion = findViewById(R.id.tvTopQuestion);
        tvAutoAnswered = findViewById(R.id.tvAutoAnswered);
        tvManualAnswered = findViewById(R.id.tvManualAnswered);
        tvEngagementRate = findViewById(R.id.tvEngagementRate);

        db = FirebaseFirestore.getInstance();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        loadAnalytics();
    }

    private void loadAnalytics() {
        db.collection("doubts").get()
            .addOnSuccessListener(snap -> {
                int total = snap.size();
                int pending = 0, answered = 0, autoAnswered = 0, manualAnswered = 0;
                Map<String, Integer> subjectCount = new HashMap<>();
                String topQuestion = "";
                long topFreq = 0;

                for (QueryDocumentSnapshot doc : snap) {
                    String status = doc.getString("status");
                    String subject = doc.getString("subject");
                    String answeredBy = doc.getString("answeredBy");
                    Long freq = doc.getLong("frequency");
                    String title = doc.getString("title");

                    if ("pending".equals(status)) pending++;
                    else {
                        answered++;
                        if (answeredBy != null && answeredBy.startsWith("Auto-matched")) autoAnswered++;
                        else manualAnswered++;
                    }

                    if (subject != null) {
                        subjectCount.put(subject, subjectCount.getOrDefault(subject, 0) + 1);
                    }
                    if (freq != null && freq > topFreq) {
                        topFreq = freq;
                        topQuestion = title != null ? title : "";
                    }
                }

                // Find most doubted subject
                String topSubject = "None";
                int topCount = 0;
                for (Map.Entry<String, Integer> entry : subjectCount.entrySet()) {
                    if (entry.getValue() > topCount) {
                        topCount = entry.getValue();
                        topSubject = entry.getKey();
                    }
                }

                double engagementRate = total > 0 ? (answered / (double) total) * 100 : 0;

                tvTotalDoubts.setText(String.valueOf(total));
                tvPending.setText(String.valueOf(pending));
                tvAnswered.setText(String.valueOf(answered));
                tvAutoAnswered.setText(String.valueOf(autoAnswered));
                tvManualAnswered.setText(String.valueOf(manualAnswered));
                tvTopSubject.setText(topSubject + " (" + topCount + " doubts)");
                tvTopQuestion.setText(topQuestion.isEmpty() ? "N/A" : topQuestion);
                tvEngagementRate.setText(String.format("%.1f%%", engagementRate));
            });
    }
}
