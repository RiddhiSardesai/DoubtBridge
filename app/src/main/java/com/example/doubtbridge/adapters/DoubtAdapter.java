package com.example.doubtbridge.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doubtbridge.DoubtDetailActivity;
import com.example.doubtbridge.R;
import com.example.doubtbridge.TeacherAnswerActivity;
import com.example.doubtbridge.models.Doubt;
import com.example.doubtbridge.utils.TimeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DoubtAdapter extends RecyclerView.Adapter<DoubtAdapter.ViewHolder> {

    private final List<Doubt> list;
    private final boolean isTeacherMode;
    private Set<String> bookmarkedIds;

    public DoubtAdapter(List<Doubt> list, boolean isTeacherMode, Set<String> bookmarkedIds) {
        this.list = list;
        this.isTeacherMode = isTeacherMode;
        this.bookmarkedIds = bookmarkedIds != null ? bookmarkedIds : new HashSet<>();
    }
    public DoubtAdapter(List<Doubt> list, boolean isTeacherMode) { this(list, isTeacherMode, new HashSet<>()); }
    public DoubtAdapter(List<Doubt> list) { this(list, false, new HashSet<>()); }

    public void setBookmarkedIds(Set<String> ids) {
        this.bookmarkedIds = ids != null ? ids : new HashSet<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doubt, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Doubt d = list.get(position);
        Context ctx = h.itemView.getContext();

        // Avatar
        String name = d.getUserName() != null && !d.getUserName().isEmpty() ? d.getUserName() : "?";
        h.avatarLetter.setText(String.valueOf(name.charAt(0)).toUpperCase());
        h.userName.setText(name);
        
        String title = d.getTitle() != null ? d.getTitle() : "";
        if (d.isImportant()) {
            title = "⭐ " + title;
        }
        h.doubtTitle.setText(title);

        h.doubtDescription.setText(d.getDescription() != null ? d.getDescription() : "");
        h.subjectChip.setText(d.getSubject() != null ? d.getSubject() : "General");

        // Dept / Class tags
        String dept = d.getDepartment();
        String cls  = d.getClassName();
        if (h.deptTag != null) {
            if (dept != null && !dept.isEmpty()) { h.deptTag.setVisibility(View.VISIBLE); h.deptTag.setText(dept); }
            else h.deptTag.setVisibility(View.GONE);
        }
        if (h.classTag != null) {
            if (cls != null && !cls.isEmpty()) { h.classTag.setVisibility(View.VISIBLE); h.classTag.setText(cls); }
            else h.classTag.setVisibility(View.GONE);
        }

        // Time
        if (h.timeText != null && d.getTimestamp() > 0)
            h.timeText.setText(TimeUtils.getRelativeTime(new Date(d.getTimestamp())));

        // Status text
        boolean answered = "answered".equals(d.getStatus());
        h.repliesCount.setText(answered ? "✅ Answered" : "⏳ " + d.getFrequency() + " views");
        h.repliesCount.setTextColor(answered ? ctx.getColor(R.color.success) : ctx.getColor(R.color.text_secondary));

        // Show attachment icon if image exists
        if (d.getImageUrl() != null && !d.getImageUrl().isEmpty()) {
            h.repliesCount.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_attach_file, 0, 0, 0);
        } else {
            h.repliesCount.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }

        // ── BOOKMARK STAR ──────────────────────────────────────────────────
        // Use ic_star_filled (yellow fill) or ic_star_outline (grey)
        // No setColorFilter — the SVG drawables have their own colors
        boolean isBookmarked = d.getId() != null && bookmarkedIds.contains(d.getId());
        setStarDrawable(h.bookmarkBtn, isBookmarked);

        // Navigation
        h.itemView.setOnClickListener(v -> openDetail(ctx, d));
        h.solveBtn.setOnClickListener(v -> openDetail(ctx, d));

        // Bookmark button (student only)
        if (!isTeacherMode) {
            h.bookmarkBtn.setVisibility(View.VISIBLE);
            h.bookmarkBtn.setOnClickListener(v -> {
                if (d.getId() == null) return;
                if (bookmarkedIds.contains(d.getId())) {
                    removeBookmark(ctx, d, h.bookmarkBtn);
                } else {
                    addBookmark(ctx, d, h.bookmarkBtn);
                }
            });
        } else {
            h.bookmarkBtn.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the star drawable WITHOUT using setColorFilter.
     * ic_star_filled   → golden filled star (SVG fill #FFC107)
     * ic_star_outline  → grey outline star (SVG fill #9E9E9E)
     */
    private void setStarDrawable(ImageButton btn, boolean bookmarked) {
        btn.clearColorFilter(); // clear any previous tint
        btn.setImageResource(bookmarked ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
    }

    private void addBookmark(Context ctx, Doubt d, ImageButton btn) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) { Toast.makeText(ctx, "Please login", Toast.LENGTH_SHORT).show(); return; }
        String uid = auth.getCurrentUser().getUid();

        // Optimistic UI update immediately
        bookmarkedIds.add(d.getId());
        setStarDrawable(btn, true);

        Map<String, Object> bm = new HashMap<>();
        bm.put("userId",      uid);
        bm.put("doubtId",     d.getId());
        bm.put("title",       d.getTitle());
        bm.put("description", d.getDescription());
        bm.put("userName",    d.getUserName());
        bm.put("subject",     d.getSubject());
        bm.put("department",  d.getDepartment());
        bm.put("className",   d.getClassName());
        bm.put("status",      d.getStatus());
        bm.put("timestamp",   System.currentTimeMillis());

        // Check dup then add
        FirebaseFirestore.getInstance().collection("bookmarks")
            .whereEqualTo("userId", uid).whereEqualTo("doubtId", d.getId()).get()
            .addOnSuccessListener(q -> {
                if (!q.isEmpty()) return; // already exists, star is already yellow — fine
                FirebaseFirestore.getInstance().collection("bookmarks").add(bm)
                    .addOnSuccessListener(ref ->
                        Toast.makeText(ctx, "⭐ Bookmarked!", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> {
                        // Revert on failure
                        bookmarkedIds.remove(d.getId());
                        setStarDrawable(btn, false);
                    });
            });
    }

    private void removeBookmark(Context ctx, Doubt d, ImageButton btn) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        // Optimistic UI update immediately
        bookmarkedIds.remove(d.getId());
        setStarDrawable(btn, false);

        FirebaseFirestore.getInstance().collection("bookmarks")
            .whereEqualTo("userId", uid).whereEqualTo("doubtId", d.getId()).get()
            .addOnSuccessListener(q -> {
                for (com.google.firebase.firestore.DocumentSnapshot doc : q.getDocuments())
                    doc.getReference().delete();
                Toast.makeText(ctx, "Bookmark removed", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                // Revert on failure
                bookmarkedIds.add(d.getId());
                setStarDrawable(btn, true);
            });
    }

    private void openDetail(Context ctx, Doubt d) {
        if (d.getId() == null) return;
        Intent i = new Intent(ctx, isTeacherMode ? TeacherAnswerActivity.class : DoubtDetailActivity.class);
        i.putExtra("doubtId", d.getId());
        ctx.startActivity(i);
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView avatarLetter, userName, timeText, doubtTitle, doubtDescription, repliesCount, subjectChip, deptTag, classTag;
        MaterialButton solveBtn;
        ImageButton bookmarkBtn;

        ViewHolder(View v) {
            super(v);
            avatarLetter     = v.findViewById(R.id.avatarLetter);
            userName         = v.findViewById(R.id.userName);
            timeText         = v.findViewById(R.id.timeText);
            doubtTitle       = v.findViewById(R.id.doubtTitle);
            doubtDescription = v.findViewById(R.id.doubtDescription);
            repliesCount     = v.findViewById(R.id.repliesCount);
            subjectChip      = v.findViewById(R.id.subjectChip);
            deptTag          = v.findViewById(R.id.deptTag);
            classTag         = v.findViewById(R.id.classTag);
            solveBtn         = v.findViewById(R.id.solveBtn);
            bookmarkBtn      = v.findViewById(R.id.bookmarkBtn);
        }
    }
}
