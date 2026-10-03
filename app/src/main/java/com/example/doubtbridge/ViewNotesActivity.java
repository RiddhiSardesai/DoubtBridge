package com.example.doubtbridge;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doubtbridge.models.Note;
import com.example.doubtbridge.utils.AppConstants;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.*;
import java.util.*;

public class ViewNotesActivity extends AppCompatActivity {

    RecyclerView rv;
    EditText searchInput;
    Spinner subjectFilter, deptFilter, classFilter;
    View emptyView;
    TextView tvCount;
    ImageButton btnBack;

    FirebaseFirestore db;
    List<Note> all     = new ArrayList<>();
    List<Note> display = new ArrayList<>();
    NoteAdapter adapter;

    String selectedSubject = "All Subjects";
    String selectedDept = "All Departments";
    String selectedClass = "All Classes";
    String query = "";

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_view_notes);

        rv            = findViewById(R.id.recyclerView);
        searchInput   = findViewById(R.id.searchInput);
        subjectFilter = findViewById(R.id.subjectFilter);
        deptFilter    = findViewById(R.id.deptFilter);
        classFilter   = findViewById(R.id.classFilter);
        emptyView     = findViewById(R.id.emptyView);
        tvCount       = findViewById(R.id.tvNotesCount);
        btnBack       = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(display);
        rv.setAdapter(adapter);

        // Setup Dept and Class spinners
        deptFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, AppConstants.DEPARTMENTS));
        classFilter.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, AppConstants.CLASSES));

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (p.getId() == R.id.deptFilter || p.getId() == R.id.classFilter) {
                    updateSubjectSpinner();
                } else {
                    selectedSubject = subjectFilter.getSelectedItem().toString();
                    applyFilter();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };

        deptFilter.setOnItemSelectedListener(filterListener);
        classFilter.setOnItemSelectedListener(filterListener);
        subjectFilter.setOnItemSelectedListener(filterListener);

        updateSubjectSpinner();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                query = s.toString().toLowerCase().trim(); applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadNotes();
    }

    private void updateSubjectSpinner() {
        selectedDept = deptFilter.getSelectedItem().toString();
        selectedClass = classFilter.getSelectedItem().toString();

        List<String> subjects = new ArrayList<>();
        subjects.add("All Subjects");
        subjects.addAll(AppConstants.getSubjects(selectedDept, selectedClass));

        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, subjects);
        subjectFilter.setAdapter(subAdapter);
        selectedSubject = "All Subjects";
        applyFilter();
    }

    private void loadNotes() {
        // No orderBy — sort in-memory to avoid Firestore index requirement
        db.collection("notes").addSnapshotListener((snap, e) -> {
            if (snap == null) return;
            all.clear();
            for (QueryDocumentSnapshot doc : snap) {
                Note n = doc.toObject(Note.class);
                n.setId(doc.getId());
                all.add(n);
            }
            // Sort by timestamp descending in-memory
            Collections.sort(all, (a, b) -> {
                if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                if (a.getTimestamp() == null) return 1;
                if (b.getTimestamp() == null) return -1;
                return b.getTimestamp().compareTo(a.getTimestamp());
            });
            applyFilter();
        });
    }

    private void applyFilter() {
        display.clear();
        for (Note n : all) {
            boolean deptOk = "All Departments".equals(selectedDept) ||
                    (n.getDepartment() != null && n.getDepartment().equalsIgnoreCase(selectedDept));
            boolean classOk = "All Classes".equals(selectedClass) ||
                    (n.getClassName() != null && n.getClassName().equalsIgnoreCase(selectedClass));
            boolean subOk = "All Subjects".equals(selectedSubject) ||
                    (n.getSubject() != null && n.getSubject().equalsIgnoreCase(selectedSubject));
            
            boolean srchOk = query.isEmpty() ||
                (n.getTitle() != null && n.getTitle().toLowerCase().contains(query)) ||
                (n.getContent() != null && n.getContent().toLowerCase().contains(query));

            if (deptOk && classOk && subOk && srchOk) display.add(n);
        }
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(display.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setVisibility(display.isEmpty() ? View.GONE : View.VISIBLE);
        if (tvCount != null)
            tvCount.setText(display.size() + " material" + (display.size() == 1 ? "" : "s") + " available");
    }

    // ── Inner adapter ─────────────────────────────────────────────

    static class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.VH> {
        final List<Note> list;
        NoteAdapter(List<Note> list) { this.list = list; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_note_student, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Note n = list.get(pos);
            h.tvTitle.setText(n.getTitle() != null ? n.getTitle() : "Untitled");
            h.tvSubject.setText(n.getSubject() != null ? n.getSubject() : "");

            // Meta
            String meta = "";
            if (n.getDepartment() != null && !n.getDepartment().isEmpty()) meta = "· " + n.getDepartment();
            if (n.getClassName()  != null && !n.getClassName().isEmpty())
                meta += (meta.isEmpty() ? "· " : " · ") + n.getClassName();
            h.tvMeta.setText(meta);
            h.tvMeta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

            h.tvUploader.setText("📤 By " + (n.getUploaderName() != null ? n.getUploaderName() : "Teacher"));

            // File type badge
            String ft = n.getFileType();
            if (ft != null && !ft.isEmpty() && !ft.equals("text")) {
                h.tvBadge.setVisibility(View.VISIBLE);
                h.tvBadge.setText(ft.toUpperCase());
            } else h.tvBadge.setVisibility(View.GONE);

            // Content preview
            String c = n.getContent();
            if (c != null && !c.trim().isEmpty()) {
                String prev = c.trim();
                if (prev.length() > 140) prev = prev.substring(0, 140) + "…";
                h.tvPreview.setText(prev);
                h.tvPreview.setVisibility(View.VISIBLE);
            } else h.tvPreview.setVisibility(View.GONE);

            // Open Detail on click
            h.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), NoteDetailActivity.class);
                intent.putExtra("noteId", n.getId());
                v.getContext().startActivity(intent);
            });

            h.btnOpen.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), NoteDetailActivity.class);
                intent.putExtra("noteId", n.getId());
                v.getContext().startActivity(intent);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubject, tvMeta, tvUploader, tvBadge, tvPreview;
            MaterialButton btnOpen;
            VH(View v) {
                super(v);
                tvTitle    = v.findViewById(R.id.tvTitle);
                tvSubject  = v.findViewById(R.id.tvSubject);
                tvMeta     = v.findViewById(R.id.tvMeta);
                tvUploader = v.findViewById(R.id.tvUploader);
                tvBadge    = v.findViewById(R.id.tvBadge);
                tvPreview  = v.findViewById(R.id.tvPreview);
                btnOpen    = v.findViewById(R.id.btnOpen);
            }
        }
    }
}
