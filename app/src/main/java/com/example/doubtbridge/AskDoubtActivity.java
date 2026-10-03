package com.example.doubtbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.doubtbridge.models.Doubt;
import com.example.doubtbridge.models.Note;
import com.example.doubtbridge.utils.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

public class AskDoubtActivity extends AppCompatActivity {

    TextInputEditText titleInput, descInput;
    Spinner subjectSpinner, deptSpinner, classSpinner;
    MaterialButton submitBtn;
    ImageButton btnBack;
    
    // Image support
    MaterialCardView imagePickerZone;
    ImageView previewImage;
    View pickerTextLayout;
    String base64Image = null;

    FirebaseFirestore db;
    FirebaseAuth auth;

    final ActivityResultLauncher<Intent> imagePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null && r.getData().getData() != null) {
                    onImagePicked(r.getData().getData());
                }
            });

    final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null) {
                    Bundle extras = r.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        processAndSetImage(imageBitmap);
                    }
                }
            });

    final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) launchCamera();
                else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_ask_doubt);

        titleInput     = findViewById(R.id.titleInput);
        descInput      = findViewById(R.id.descInput);
        subjectSpinner = findViewById(R.id.subjectSpinner);
        deptSpinner    = findViewById(R.id.deptSpinner);
        classSpinner   = findViewById(R.id.classSpinner);
        submitBtn      = findViewById(R.id.submitBtn);
        btnBack        = findViewById(R.id.btnBack);
        
        imagePickerZone = findViewById(R.id.imagePickerZone);
        previewImage    = findViewById(R.id.previewImage);
        pickerTextLayout = findViewById(R.id.pickerTextLayout);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        
        imagePickerZone.setOnClickListener(v -> showImageSourceDialog());

        deptSpinner.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, dropFirst(AppConstants.DEPARTMENTS)));
        classSpinner.setAdapter(new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_dropdown_item, dropFirst(AppConstants.CLASSES)));

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSubjects();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        deptSpinner.setOnItemSelectedListener(listener);
        classSpinner.setOnItemSelectedListener(listener);

        prefillFromProfile();
        submitBtn.setOnClickListener(v -> postDoubt());
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            launchCamera();
                        } else {
                            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
                        }
                    } else if (which == 1) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        imagePicker.launch(Intent.createChooser(intent, "Select Doubt Image"));
                    }
                }).show();
    }

    private void launchCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    private void onImagePicked(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                processAndSetImage(bitmap);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void processAndSetImage(Bitmap bitmap) {
        // Resize for Firestore (keep it under 1MB)
        Bitmap resized = resizeBitmap(bitmap, 800);
        previewImage.setImageBitmap(resized);
        previewImage.setVisibility(View.VISIBLE);
        pickerTextLayout.setVisibility(View.GONE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        byte[] bytes = baos.toByteArray();
        base64Image = Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private Bitmap resizeBitmap(Bitmap source, int maxSize) {
        int width = source.getWidth();
        int height = source.getHeight();
        float ratio = (float) width / (float) height;
        if (width > height) {
            width = maxSize;
            height = (int) (width / ratio);
        } else {
            height = maxSize;
            width = (int) (height * ratio);
        }
        return Bitmap.createScaledBitmap(source, width, height, true);
    }

    private void prefillFromProfile() {
        if (auth.getCurrentUser() == null) return;
        db.collection("users").document(auth.getCurrentUser().getUid()).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) return;
                selectSpinner(deptSpinner, doc.getString("department"));
                selectSpinner(classSpinner, doc.getString("className"));
                updateSubjects();
            });
    }

    private void updateSubjects() {
        String dept = deptSpinner.getSelectedItem() != null ? deptSpinner.getSelectedItem().toString() : "";
        String cls = classSpinner.getSelectedItem() != null ? classSpinner.getSelectedItem().toString() : "";
        List<String> subjects = AppConstants.getSubjects(dept, cls);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, subjects);
        subjectSpinner.setAdapter(adapter);
    }

    private void selectSpinner(Spinner sp, String val) {
        if (val == null) return;
        for (int i = 0; i < sp.getCount(); i++)
            if (sp.getItemAtPosition(i).toString().equals(val)) { sp.setSelection(i); break; }
    }

    private void postDoubt() {
        String title   = t(titleInput);
        String desc    = t(descInput);
        String subject = subjectSpinner.getSelectedItem().toString();
        String dept    = deptSpinner.getSelectedItem().toString();
        String cls     = classSpinner.getSelectedItem().toString();

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this,"Please fill title and description",Toast.LENGTH_SHORT).show(); return;
        }
        submitBtn.setEnabled(false);
        submitBtn.setText("Submitting…");

        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
        db.collection("users").document(uid).get().addOnSuccessListener(u -> {
            String name = (u.exists() && u.getString("name") != null)
                ? u.getString("name")
                : (auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "Anonymous");

            Doubt doubt = new Doubt(title, desc, name, uid, subject, dept, cls, base64Image);
            db.collection("doubts").add(doubt)
                .addOnSuccessListener(ref -> autoMatch(doubt, ref.getId()))
                .addOnFailureListener(e -> {
                    submitBtn.setEnabled(true); submitBtn.setText("Post Doubt");
                    Toast.makeText(this,"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                });
        });
    }

    private void autoMatch(Doubt doubt, String doubtId) {
        // Fetch notes for this subject — no orderBy, no composite index needed
        db.collection("notes").whereEqualTo("subject", doubt.getSubject()).get()
            .addOnSuccessListener(snap -> {
                List<Note> notes = new ArrayList<>();
                for (QueryDocumentSnapshot nd : snap) {
                    Note n = nd.toObject(Note.class); n.setId(nd.getId()); notes.add(n);
                }

                String ragContext = KeywordMatcher.extractRAGContext(doubt, notes);
                Note bestNote = KeywordMatcher.findBestMatch(doubt, notes, AppConstants.MATCH_THRESHOLD);
                String sourceNoteId = (bestNote != null) ? bestNote.getId() : null;

                if (!ragContext.isEmpty()) {
                    // Try RAG first
                    GeminiRAGHelper ragHelper = new GeminiRAGHelper();
                    ragHelper.generateAnswer(doubt.getTitle() + ": " + doubt.getDescription(), ragContext, new GeminiRAGHelper.RAGCallback() {
                        @Override
                        public void onSuccess(String answer) {
                            runOnUiThread(() -> saveAutoAnswer(doubtId, doubt, answer, "AI Assistant (RAG)", sourceNoteId));
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            // Fallback to traditional Keyword Match
                            runOnUiThread(() -> fallbackToKeywordMatch(doubt, notes, doubtId));
                        }
                    });
                } else {
                    Toast.makeText(this,"Doubt posted! Teacher will answer soon. 📬",Toast.LENGTH_LONG).show();
                    finish();
                }
            })
            .addOnFailureListener(e -> { Toast.makeText(this,"Doubt posted!",Toast.LENGTH_SHORT).show(); finish(); });
    }

    private void fallbackToKeywordMatch(Doubt doubt, List<Note> notes, String doubtId) {
        Note match = KeywordMatcher.findBestMatch(doubt, notes, AppConstants.MATCH_THRESHOLD);
        if (match != null) {
            String ans = KeywordMatcher.extractRelevantAnswer(doubt, match);
            if (ans.isEmpty()) ans = KeywordMatcher.trimContent(match.getContent(), 800);
            saveAutoAnswer(doubtId, doubt, ans, "Auto-matched from: " + match.getTitle(), match.getId());
        } else {
            Toast.makeText(this,"Doubt posted! Teacher will answer soon. 📬",Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void saveAutoAnswer(String doubtId, Doubt doubt, String answer, String source, String sourceNoteId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "answered");
        updates.put("answer", answer);
        updates.put("answeredBy", source);
        updates.put("answeredAt", System.currentTimeMillis());
        if (sourceNoteId != null) updates.put("sourceNoteId", sourceNoteId);

        db.collection("doubts").document(doubtId).update(updates).addOnCompleteListener(task -> {
            NotificationHelper.sendNotification(doubt.getUserId(),
                "✅ Instant Answer Found!",
                "\""+doubt.getTitle()+"\" was answered using " + (source.contains("AI") ? "AI Precision" : "matched notes") + ".",
                "answer", doubtId);
            if (!isFinishing()) {
                Toast.makeText(this,"✅ Precision answer found!",Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private String t(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }
    private static String[] dropFirst(String[] arr) {
        String[] o = new String[arr.length-1]; System.arraycopy(arr,1,o,0,o.length); return o;
    }
}
