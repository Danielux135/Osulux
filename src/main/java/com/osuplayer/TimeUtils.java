package com.osuplayer;

import javafx.scene.control.Label;

public final class TimeUtils {
    private TimeUtils() {}

    public static String formatTime(double seconds) {
        int total = (int) seconds;
        int h = total / 3600;
        int m = (total % 3600) / 60;
        int s = total % 60;
        if (h > 0) {
            return String.format("%d:%02d:%02d", h, m, s);
        } else {
            return String.format("%02d:%02d", m, s);
        }
    }

    public static void updateTimeLabel(Label label, double current, double total) {
        String c = formatTime(current);
        String t = formatTime(total);
        label.setText(c + " / " + t);
    }
}