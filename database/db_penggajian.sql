CREATE DATABASE IF NOT EXISTS db_penggajian;
USE db_penggajian;

CREATE TABLE divisi (
    id_divisi INT AUTO_INCREMENT PRIMARY KEY,
    kode_divisi VARCHAR(20) NOT NULL,
    nama_divisi VARCHAR(100) NOT NULL,
    keterangan TEXT
);

CREATE TABLE jabatan (
    id_jabatan INT AUTO_INCREMENT PRIMARY KEY,
    kode_jabatan VARCHAR(20) NOT NULL,
    nama_jabatan VARCHAR(100) NOT NULL,
    gaji_pokok DECIMAL(12,2) NOT NULL DEFAULT 0
);

CREATE TABLE tunjangan (
    id_tunjangan INT AUTO_INCREMENT PRIMARY KEY,
    kode_tunjangan VARCHAR(20) NOT NULL,
    nama_tunjangan VARCHAR(100) NOT NULL,
    nominal DECIMAL(12,2) NOT NULL DEFAULT 0
);

CREATE TABLE karyawan (
    id_karyawan INT AUTO_INCREMENT PRIMARY KEY,
    nik VARCHAR(30) NOT NULL,
    nama_karyawan VARCHAR(100) NOT NULL,
    jenis_kelamin VARCHAR(20),
    alamat TEXT,
    no_hp VARCHAR(20),
    id_divisi INT,
    id_jabatan INT,
    FOREIGN KEY (id_divisi) REFERENCES divisi(id_divisi),
    FOREIGN KEY (id_jabatan) REFERENCES jabatan(id_jabatan)
);

CREATE TABLE users (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    id_karyawan INT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role ENUM('HRD', 'KARYAWAN') NOT NULL,
    FOREIGN KEY (id_karyawan) REFERENCES karyawan(id_karyawan)
);

CREATE TABLE absensi (
    id_absensi INT AUTO_INCREMENT PRIMARY KEY,
    id_karyawan INT NOT NULL,
    tanggal DATE NOT NULL,
    jam_masuk TIME,
    jam_pulang TIME,
    status VARCHAR(30),
    FOREIGN KEY (id_karyawan) REFERENCES karyawan(id_karyawan)
);

CREATE TABLE penggajian (
    id_penggajian INT AUTO_INCREMENT PRIMARY KEY,
    id_karyawan INT NOT NULL,
    periode VARCHAR(20) NOT NULL,
    gaji_pokok DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_tunjangan DECIMAL(12,2) NOT NULL DEFAULT 0,
    potongan DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_gaji DECIMAL(12,2) NOT NULL DEFAULT 0,
    tanggal_gaji DATE,
    FOREIGN KEY (id_karyawan) REFERENCES karyawan(id_karyawan)
);

INSERT INTO divisi (kode_divisi, nama_divisi, keterangan) VALUES
('DIV001', 'Human Resource', 'Divisi HRD'),
('DIV002', 'Operasional', 'Divisi Operasional');

INSERT INTO jabatan (kode_jabatan, nama_jabatan, gaji_pokok) VALUES
('JBT001', 'HRD', 5000000),
('JBT002', 'Staff', 3500000);

INSERT INTO tunjangan (kode_tunjangan, nama_tunjangan, nominal) VALUES
('TJN001', 'Tunjangan Makan', 500000),
('TJN002', 'Tunjangan Transport', 400000);

INSERT INTO karyawan (nik, nama_karyawan, jenis_kelamin, alamat, no_hp, id_divisi, id_jabatan) VALUES
('KRY001', 'Karyawan Demo', 'Laki-laki', 'Jakarta', '08123456789', 2, 2);

INSERT INTO users (id_karyawan, username, password, role) VALUES
(NULL, 'hrd', 'hrd123', 'HRD'),
(1, 'karyawan', 'karyawan123', 'KARYAWAN');
