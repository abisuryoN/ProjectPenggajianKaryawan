package util;

import java.text.NumberFormat;
import java.util.Locale;

public class FormatRupiah {

    /** Format mata uang lengkap, mis. "Rp100.000,00" (untuk tampilan laporan/cetak). */
    public static String format(double angka) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return format.format(angka);
    }

    /**
     * Format rupiah tanpa desimal, mis. "Rp100.000".
     * Dipakai untuk field input & tabel supaya nilai tidak berakhiran ",00"
     * saat diedit (kalau berakhiran ",00" pembacaan angka jadi tidak valid).
     */
    public static String formatPlain(double angka) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(0);
        return "Rp" + nf.format(Math.round(angka));
    }

    /**
     * Membaca angka dari teks apa pun bentuknya:
     * "Rp100.000", "100000,00", "100.000", "100000", "Rp 1.500.000,-" -> double.
     */
    public static double parse(String rupiah) {
        if (rupiah == null) return 0;
        String angka = rupiah.replace("Rp", "")
                .replace("rp", "")
                .replace(" ", "")
                .trim();
        angka = angka.replaceAll("[.,]00$", "");
        angka = angka.replaceAll("[.,]-$", "");
        angka = angka.replace(".", "");
        angka = angka.replace(",", ".");
        angka = angka.replaceAll("^[.,]+", "");
        if (angka.isEmpty()) return 0;
        try {
            return Double.parseDouble(angka);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
