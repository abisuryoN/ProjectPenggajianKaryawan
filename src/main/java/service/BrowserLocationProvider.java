package service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provider lokasi dari GPS PERANGKAT lewat browser (Geolocation API) — dipakai
 * saat GeoClue sistem tidak tersedia/diblokir (Bagian D §15).
 *
 * <p>Alasan desain: cukup banyak mesin Linux desktop tidak punya GPS dan
 * layanan geolokasi sistem dimatikan, sehingga GeoClue menjawab
 * {@code Geolocation disabled for UID 1000} dan absensi tidak mungkin
 * dilakukan. Browser modern sudah punya sumber lokasi sendiri (GPS perangkat
 * / WiFi / IP) dan menampilkan izin ke pengguna, jadi lokasi diambil dari sana.
 *
 * <p>Cara kerja (tanpa dependensi baru, hanya JDK):
 * <ol>
 *   <li>Server HTTP mini di loopback (127.0.0.1) dengan port acak.</li>
 *   <li>Halaman kecil dibuka di browser; halaman memanggil
 *       {@code navigator.geolocation.getCurrentPosition()} lalu mengirim
 *       koordinat kembali ke server.</li>
 *   <li>Provider memvalidasi koordinat, lalu menutup server. Ambil ulang kapan
 *       saja lewat tombol "Perbarui Lokasi".</li>
 * </ol>
 *
 * <p>Keamanan: server hanya mendengarkan di loopback, hanya menerima satu
 * permintaan hasil, dan langsung mati setelah dipakai. Nilai koordinat selalu
 * lewat validasi {@link LocationService#validasi} — radius dan akurasi tetap
 * diperiksa, tidak ada koordinat yang "dipercaya begitu saja".
 */
public class BrowserLocationProvider implements LocationProvider {

    /**
     * Batas waktu menunggu pengguna menyetujui & browser mengirim posisi.
     * Longgar karena pengguna harus menjawab dialog izin lokasi browser.
     */
    private static final long TIMEOUT_MS = 180_000;

    /** Host loopback saja — jangan pernah buka ke jaringan. */
    private static final String HOST = "127.0.0.1";

    @Override
    public String nama() {
        return "GPS perangkat (browser)";
    }

    @Override
    public HasilLokasiPengguna ambilLokasi() {
        ServerSocket server = null;
        try {
            // Backlog longgar: browser sering membuka beberapa koneksi hampir
            // bersamaan (preconnect); backlog terlalu kecil membuat koneksi
            // berikutnya ditolak (ERR_CONNECTION_REFUSED).
            server = new ServerSocket(0, 64, java.net.InetAddress.getByName(HOST));
            int port = server.getLocalPort();
            server.setSoTimeout((int) TIMEOUT_MS);

            if (!bukaBrowser("http://" + HOST + ":" + port + "/")) {
                return HasilLokasiPengguna.tidakAda(
                        "GPS perangkat: tidak ada browser yang bisa dibuka otomatis "
                      + "(buka manual " + "http://" + HOST + ":" + port + "/ ).");
            }

            AtomicReference<String> hasil = new AtomicReference<>(null);
            long tenggat = System.currentTimeMillis() + TIMEOUT_MS;
            while (System.currentTimeMillis() < tenggat && hasil.get() == null) {
                // Satu koneksi yang gagal TIDAK boleh menghentikan penantian:
                // browser sering membuka soket spekulatif/preconnect lalu
                // menutupnya tanpa mengirim apa pun, meminta /favicon.ico, atau
                // memutus sambungan saat halaman di-reload. Semua itu normal —
                // tangani per koneksi dan lanjut menunggu sampai tenggat.
                try (Socket klien = server.accept()) {
                    klien.setSoTimeout(5_000);
                    String barisPertama = bacaPermintaan(klien, hasil);
                    if (barisPertama == null) continue;
                    kirimHalaman(klien, barisPertama);
                } catch (java.net.SocketTimeoutException ulang) {
                    // tidak ada sambungan baru; terus tunggu sampai tenggat
                } catch (Exception koneksiGagal) {
                    // koneksi ini bermasalah (reset/putus) — abaikan, tetap tunggu
                }
            }

            String data = hasil.get();
            if (data == null) {
                return HasilLokasiPengguna.tidakAda(
                        "GPS perangkat: posisi tidak dikirim dalam "
                      + (TIMEOUT_MS / 1000) + " detik (izin lokasi ditolak/dibatalkan?).");
            }
            return parse(data);
        } catch (Exception e) {
            return HasilLokasiPengguna.tidakAda(
                    "GPS perangkat gagal dipakai: " + ringkas(e.getMessage()) + ".");
        } finally {
            tutup(server);
        }
    }

    /** Baca request; bila memuat koordinat (/hasil...), simpan ke {@code hasil}. */
    private String bacaPermintaan(Socket klien, AtomicReference<String> hasil) throws Exception {
        BufferedReader r = new BufferedReader(
                new InputStreamReader(klien.getInputStream(), StandardCharsets.UTF_8));
        String barisPertama = r.readLine();
        if (barisPertama == null) return null;

        // Baca header sampai baris kosong (abaikan isinya).
        String baris;
        while ((baris = r.readLine()) != null && !baris.isEmpty()) {
            // tidak dipakai, hanya menghabiskan header
        }

        if (barisPertama.contains("/hasil")) {
            int tanya = barisPertama.indexOf('?');
            if (tanya > 0) {
                String kueri = barisPertama.substring(tanya + 1).trim();
                int spasi = kueri.indexOf(' ');
                if (spasi > 0) kueri = kueri.substring(0, spasi);
                if (kueri.contains("lat=")) hasil.set(kueri);
            }
        }
        return barisPertama;
    }

    private void kirimHalaman(Socket klien, String barisPertama) throws Exception {
        // Permintaan yang tidak kita pedulikan (favicon, dsb) dijawab cepat
        // supaya browser tidak menahan sambungan sambil menunggu.
        if (barisPertama.contains("/favicon")) {
            String h204 = "HTTP/1.1 204 No Content\r\n"
                    + "Content-Length: 0\r\n"
                    + "Connection: close\r\n\r\n";
            klien.getOutputStream().write(h204.getBytes(StandardCharsets.UTF_8));
            klien.getOutputStream().flush();
            return;
        }
        boolean permintaanHasil = barisPertama.contains("/hasil");
        String halaman = permintaanHasil ? halamanSelesai() : halamanUtama();
        byte[] isi = halaman.getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Cache-Control: no-store\r\n"
                + "Content-Length: " + isi.length + "\r\n"
                + "Connection: close\r\n\r\n";
        klien.getOutputStream().write(header.getBytes(StandardCharsets.UTF_8));
        klien.getOutputStream().write(isi);
        klien.getOutputStream().flush();
    }

    /** Halaman peminta lokasi (satu kali klik tanpa perlu memperhatikan UI). */
    private static String halamanUtama() {
        return "<!doctype html><html lang=\"id\"><head><meta charset=\"utf-8\">"
             + "<title>Ambil Lokasi Absensi</title>"
             + "<style>body{font-family:system-ui,sans-serif;background:#f4f7fa;color:#16305c;"
             + "display:flex;align-items:center;justify-content:center;height:100vh;margin:0}"
             + ".k{background:#fff;border:1px solid #dce2eb;border-radius:14px;padding:28px 32px;"
             + "max-width:420px;box-shadow:0 8px 24px rgba(22,48,92,.08)}"
             + "h1{font-size:20px;margin:0 0 8px}p{margin:6px 0;color:#475569;font-size:14px}"
             + "button{margin-top:14px;background:#16305c;color:#fff;border:0;border-radius:10px;"
             + "padding:11px 18px;font-size:15px;cursor:pointer}"
             + ".s{margin-top:14px;font-size:14px}</style></head><body><div class=\"k\">"
             + "<h1>Ambil Lokasi Absensi</h1>"
             + "<p>Halaman ini meminta lokasi dari perangkat/browser Anda, lalu mengirimkannya "
             + "ke aplikasi absensi. Tidak ada data lain yang dikirim.</p>"
             + "<button id=\"b\">Izinkan &amp; Kirim Lokasi</button>"
             + "<div class=\"s\" id=\"s\"></div></div><script>"
             + "var s=document.getElementById('s');"
             + "function kirim(p){"
             + "var q='?lat='+encodeURIComponent(p.coords.latitude)"
             + "+'&lon='+encodeURIComponent(p.coords.longitude)"
             + "+'&acc='+encodeURIComponent(p.coords.accuracy||-1);"
             + "var n=0;"
             + "function coba(){n++;s.textContent='Mengirim lokasi... (percobaan '+n+')';"
             + "fetch('/hasil'+q).then(function(r){if(!r.ok)throw new Error('HTTP '+r.status);"
             + "s.textContent='Lokasi terkirim. Kembali ke aplikasi absensi.';})"
             + ".catch(function(e){if(n<3){setTimeout(coba,1200);return;}"
             + "s.textContent='Gagal mengirim ('+e+'). Kembali ke aplikasi absensi, "
             + "lalu tekan Perbarui Lokasi sekali lagi.';});}"
             + "coba();}"
             + "function ambil(){s.textContent='Meminta izin lokasi...';"
             + "if(!navigator.geolocation){s.textContent='Browser ini tidak mendukung geolokasi.';return;}"
             + "navigator.geolocation.getCurrentPosition(kirim,"
             + "function(e){s.textContent='Gagal mengambil lokasi: '+e.message;},"
             + "{enableHighAccuracy:true,timeout:30000,maximumAge:0});}"
             + "document.getElementById('b').onclick=ambil;"
             + "if(location.protocol==='http:'&&location.hostname==='127.0.0.1'){ambil();}"
             + "</script></body></html>";
    }

    /** Halaman penutup setelah koordinat terkirim. */
    private static String halamanSelesai() {
        return "<!doctype html><html lang=\"id\"><head><meta charset=\"utf-8\">"
             + "<title>Lokasi Terkirim</title></head><body "
             + "style=\"font-family:system-ui,sans-serif;background:#f4f7fa;color:#16305c;"
             + "display:flex;align-items:center;justify-content:center;height:100vh\">"
             + "<div>Lokasi sudah terkirim. Silakan kembali ke aplikasi absensi "
             + "(tab ini boleh ditutup).</div></body></html>";
    }

    /** Buka URL di browser default tanpa memblokir (best-effort). */
    private static boolean bukaBrowser(String url) {
        String[][] kandidat = {
                {"xdg-open", url},
                {"firefox", url},
                {"google-chrome", url},
        };
        for (String[] cmd : kandidat) {
            if (!perintahAda(cmd[0])) continue;
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.start();
                return true;
            } catch (Exception ignored) {
                // coba kandidat berikutnya
            }
        }
        return false;
    }

    private static boolean perintahAda(String nama) {
        String path = System.getenv("PATH");
        if (path == null) return false;
        for (String dir : path.split(File.pathSeparator)) {
            if (new File(dir, nama).canExecute()) return true;
        }
        return false;
    }

    private HasilLokasiPengguna parse(String kueri) {
        try {
            String lat = nilai(kueri, "lat");
            String lon = nilai(kueri, "lon");
            String acc = nilai(kueri, "acc");
            if (lat == null || lon == null) {
                return HasilLokasiPengguna.tidakAda("GPS perangkat: jawaban tidak lengkap.");
            }
            double la = Double.parseDouble(lat);
            double lo = Double.parseDouble(lon);
            double ak = (acc == null) ? -1 : Double.parseDouble(acc);
            if (!util.GeoUtil.latitudeValid(la) || !util.GeoUtil.longitudeValid(lo)) {
                return HasilLokasiPengguna.tidakAda("GPS perangkat: koordinat tidak valid.");
            }
            return HasilLokasiPengguna.ada(la, lo, ak,
                    "Lokasi dari GPS perangkat/browser (akurasi ±"
                  + (ak > 0 ? Math.round(ak) + " m" : "tidak diketahui") + ")");
        } catch (Exception e) {
            return HasilLokasiPengguna.tidakAda(
                    "GPS perangkat: koordinat tidak dapat dibaca (" + ringkas(e.getMessage()) + ").");
        }
    }

    private static String nilai(String kueri, String kunci) {
        for (String pasangan : kueri.split("&")) {
            int eq = pasangan.indexOf('=');
            if (eq > 0 && pasangan.substring(0, eq).equals(kunci)) {
                return java.net.URLDecoder.decode(pasangan.substring(eq + 1),
                        StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String ringkas(String pesan) {
        if (pesan == null) return "tidak diketahui";
        return pesan.length() > 120 ? pesan.substring(0, 120) + "…" : pesan;
    }

    private static void tutup(ServerSocket s) {
        if (s != null) {
            try { s.close(); } catch (Exception ignored) { }
        }
    }
}
