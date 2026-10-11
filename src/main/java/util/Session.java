package util;

public class Session {
    private static int idKaryawan;
    private static String namaKaryawan;
    private static String role;
    // Tambahan untuk audit (tidak mengubah perilaku lama):
    private static int idUser;
    private static String username;

    public static void login(int id, String nama, String userRole) {
        idKaryawan = id;
        namaKaryawan = nama;
        role = userRole;
    }

    /** Login lengkap dengan id user (untuk audit konfigurasi lokasi dll). */
    public static void login(int idUserBaru, int id, String nama, String usernameBaru, String userRole) {
        idUser = idUserBaru;
        username = usernameBaru;
        login(id, nama, userRole);
    }

    public static void logout() {
        idKaryawan = 0;
        namaKaryawan = "";
        role = "";
        idUser = 0;
        username = "";
    }

    public static int getIdKaryawan() {
        return idKaryawan;
    }

    public static String getNamaKaryawan() {
        return namaKaryawan;
    }

    public static String getRole() {
        return role;
    }

    public static boolean isHRD() {
        return "HRD".equalsIgnoreCase(role);
    }

    public static String getNama() {
        return namaKaryawan;
    }

    public static int getIdUser() {
        return idUser;
    }

    public static String getUsername() {
        return username == null ? "" : username;
    }
}
