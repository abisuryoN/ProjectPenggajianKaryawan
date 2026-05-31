package util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
    private static final SimpleDateFormat DB_DATE = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat VIEW_DATE = new SimpleDateFormat("dd-MM-yyyy");
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss");

    public static String tanggalHariIni() {
        return DB_DATE.format(new Date());
    }

    public static String jamSekarang() {
        return TIME.format(new Date());
    }

    public static String dateToDatabase(Date date) {
        return date == null ? null : DB_DATE.format(date);
    }

    public static String dateToView(Date date) {
        return date == null ? "" : VIEW_DATE.format(date);
    }

    public static Date databaseToDate(String value) {
        try {
            return value == null || value.isEmpty() ? null : DB_DATE.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }
}
