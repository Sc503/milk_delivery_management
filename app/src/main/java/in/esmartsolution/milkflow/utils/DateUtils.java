package in.esmartsolution.milkflow.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    /**
     * Gets today's date formatted as YYYY-MM-DD
     */
    public static String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Gets current time formatted as hh:mm a (e.g., 07:45 AM)
     */
    public static String getCurrentTimeString() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Formats an input Date object as YYYY-MM-DD
     */
    public static String formatDateString(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(date);
    }

    /**
     * Converts a database date string (yyyy-MM-dd) to a user-friendly string (dd MMM yyyy)
     * e.g., "2026-06-01" becomes "01 Jun 2026"
     */
    public static String getFriendlyDateString(String dateStr) {
        try {
            SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = dbFormat.parse(dateStr);
            if (date != null) {
                SimpleDateFormat friendlyFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                return friendlyFormat.format(date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dateStr;
    }

    /**
     * Gets the number of days in a given month of a given year
     */
    public static int getDaysInMonth(int monthZeroBased, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, monthZeroBased);
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * Gets month name short/long from index (0-based)
     */
    public static String getMonthName(int monthZeroBased) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, monthZeroBased);
        return calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
    }

    /**
     * Formats month/year into search prefix (e.g., Month = 5 (June), Year = 2026 produces "2026-06%")
     */
    public static String getYearMonthPrefix(int monthZeroBased, int year) {
        int actualMonth = monthZeroBased + 1; // 1-12
        return String.format(Locale.getDefault(), "%04d-%02d%%", year, actualMonth);
    }

    /**
     * Formats month/year into double digit date prefix (e.g., "2026-06")
     */
    public static String getYearMonthString(int monthZeroBased, int year) {
        int actualMonth = monthZeroBased + 1; // 1-12
        return String.format(Locale.getDefault(), "%04d-%02d", year, actualMonth);
    }
}
