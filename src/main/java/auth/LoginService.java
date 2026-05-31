package auth;

import dao.UserDAO;
import model.User;

public class LoginService {
    public static boolean login(String username, String password) {
        try {
            User user = new UserDAO().login(username, password);
            if (user != null) {
                String namaTampil = user.getNamaKaryawan();
                if (namaTampil == null || namaTampil.trim().isEmpty()) {
                    namaTampil = user.getRole().equalsIgnoreCase("HRD") ? "HRD Utama" : user.getUsername();
                }
                Session.setSession(user.getIdUser(), user.getIdKaryawan(), namaTampil, user.getUsername(), user.getRole());
                return true;
            }
        } catch (Exception e) {
            System.out.println("Login database gagal, pakai login demo: " + e.getMessage());
        }

        // Login demo cadangan, biar project tetap bisa dibuka walau database belum aktif.
        if ("hrd".equalsIgnoreCase(username) && "hrd123".equals(password)) {
            Session.setSession(1, 0, "HRD Utama", username, "HRD");
            return true;
        }

        if ("karyawan".equalsIgnoreCase(username) && "karyawan123".equals(password)) {
            Session.setSession(2, 1, "Karyawan Demo", username, "KARYAWAN");
            return true;
        }

        return false;
    }
}
