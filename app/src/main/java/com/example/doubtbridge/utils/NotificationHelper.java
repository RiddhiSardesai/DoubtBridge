package com.example.doubtbridge.utils;

import com.example.doubtbridge.models.NotificationModel;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationHelper {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void sendNotification(String userId, String title,
                                        String message, String type, String doubtId) {
        if (userId == null || userId.isEmpty()) return;
        NotificationModel n = new NotificationModel(userId, title, message, type, doubtId);
        db.collection("notifications").add(n);
    }
}
