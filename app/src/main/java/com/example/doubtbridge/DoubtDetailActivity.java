package com.example.doubtbridge;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.doubtbridge.models.Doubt;
import com.example.doubtbridge.utils.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DoubtDetailActivity extends AppCompatActivity {

    TextView tvTitle, tvDesc, tvSubject, tvStatus, tvAnswer, tvAskedBy, tvFrequency, tvDeptClass, btnReport;
    MaterialCardView answerCard, imageCard;
    ImageView ivDoubtImage;
    FloatingActionButton bookmarkFab;
    MaterialToolbar toolbar;
    Chip statusChip;

    FirebaseFirestore db;
    FirebaseAuth auth;
    String doubtId;
    Doubt currentDoubt;
    boolean isBookmarked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doubt_detail);

        tvTitle      = findViewById(R.id.tvTitle);
        tvDesc       = findViewById(R.id.tvDescription);
        tvSubject    = findViewById(R.id.tvSubject);
        tvStatus     = findViewById(R.id.tvStatus);
        tvAnswer     = findViewById(R.id.tvAnswer);
        tvAskedBy    = findViewById(R.id.tvAskedBy);
        tvFrequency  = findViewById(R.id.tvFrequency);
        tvDeptClass  = findViewById(R.id.tvDeptClass);
        btnReport    = findViewById(R.id.btnReport);
        answerCard   = findViewById(R.id.answerCard);
        imageCard    = findViewById(R.id.imageCard);
        ivDoubtImage = findViewById(R.id.ivDoubtImage);
        bookmarkFab  = findViewById(R.id.bookmarkFab);
        toolbar      = findViewById(R.id.toolbar);
        statusChip   = findViewById(R.id.statusChip);

        db      = FirebaseFirestore.getInstance();
        auth    = FirebaseAuth.getInstance();
        doubtId = getIntent().getStringExtra("doubtId");

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Set initial FAB icon (outline star)
        bookmarkFab.setImageResource(R.drawable.ic_star_outline);
        bookmarkFab.clearColorFilter();

        bookmarkFab.setOnClickListener(v -> toggleBookmark());

        loadDoubt();
        checkIfBookmarked();
    }

    private void loadDoubt() {
        if (doubtId == null) return;
        // Increment view count
        db.collection("doubts").document(doubtId)
            .update("frequency", com.google.firebase.firestore.FieldValue.increment(1));

        db.collection("doubts").document(doubtId).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) return;
                currentDoubt = doc.toObject(Doubt.class);
                if (currentDoubt != null) {
                    currentDoubt.setId(doc.getId());
                    displayDoubt();
                }
            });
    }

    private void displayDoubt() {
        if (currentDoubt == null) return;
        tvTitle.setText(currentDoubt.getTitle());
        tvDesc.setText(currentDoubt.getDescription());

        // Handle Image
        if (currentDoubt.getImageUrl() != null && !currentDoubt.getImageUrl().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(currentDoubt.getImageUrl(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivDoubtImage.setImageBitmap(decodedByte);
                imageCard.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                imageCard.setVisibility(View.GONE);
            }
        } else {
            imageCard.setVisibility(View.GONE);
        }

        tvSubject.setText("📚 " + (currentDoubt.getSubject() != null ? currentDoubt.getSubject() : "General"));
        tvAskedBy.setText("Asked by: " + (currentDoubt.getUserName() != null ? currentDoubt.getUserName() : "Anonymous"));
        tvFrequency.setText("👀 " + currentDoubt.getFrequency() + " students viewed this");

        // Dept + Class
        if (tvDeptClass != null) {
            String dc = "";
            if (currentDoubt.getDepartment() != null && !currentDoubt.getDepartment().isEmpty())
                dc += "🏫 " + currentDoubt.getDepartment();
            if (currentDoubt.getClassName() != null && !currentDoubt.getClassName().isEmpty())
                dc += (dc.isEmpty() ? "" : "  ") + "📋 " + currentDoubt.getClassName();
            tvDeptClass.setText(dc);
            tvDeptClass.setVisibility(dc.isEmpty() ? View.GONE : View.VISIBLE);
        }

        if ("answered".equals(currentDoubt.getStatus())) {
            statusChip.setText("✅ Answered");
            statusChip.setChipBackgroundColorResource(R.color.success_container);
            statusChip.setTextColor(ContextCompat.getColor(this, R.color.on_success_container));
            answerCard.setVisibility(View.VISIBLE);
            tvAnswer.setText(currentDoubt.getAnswer() != null ? currentDoubt.getAnswer() : "");
            
            if (btnReport != null) {
                btnReport.setVisibility(View.VISIBLE);
                btnReport.setOnClickListener(v -> reportAnswer());
            }

            String answeredBy = currentDoubt.getAnsweredBy() != null ? currentDoubt.getAnsweredBy() : "";
            tvStatus.setText("By: " + answeredBy);

            // If auto-answered, show link to source note
            if (currentDoubt.getSourceNoteId() != null && !currentDoubt.getSourceNoteId().isEmpty()) {
                tvStatus.setText(answeredBy + " (Click to view source)");
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.primary));
                tvStatus.setOnClickListener(v -> {
                    Intent intent = new Intent(this, NoteDetailActivity.class);
                    intent.putExtra("noteId", currentDoubt.getSourceNoteId());
                    startActivity(intent);
                });
            } else {
                tvStatus.setOnClickListener(null);
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            }
        } else {
            statusChip.setText("⏳ Pending");
            statusChip.setChipBackgroundColorResource(R.color.warning_container);
            statusChip.setTextColor(ContextCompat.getColor(this, R.color.on_warning_container));
            answerCard.setVisibility(View.GONE);
            tvStatus.setText("Waiting for teacher's answer...");
        }

        if (currentDoubt.isImportant() && getSupportActionBar() != null)
            getSupportActionBar().setSubtitle("⭐ Marked Important by Teacher");
    }

    private void checkIfBookmarked() {
        if (auth.getCurrentUser() == null || doubtId == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("bookmarks")
            .whereEqualTo("userId", uid)
            .whereEqualTo("doubtId", doubtId)
            .get()
            .addOnSuccessListener(q -> {
                isBookmarked = !q.isEmpty();
                updateFabStar();
            });
    }

    private void toggleBookmark() {
        if (auth.getCurrentUser() == null || currentDoubt == null) return;
        if (isBookmarked) {
            removeBookmark();
        } else {
            addBookmark();
        }
    }

    private void addBookmark() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        Map<String, Object> bm = new HashMap<>();
        bm.put("userId",      uid);
        bm.put("doubtId",     doubtId);
        bm.put("title",       currentDoubt.getTitle());
        bm.put("description", currentDoubt.getDescription());
        bm.put("userName",    currentDoubt.getUserName());
        bm.put("subject",     currentDoubt.getSubject());
        bm.put("department",  currentDoubt.getDepartment());
        bm.put("className",   currentDoubt.getClassName());
        bm.put("status",      currentDoubt.getStatus());
        bm.put("timestamp",   System.currentTimeMillis());

        // Optimistic
        isBookmarked = true;
        updateFabStar();

        db.collection("bookmarks").add(bm)
            .addOnSuccessListener(r -> Toast.makeText(this, "⭐ Bookmarked!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> { isBookmarked = false; updateFabStar(); });
    }

    private void removeBookmark() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        // Optimistic
        isBookmarked = false;
        updateFabStar();

        db.collection("bookmarks")
            .whereEqualTo("userId", uid)
            .whereEqualTo("doubtId", doubtId)
            .get()
            .addOnSuccessListener(q -> {
                for (com.google.firebase.firestore.DocumentSnapshot doc : q.getDocuments())
                    doc.getReference().delete();
                Toast.makeText(this, "Bookmark removed", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> { isBookmarked = true; updateFabStar(); });
    }

    private void updateFabStar() {
        bookmarkFab.clearColorFilter();
        bookmarkFab.setImageResource(
            isBookmarked ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
    }

    private void reportAnswer() {
        if (currentDoubt == null || doubtId == null) return;
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Report Incorrect Answer")
            .setMessage("Is this answer irrelevant or incorrect? Reporting it will notify the teaching staff to review and update it.")
            .setPositiveButton("Report", (dialog, which) -> {
                db.collection("doubts").document(doubtId)
                    .update("status", "pending", "reportCount", com.google.firebase.firestore.FieldValue.increment(1))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Answer reported. Teachers will review it.", Toast.LENGTH_LONG).show();
                        
                        // Notify uploader if it was auto-matched or a specific teacher
                        // For now, general notification to relevant department
                        NotificationHelper.sendNotification("teacher_channel", 
                            "🚩 Answer Reported", 
                            "An answer for \"" + currentDoubt.getTitle() + "\" was flagged as incorrect.", 
                            "report", doubtId);
                        
                        finish();
                    });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
