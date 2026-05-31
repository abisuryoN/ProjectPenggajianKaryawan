package util;

import java.text.NumberFormat;
import java.util.Locale;

public class FormatRupiah {
    public static String format(double angka) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return format.format(angka);
    }

    public static double parse(String rupiah) {
        if (rupiah == null || rupiah.trim().isEmpty()) return 0;
        String angka = rupiah.replace("Rp", "")
                .replace("rp", "")
                .replace(".", "")
                .replace(",", ".")
                .trim();
        try {
            return Double.parseDouble(angka);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
