package com.example.doubtbridge;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.doubtbridge.models.Note;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class NoteDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvMeta, tvContent, tvUploader;
    private Chip chipSubject;
    private LinearLayout fileSection;
    private MaterialButton btnOpenFile;

    private FirebaseFirestore db;
    private String noteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        tvTitle = findViewById(R.id.tvTitle);
        tvMeta = findViewById(R.id.tvMeta);
        tvContent = findViewById(R.id.tvContent);
        tvUploader = findViewById(R.id.tvUploader);
        chipSubject = findViewById(R.id.chipSubject);
        fileSection = findViewById(R.id.fileSection);
        btnOpenFile = findViewById(R.id.btnOpenFile);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);

        db = FirebaseFirestore.getInstance();
        noteId = getIntent().getStringExtra("noteId");

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        if (noteId == null) {
            Toast.makeText(this, "Error: Note ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadNoteDetails();
    }

    private void loadNoteDetails() {
        db.collection("notes").document(noteId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Note note = documentSnapshot.toObject(Note.class);
                        if (note != null) {
                            displayNote(note);
                        }
                    } else {
                        Toast.makeText(this, "Note not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void displayNote(Note note) {
        tvTitle.setText(note.getTitle());
        chipSubject.setText(note.getSubject());
        
        String meta = "";
        if (note.getDepartment() != null) meta += note.getDepartment();
        if (note.getClassName() != null) meta += (meta.isEmpty() ? "" : " · ") + note.getClassName();
        tvMeta.setText(meta);

        tvContent.setText(note.getContent());

        if (note.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String date = sdf.format(note.getTimestamp());
            tvUploader.setText(String.format("Uploaded by %s on %s", note.getUploaderName(), date));
        } else {
            tvUploader.setText(String.format("Uploaded by %s", note.getUploaderName()));
        }

        if (note.getFileBase64() != null && !note.getFileBase64().isEmpty()) {
            fileSection.setVisibility(View.VISIBLE);
            String type = note.getFileType() != null ? note.getFileType().toUpperCase() : "DOCUMENT";
            btnOpenFile.setText(String.format("View Original %s", type));
            btnOpenFile.setOnClickListener(v -> openBase64File(note.getFileBase64(), note.getFileType(), note.getTitle()));
        } else if (note.getFileUrl() != null && !note.getFileUrl().isEmpty()) {
            fileSection.setVisibility(View.VISIBLE);
            String type = note.getFileType() != null ? note.getFileType().toUpperCase() : "DOCUMENT";
            btnOpenFile.setText(String.format("View Original %s", type));
            btnOpenFile.setOnClickListener(v -> {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(note.getFileUrl())));
                } catch (Exception e) {
                    Toast.makeText(this, "No app found to open link", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            fileSection.setVisibility(View.GONE);
        }
    }

    private void openBase64File(String base64String, String fileType, String title) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            String extension = ".pdf";
            String mimeType = "application/pdf";

            if (fileType != null) {
                String lo = fileType.toLowerCase();
                if (lo.contains("ppt")) {
                    extension = ".pptx";
                    mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                } else if (lo.contains("text") || lo.contains("txt")) {
                    extension = ".txt";
                    mimeType = "text/plain";
                } else if (lo.contains("doc")) {
                    extension = ".docx";
                    mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                }
            }

            File cacheFile = new File(getCacheDir(), title.replaceAll("[^a-zA-Z0-9]", "_") + "_" + System.currentTimeMillis() + extension);
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                fos.write(decodedBytes);
                fos.flush();
            }

            Uri fileUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", cacheFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Please install a " + extension.toUpperCase().substring(1) + " viewer app", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error opening file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
