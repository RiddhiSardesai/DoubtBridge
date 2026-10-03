package com.example.doubtbridge.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.doubtbridge.NoteDetailActivity;
import com.example.doubtbridge.R;
import com.example.doubtbridge.models.Note;
import java.util.List;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {
    private final List<Note> list;

    public NoteAdapter(List<Note> list) { this.list = list; }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Note n = list.get(pos);
        h.tvTitle.setText(n.getTitle());

        // Subject + dept + class
        String meta = (n.getSubject() != null ? n.getSubject() : "");
        if (n.getDepartment() != null && !n.getDepartment().isEmpty()) meta += " · " + n.getDepartment();
        if (n.getClassName()  != null && !n.getClassName().isEmpty())  meta += " · " + n.getClassName();
        h.tvSubject.setText(meta);

        String kw = n.getKeywords() != null && !n.getKeywords().isEmpty()
            ? String.join(", ", n.getKeywords().subList(0, Math.min(6, n.getKeywords().size())))
            : "No keywords";
        h.tvKeywords.setText("🔑 " + kw);

        // File badge
        if (h.tvFileBadge != null) {
            String ft = n.getFileType();
            if (ft != null && !ft.equals("text") && !ft.isEmpty()) {
                h.tvFileBadge.setVisibility(View.VISIBLE);
                h.tvFileBadge.setText(ft.toUpperCase());
            } else {
                h.tvFileBadge.setVisibility(View.GONE);
            }
        }

        // Open Note Detail on click
        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), NoteDetailActivity.class);
            i.putExtra("noteId", n.getId());
            v.getContext().startActivity(i);
        });
    }

    @Override public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSubject, tvKeywords, tvFileBadge;
        ViewHolder(View v) {
            super(v);
            tvTitle    = v.findViewById(R.id.tvTitle);
            tvSubject  = v.findViewById(R.id.tvSubject);
            tvKeywords = v.findViewById(R.id.tvKeywords);
            tvFileBadge = v.findViewById(R.id.tvFileBadge);
        }
    }
}
