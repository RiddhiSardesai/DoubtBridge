package com.example.doubtbridge;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doubtbridge.adapters.NoteAdapter;
import com.example.doubtbridge.models.Doubt;
import com.example.doubtbridge.models.Note;
import com.example.doubtbridge.utils.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import android.util.Base64;
import java.io.InputStream;
import java.util.*;

public class UploadNotesActivity extends AppCompatActivity {

    TextInputEditText titleInput, contentPreview;
    Spinner subjectSpinner, deptSpinner, classSpinner;
    MaterialButton uploadBtn;
    RecyclerView notesRecycler;
    MaterialToolbar toolbar;
    MaterialCardView uploadZone;
    TextView uploadZoneText, tvFileName, tvExtractionStatus, tvWordCount, tvPageCount;
    LinearProgressIndicator uploadProgress;

    FirebaseFirestore db;
    FirebaseAuth auth;
    List<Note> notesList = new ArrayList<>();

    Uri    fileUri       = null;
    byte[] fileBytes     = null; // To store file data locally for Firestore
    String fileName      = "";
    String fileType      = "text";
    String extracted     = "";   // full text extracted from document

    final ActivityResultLauncher<Intent> picker = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == Activity.RESULT_OK
                    && r.getData() != null && r.getData().getData() != null)
                onFilePicked(r.getData().getData());
        });

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_upload_notes);

        toolbar            = findViewById(R.id.toolbar);
        titleInput         = findViewById(R.id.titleInput);
        contentPreview     = findViewById(R.id.contentInput);
        subjectSpinner     = findViewById(R.id.subjectSpinner);
        deptSpinner        = findViewById(R.id.deptSpinner);
        classSpinner       = findViewById(R.id.classSpinner);
        uploadBtn          = findViewById(R.id.uploadBtn);
        notesRecycler      = findViewById(R.id.notesRecycler);
        uploadZone         = findViewById(R.id.uploadZone);
        uploadZoneText     = findViewById(R.id.uploadZoneText);
        tvFileName         = findViewById(R.id.selectedFileName);
        tvExtractionStatus = findViewById(R.id.tvExtractionStatus);
        tvWordCount        = findViewById(R.id.tvWordCount);
        tvPageCount        = findViewById(R.id.tvPageCount);
        uploadProgress     = findViewById(R.id.uploadProgress);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

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

        updateSubjects();

        notesRecycler.setLayoutManager(new LinearLayoutManager(this));

        // Preview is read-only
        if (contentPreview != null) {
            contentPreview.setKeyListener(null);
            contentPreview.setFocusable(false);
        }

        uploadZone.setOnClickListener(v -> openPicker());
        uploadBtn.setOnClickListener(v -> doUpload());
        uploadBtn.setEnabled(false);
        loadMyNotes();
    }

    private void updateSubjects() {
        String dept = deptSpinner.getSelectedItem() != null ? deptSpinner.getSelectedItem().toString() : "";
        String cls = classSpinner.getSelectedItem() != null ? classSpinner.getSelectedItem().toString() : "";
        List<String> subjects = AppConstants.getSubjects(dept, cls);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, subjects);
        subjectSpinner.setAdapter(adapter);
    }

    // ── Step 1: file picked ───────────────────────────────────────

    private void onFilePicked(Uri uri) {
        fileUri  = uri;
        fileName = resolveFileName(uri);
        
        // Read file bytes
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is != null) {
                fileBytes = new byte[is.available()];
                is.read(fileBytes);
                
                // Firestore limit check (approx 1MB)
                if (fileBytes.length > 900000) {
                    Toast.makeText(this, "File too large! Max 1MB allowed for free tier.", Toast.LENGTH_LONG).show();
                    fileUri = null; fileBytes = null; return;
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error reading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        String lo = fileName.toLowerCase();
        if      (lo.endsWith(".pdf"))  fileType = "pdf";
        else if (lo.endsWith(".pptx")) fileType = "pptx";
        else if (lo.endsWith(".docx")) fileType = "docx";
        else if (lo.endsWith(".ppt"))  fileType = "ppt";
        else                           fileType = "text";

        // Auto-fill title
        String base = fileName;
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        base = base.replace('_',' ').replace('-',' ').trim();
        if (titleInput != null && str(titleInput).isEmpty()) titleInput.setText(base);

        tvFileName.setText("📎 " + fileName);
        tvFileName.setVisibility(View.VISIBLE);
        uploadZoneText.setText("⏳ Extracting text…");
        setStatus("⏳ Reading " + fileType.toUpperCase() + " file…", false);
        uploadProgress.setIndeterminate(true);
        uploadProgress.setVisibility(View.VISIBLE);
        tvWordCount.setVisibility(View.GONE);
        tvPageCount.setVisibility(View.GONE);
        uploadBtn.setEnabled(false);
        if (contentPreview != null) contentPreview.setText("");
        extracted = "";

        DocumentExtractor.extract(this, uri, fileName,
            new DocumentExtractor.ExtractionCallback() {
                @Override public void onSuccess(String text, int pages) {
                    extracted = text;
                    uploadZoneText.setText("✅ Text extracted successfully!");
                    setStatus("✅ " + text.trim().split("\\s+").length + " words ready to index", false);
                    uploadProgress.setIndeterminate(false);
                    uploadProgress.setProgress(100);
                    int wc = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
                    tvWordCount.setText("📝 " + wc + " words");
                    tvWordCount.setVisibility(View.VISIBLE);
                    if (pages > 1) {
                        tvPageCount.setText("📄 " + pages + " pages");
                        tvPageCount.setVisibility(View.VISIBLE);
                    }
                    // Show first 600 chars as preview
                    if (contentPreview != null) {
                        String prev = text.length() > 600 ? text.substring(0,600) + "\n…" : text;
                        contentPreview.setText(prev);
                    }
                    uploadBtn.setEnabled(true);
                    Toast.makeText(UploadNotesActivity.this,
                        "✅ " + wc + " words extracted!", Toast.LENGTH_LONG).show();
                }
                @Override public void onError(String msg) {
                    uploadZoneText.setText("⚠️ Tap to try another file");
                    setStatus("❌ " + msg, true);
                    uploadProgress.setVisibility(View.GONE);
                    extracted = ""; uploadBtn.setEnabled(false);
                }
            });
    }

    private void setStatus(String t, boolean err) {
        if (tvExtractionStatus == null) return;
        tvExtractionStatus.setText(t);
        tvExtractionStatus.setTextColor(getColor(err ? R.color.accent : R.color.answered_text));
        tvExtractionStatus.setVisibility(View.VISIBLE);
    }

    // ── Step 2: save to Firestore ONLY (no Firebase Storage) ─────

    private void doUpload() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in again to upload.", Toast.LENGTH_SHORT).show();
            return;
        }

        String title   = str(titleInput);
        String subject = subjectSpinner.getSelectedItem().toString();
        String dept    = deptSpinner.getSelectedItem().toString();
        String cls     = classSpinner.getSelectedItem().toString();

        if (title.isEmpty()) { Toast.makeText(this,"Please enter a title",Toast.LENGTH_SHORT).show(); return; }
        if (extracted.isEmpty()) { Toast.makeText(this,"No content yet — pick a file first",Toast.LENGTH_SHORT).show(); return; }
        if (fileBytes == null) { Toast.makeText(this, "No file data found.", Toast.LENGTH_SHORT).show(); return; }

        uploadBtn.setEnabled(false);
        uploadBtn.setText("Saving...");
        uploadProgress.setVisibility(View.VISIBLE);
        uploadProgress.setIndeterminate(true);

        String uid = auth.getCurrentUser().getUid();
        String base64Data = Base64.encodeToString(fileBytes, Base64.NO_WRAP);

        db.collection("users").document(uid).get()
            .addOnSuccessListener(u -> {
                String uploader = (u.exists() && u.getString("name") != null)
                        ? u.getString("name") : "Teacher";
                saveNote(title, extracted, subject, dept, cls, uid, uploader, base64Data);
            })
            .addOnFailureListener(e -> {
                saveNote(title, extracted, subject, dept, cls, uid, "Teacher", base64Data);
            });
    }

    private void saveNote(String title, String content, String subject,
            String dept, String cls, String uid, String uploader, String base64Data) {

        List<String> kw = KeywordMatcher.extractKeywords(
            title + " " + content + " " + subject + " " + dept);

        Note note = new Note(title, content, subject, dept, cls, uid, uploader, kw, "", fileType, base64Data);

        db.collection("notes").add(note)
            .addOnSuccessListener(ref -> {
                uploadBtn.setEnabled(false);
                uploadBtn.setText("Upload & Index");
                uploadProgress.setVisibility(View.GONE);
                fileUri = null; fileBytes = null; extracted = "";
                if (titleInput != null)   titleInput.setText("");
                if (contentPreview != null) contentPreview.setText("");
                tvFileName.setVisibility(View.GONE);
                tvExtractionStatus.setVisibility(View.GONE);
                tvWordCount.setVisibility(View.GONE);
                tvPageCount.setVisibility(View.GONE);
                uploadZoneText.setText("Tap to attach PDF, PPTX, DOCX or TXT");
                Toast.makeText(this, "✅ Saved! " + kw.size() + " keywords indexed.", Toast.LENGTH_LONG).show();
                loadMyNotes();
                rematch(subject);
            })
            .addOnFailureListener(e -> {
                uploadBtn.setEnabled(true);
                uploadBtn.setText("Upload & Index");
                uploadProgress.setVisibility(View.GONE);
                Toast.makeText(this,"Save failed: "+e.getMessage(),Toast.LENGTH_SHORT).show();
            });
    }

    // ── Re-match pending doubts with extractRelevantAnswer ────────

    private void rematch(String subject) {
        db.collection("notes").whereEqualTo("subject", subject).get()
            .addOnSuccessListener(ns -> {
                List<Note> notes = new ArrayList<>();
                for (QueryDocumentSnapshot nd : ns) {
                    Note n = nd.toObject(Note.class); n.setId(nd.getId()); notes.add(n);
                }
                db.collection("doubts").whereEqualTo("status","pending")
                    .whereEqualTo("subject", subject).get()
                    .addOnSuccessListener(ds -> {
                        int matched = 0;
                        for (QueryDocumentSnapshot dd : ds) {
                            Doubt d = dd.toObject(Doubt.class); d.setId(dd.getId());
                            Note m = KeywordMatcher.findBestMatch(d, notes, AppConstants.MATCH_THRESHOLD);
                            if (m != null) {
                                // ✅ Use RAG for precision re-matching
                                String context = KeywordMatcher.extractRAGContext(d, notes);
                                final String doubtId = d.getId();
                                final Doubt finalD = d;
                                
                                new GeminiRAGHelper().generateAnswer(d.getTitle() + ": " + d.getDescription(), context, new GeminiRAGHelper.RAGCallback() {
                                    @Override
                                    public void onSuccess(String answer) {
                                        saveRematchAnswer(doubtId, finalD, answer, "AI Assistant (RAG Re-match)");
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {
                                        // Fallback to traditional match
                                        String ans = KeywordMatcher.extractRelevantAnswer(finalD, m);
                                        if (ans.isEmpty()) ans = KeywordMatcher.trimContent(m.getContent(), 800);
                                        saveRematchAnswer(doubtId, finalD, ans, "Auto-matched from: " + m.getTitle());
                                    }
                                });
                                matched++;
                            }
                        }
                    });
            });
    }

    private void saveRematchAnswer(String doubtId, Doubt d, String answer, String source) {
        db.collection("doubts").document(doubtId).update(
            "status", "answered",
            "answer", answer,
            "answeredBy", source,
            "answeredAt", System.currentTimeMillis()
        ).addOnSuccessListener(aVoid -> {
            if (d.getUserId() != null)
                NotificationHelper.sendNotification(d.getUserId(),
                    "✅ Your doubt was answered!",
                    "\"" + d.getTitle() + "\" was answered using " + (source.contains("AI") ? "AI Precision" : "matched notes") + ".",
                    "answer", doubtId);
        });
    }

    // ── Load my notes (no orderBy — sort in-memory) ───────────────

    private void loadMyNotes() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("notes").get()
            .addOnSuccessListener(snap -> {
                notesList.clear();
                for (QueryDocumentSnapshot doc : snap) {
                    if (!uid.equals(doc.getString("uploadedBy"))) continue;
                    Note n = doc.toObject(Note.class); n.setId(doc.getId()); notesList.add(n);
                }
                Collections.sort(notesList, (a,b) -> {
                    if (a.getTimestamp() == null || b.getTimestamp() == null) return 0;
                    return b.getTimestamp().compareTo(a.getTimestamp());
                });
                notesRecycler.setAdapter(new NoteAdapter(notesList));
            });
    }

    // ── Helpers ───────────────────────────────────────────────────

    private void openPicker() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"});
        i.addCategory(Intent.CATEGORY_OPENABLE);
        picker.launch(Intent.createChooser(i,"Pick PDF, PPTX, DOCX or TXT"));
    }

    private String resolveFileName(Uri uri) {
        String name = "document";
        try (Cursor c = getContentResolver().query(uri,null,null,null,null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0 && !c.isNull(idx)) name = c.getString(idx);
            }
        } catch (Exception e) { /* ignore */ }
        return name;
    }

    private String str(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private static String[] dropFirst(String[] arr) {
        String[] o = new String[arr.length-1];
        System.arraycopy(arr,1,o,0,o.length);
        return o;
    }
}
