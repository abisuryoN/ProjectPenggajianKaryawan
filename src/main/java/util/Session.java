package util;

public class Session {
    private static int idKaryawan;
    private static String namaKaryawan;
    private static String role;

    public static void login(int id, String nama, String userRole) {
        idKaryawan = id;
        namaKaryawan = nama;
        role = userRole;
    }

    public static void logout() {
        idKaryawan = 0;
        namaKaryawan = "";
        role = "";
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
}