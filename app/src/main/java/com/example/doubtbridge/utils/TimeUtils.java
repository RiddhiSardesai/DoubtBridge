package com.example.doubtbridge.utils;

import java.util.Date;

public class TimeUtils {
    public static String getRelativeTime(Date date) {
        if (date == null) return "Just now";
        long diff = System.currentTimeMillis() - date.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) return "Just now";
        if (minutes < 60) return minutes + "m ago";
        if (hours < 24) return hours + "h ago";
        if (days < 7) return days + "d ago";
        return days / 7 + "w ago";
    }
}
