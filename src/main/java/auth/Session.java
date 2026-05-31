package auth;

public class Session {
    private static int idUser;
    private static int idKaryawan;
    private static String nama;
    private static String username;
    private static String role;

    public static void setSession(int userId, int karyawanId, String namaUser, String roleUser) {
        setSession(userId, karyawanId, namaUser, null, roleUser);
    }

    public static void setSession(int userId, int karyawanId, String namaUser, String usernameUser, String roleUser) {
        idUser = userId;
        idKaryawan = karyawanId;
        nama = namaUser;
        username = usernameUser;
        role = roleUser;
    }

    public static int getIdUser() {
        return idUser;
    }

    public static int getIdKaryawan() {
        return idKaryawan;
    }

    public static String getNama() {
        return nama == null ? "" : nama;
    }

    public static String getUsername() {
        return username == null ? "" : username;
    }

    public static String getRole() {
        return role == null ? "" : role;
    }

    public static boolean isHRD() {
        return "HRD".equalsIgnoreCase(role);
    }

    public static boolean isKaryawan() {
        return "KARYAWAN".equalsIgnoreCase(role);
    }

    public static void clear() {
        idUser = 0;
        idKaryawan = 0;
        nama = null;
        username = null;
        role = null;
    }
}
