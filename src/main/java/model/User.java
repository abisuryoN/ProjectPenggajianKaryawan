package model;

public class User {
    private int idUser;
    private int idKaryawan;
    private String namaKaryawan;
    private String username;
    private String password;
    private String role;

    public User() {}

    public User(int idUser, int idKaryawan, String namaKaryawan, String username, String password, String role) {
        this.idUser = idUser;
        this.idKaryawan = idKaryawan;
        this.namaKaryawan = namaKaryawan;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public int getIdUser() { return idUser; }
    public void setIdUser(int idUser) { this.idUser = idUser; }

    public int getIdKaryawan() { return idKaryawan; }
    public void setIdKaryawan(int idKaryawan) { this.idKaryawan = idKaryawan; }

    public String getNamaKaryawan() { return namaKaryawan; }
    public void setNamaKaryawan(String namaKaryawan) { this.namaKaryawan = namaKaryawan; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
