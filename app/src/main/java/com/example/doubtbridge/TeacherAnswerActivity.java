package com.example.doubtbridge;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doubtbridge.models.Doubt;
import com.example.doubtbridge.utils.NotificationHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TeacherAnswerActivity extends AppCompatActivity {

    TextView tvTitle, tvDesc, tvSubject, tvAskedBy, tvDeptClass;
    TextInputEditText answerInput;
    MaterialButton submitBtn;
    SwitchMaterial importantSwitch;
    MaterialToolbar toolbar;
    Chip statusChip;
    
    MaterialCardView imageCard;
    ImageView ivDoubtImage;

    FirebaseFirestore db;
    String doubtId;
    Doubt currentDoubt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_answer);

        tvTitle       = findViewById(R.id.tvTitle);
        tvDesc        = findViewById(R.id.tvDescription);
        tvSubject     = findViewById(R.id.tvSubject);
        tvAskedBy     = findViewById(R.id.tvAskedBy);
        tvDeptClass   = findViewById(R.id.tvDeptClass);
        answerInput   = findViewById(R.id.answerInput);
        submitBtn     = findViewById(R.id.submitBtn);
        importantSwitch = findViewById(R.id.importantSwitch);
        toolbar       = findViewById(R.id.toolbar);
        statusChip    = findViewById(R.id.statusChip);
        imageCard     = findViewById(R.id.imageCard);
        ivDoubtImage  = findViewById(R.id.ivDoubtImage);

        db = FirebaseFirestore.getInstance();
        doubtId = getIntent().getStringExtra("doubtId");

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        loadDoubt();
        submitBtn.setOnClickListener(v -> submitAnswer());
    }

    private void loadDoubt() {
        if (doubtId == null) return;
        db.collection("doubts").document(doubtId).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) return;
                currentDoubt = doc.toObject(Doubt.class);
                currentDoubt.setId(doc.getId());

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

                tvSubject.setText("📚 " + currentDoubt.getSubject());
                tvAskedBy.setText("👤 Asked by: " + currentDoubt.getUserName());

                String deptCls = "";
                if (currentDoubt.getDepartment() != null) deptCls += "🏫 " + currentDoubt.getDepartment();
                if (currentDoubt.getClassName() != null)  deptCls += "  📋 " + currentDoubt.getClassName();
                if (tvDeptClass != null) tvDeptClass.setText(deptCls);

                importantSwitch.setChecked(currentDoubt.isImportant());

                if ("answered".equals(currentDoubt.getStatus())) {
                    statusChip.setText("✅ Already Answered");
                    statusChip.setChipBackgroundColorResource(R.color.success_container);
                    statusChip.setTextColor(getResources().getColor(R.color.on_success_container));
                    if (currentDoubt.getAnswer() != null) {
                        answerInput.setText(currentDoubt.getAnswer());
                    }
                } else {
                    statusChip.setText("⏳ Pending");
                    statusChip.setChipBackgroundColorResource(R.color.warning_container);
                    statusChip.setTextColor(getResources().getColor(R.color.on_warning_container));
                }
            });
    }

    private void submitAnswer() {
        String answer = answerInput.getText() != null ? answerInput.getText().toString().trim() : "";
        if (answer.isEmpty()) {
            Toast.makeText(this, "Please write an answer", Toast.LENGTH_SHORT).show();
            return;
        }

        submitBtn.setEnabled(false);
        submitBtn.setText("Saving...");

        String teacherUid = FirebaseAuth.getInstance().getCurrentUser() != null
            ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        db.collection("users").document(teacherUid).get()
            .addOnSuccessListener(userDoc -> {
                String teacherName = userDoc.exists() && userDoc.getString("name") != null
                    ? userDoc.getString("name") : "Teacher";
                boolean markImportant = importantSwitch.isChecked();

                Map<String, Object> updates = new HashMap<>();
                updates.put("status", "answered");
                updates.put("answer", answer);
                updates.put("answeredBy", teacherName);
                updates.put("isImportant", markImportant);
                updates.put("answeredAt", System.currentTimeMillis());

                db.collection("doubts").document(doubtId).update(updates)
                    .addOnSuccessListener(unused -> {
                        // Notify student
                        if (currentDoubt != null && currentDoubt.getUserId() != null) {
                            String notifTitle = markImportant
                                ? "⭐ Your doubt was answered & marked important!"
                                : "✅ Your doubt was answered!";
                            NotificationHelper.sendNotification(
                                currentDoubt.getUserId(),
                                notifTitle,
                                "\"" + currentDoubt.getTitle() + "\" — answered by " + teacherName,
                                markImportant ? "important" : "answer",
                                doubtId
                            );
                        }
                        Toast.makeText(this, "Answer submitted! ✅", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        submitBtn.setEnabled(true);
                        submitBtn.setText("Submit Answer");
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            });
    }
}
