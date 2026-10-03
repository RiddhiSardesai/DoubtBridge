package com.example.doubtbridge.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.doubtbridge.DoubtDetailActivity;
import com.example.doubtbridge.R;
import com.example.doubtbridge.models.NotificationModel;
import com.example.doubtbridge.utils.TimeUtils;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
    private final List<NotificationModel> list;

    public NotificationAdapter(List<NotificationModel> list) { this.list = list; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
        View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_notification, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        NotificationModel n = list.get(pos);
        Context ctx = h.itemView.getContext();

        // Icon based on type
        String icon = "🔔";
        if ("answer".equals(n.getType())) icon = "✅";
        else if ("important".equals(n.getType())) icon = "⭐";
        else if ("new_doubt".equals(n.getType())) icon = "❓";
        h.tvIcon.setText(icon);
        h.tvTitle.setText(n.getTitle());
        h.tvMessage.setText(n.getMessage());
        h.tvTime.setText(TimeUtils.getRelativeTime(new Date(n.getTimestamp())));

        // Unread dot
        h.unreadDot.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);

        // Click → mark read + open doubt
        h.itemView.setOnClickListener(v -> {
            if (!n.isRead() && n.getId() != null) {
                FirebaseFirestore.getInstance().collection("notifications")
                        .document(n.getId()).update("isRead", true);
                n.setRead(true);
                h.unreadDot.setVisibility(View.GONE);
                h.itemView.setBackgroundColor(Color.TRANSPARENT);
            }
            if (n.getDoubtId() != null) {
                Intent i = new Intent(ctx, DoubtDetailActivity.class);
                i.putExtra("doubtId", n.getDoubtId());
                ctx.startActivity(i);
            }
        });

        // Unread → light tint background
        if (!n.isRead()) {
            h.itemView.setBackgroundColor(Color.parseColor("#F3F4FF"));
        } else {
            h.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvIcon, tvTitle, tvMessage, tvTime;
        View unreadDot;
        VH(@NonNull View v) {
            super(v);
            tvIcon = v.findViewById(R.id.tvIcon);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTime = v.findViewById(R.id.tvTime);
            unreadDot = v.findViewById(R.id.unreadDot);
        }
    }
}
