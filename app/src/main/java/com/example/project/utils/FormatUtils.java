package com.example.project.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {

    public static String formatPrice(double amount) {
        return String.format(Locale.getDefault(), "₹%.2f", amount);
    }

    public static String formatDate(long timestamp) {
        if (timestamp <= 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
