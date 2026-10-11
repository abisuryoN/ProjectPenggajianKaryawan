-- ============================================================
-- Migrasi V2: absensi face-recognition + validasi GPS (radius 200 m)
-- Aman untuk data lama: semua objek IF NOT EXISTS / ADD COLUMN IF NOT EXISTS
-- (MySQL 8 mendukung sintaks ini), tidak ada DROP/DELETE.
-- Jalankan: mysql -u root db_penggajian < database/migrasi_v2_absensi_face_gps.sql
-- ============================================================

-- 1. Konfigurasi lokasi kantor (diatur HRD). Radius dibatasi max 200 m
--    oleh aplikasi (CHECK tidak dipakai agar kompatibel luas).
CREATE TABLE IF NOT EXISTS pengaturan_lokasi (
    id_pengaturan INT AUTO_INCREMENT PRIMARY KEY,
    nama_lokasi   VARCHAR(100) NOT NULL DEFAULT 'Kantor Utama',
    latitude      DOUBLE NOT NULL,
    longitude     DOUBLE NOT NULL,
    radius_meter  INT NOT NULL DEFAULT 200,
    aktif         TINYINT(1) NOT NULL DEFAULT 1,
    diubah_oleh   INT NULL,
    diubah_pada   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                  ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pengaturan_ubah FOREIGN KEY (diubah_oleh)
        REFERENCES users (id_user)
) ENGINE=InnoDB;

-- Default kantor bila tabel masih kosong (koordinat contoh Jakarta;
-- HRD WAJIB mengganti dengan koordinat kantor sebenarnya via menu
-- "Pengaturan Lokasi Absensi").
INSERT INTO pengaturan_lokasi (nama_lokasi, latitude, longitude, radius_meter, aktif)
SELECT 'Kantor Utama', -6.200000, 106.816666, 200, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM pengaturan_lokasi WHERE aktif = 1);

-- 2. Template wajah karyawan (embedding LBPH, bukan foto mentah).
--    Satu baris per karyawan; didaftarkan HRD. BLOB agar ukuran fleksibel.
CREATE TABLE IF NOT EXISTS wajah_karyawan (
    id_wajah      INT AUTO_INCREMENT PRIMARY KEY,
    id_karyawan   INT NOT NULL UNIQUE,
    template      LONGBLOB NOT NULL,
    algoritma     VARCHAR(40) NOT NULL DEFAULT 'LBPH-OpenCV',
    kualitas      INT NULL,
    didaftarkan_oleh INT NULL,
    didaftarkan_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wajah_karyawan FOREIGN KEY (id_karyawan)
        REFERENCES karyawan (id_karyawan),
    CONSTRAINT fk_wajah_daftar FOREIGN KEY (didaftarkan_oleh)
        REFERENCES users (id_user)
) ENGINE=InnoDB;

-- 3. Kolom audit pada absensi: bukti verifikasi wajah + lokasi.
--    NULL = absensi manual lama (kompatibel mundur).
--    MySQL 8 tidak mendukung ADD COLUMN IF NOT EXISTS, jadi pakai
--    prosedur pembantu yang mengecek INFORMATION_SCHEMA.
DELIMITER $$
DROP PROCEDURE IF EXISTS tambah_kolom_absensi$$
CREATE PROCEDURE tambah_kolom_absensi()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'absensi'
                   AND COLUMN_NAME = 'verifikasi_wajah') THEN
        ALTER TABLE absensi ADD COLUMN verifikasi_wajah VARCHAR(20) NULL
            COMMENT 'COCOK | TIDAK_COCOK | TAK_DIKENAL | MANUAL';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'absensi'
                   AND COLUMN_NAME = 'skor_wajah') THEN
        ALTER TABLE absensi ADD COLUMN skor_wajah DOUBLE NULL
            COMMENT 'Confidence LBPH: makin kecil makin mirip';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'absensi'
                   AND COLUMN_NAME = 'lat_absen') THEN
        ALTER TABLE absensi ADD COLUMN lat_absen DOUBLE NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'absensi'
                   AND COLUMN_NAME = 'lon_absen') THEN
        ALTER TABLE absensi ADD COLUMN lon_absen DOUBLE NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'absensi'
                   AND COLUMN_NAME = 'jarak_meter') THEN
        ALTER TABLE absensi ADD COLUMN jarak_meter DOUBLE NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'absensi'
                   AND COLUMN_NAME = 'foto_bukti') THEN
        ALTER TABLE absensi ADD COLUMN foto_bukti VARCHAR(255) NULL
            COMMENT 'Path file bukti, bukan isi gambar';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'absensi'
                   AND COLUMN_NAME = 'sumber') THEN
        ALTER TABLE absensi ADD COLUMN sumber VARCHAR(20) NULL DEFAULT 'MANUAL'
            COMMENT 'MANUAL | FACE_GPS';
    END IF;
END$$
DELIMITER ;
CALL tambah_kolom_absensi();
DROP PROCEDURE tambah_kolom_absensi;

-- 4. Log kegagalan verifikasi (proporsional: tanpa foto mentah).
CREATE TABLE IF NOT EXISTS log_verifikasi_absensi (
    id_log        INT AUTO_INCREMENT PRIMARY KEY,
    id_karyawan   INT NOT NULL,
    tanggal       DATE NOT NULL,
    waktu         TIME NOT NULL,
    alasan        VARCHAR(50) NOT NULL
        COMMENT 'WAJAH_TIDAK_COCOK | WAJAH_TAK_DIKENAL | TANPA_WAJAH | MULTI_WAJAH | LOKASI_DI_LUAR | LOKASI_TAK_TERSEDIA | LIVENESS_GAGAL',
    detail        VARCHAR(255) NULL,
    CONSTRAINT fk_logver_karyawan FOREIGN KEY (id_karyawan)
        REFERENCES karyawan (id_karyawan)
) ENGINE=InnoDB;

-- 5. Audit perubahan konfigurasi lokasi.
CREATE TABLE IF NOT EXISTS audit_pengaturan_lokasi (
    id_audit      INT AUTO_INCREMENT PRIMARY KEY,
    id_pengaturan INT NOT NULL,
    diubah_oleh   INT NULL,
    diubah_pada   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    latitude_lama DOUBLE NULL,
    longitude_lama DOUBLE NULL,
    radius_lama   INT NULL,
    latitude_baru DOUBLE NOT NULL,
    longitude_baru DOUBLE NOT NULL,
    radius_baru   INT NOT NULL,
    CONSTRAINT fk_auditlok_peng FOREIGN KEY (id_pengaturan)
        REFERENCES pengaturan_lokasi (id_pengaturan),
    CONSTRAINT fk_auditlok_user FOREIGN KEY (diubah_oleh)
        REFERENCES users (id_user)
) ENGINE=InnoDB;
