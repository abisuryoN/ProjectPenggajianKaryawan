package service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

/** Unit test aturan penggajian (BAGIAN A §5). */
public class PenggajianServiceTest {

    private final PenggajianService svc = new PenggajianService();

    @Test
    void rumusDasarPokokPlusTunjanganMinusPotongan() {
        BigDecimal total = PenggajianService.hitungTotal(
                new BigDecimal("5000000"), new BigDecimal("1000000"), new BigDecimal("500000"));
        assertEquals(new BigDecimal("5500000"), total);
    }

    @Test
    void semuaNol() {
        assertEquals(new BigDecimal("0"), PenggajianService.hitungTotal(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void periodeFormatValid() {
        assertDoesNotThrow(() -> PenggajianService.validasiPeriode("2026-01"));
        assertDoesNotThrow(() -> PenggajianService.validasiPeriode("2026-12"));
    }

    @Test
    void periodeFormatInvalidDitolak() {
        assertThrows(Exception.class, () -> PenggajianService.validasiPeriode("2026-13"));
        assertThrows(Exception.class, () -> PenggajianService.validasiPeriode("2026"));
        assertThrows(Exception.class, () -> PenggajianService.validasiPeriode("abcd-xx"));
        assertThrows(Exception.class, () -> PenggajianService.validasiPeriode(null));
    }

    @Test
    void nominalNegatifDitolak() {
        assertThrows(Exception.class, () -> svc.validasiSebelumSimpan(
                1, "2026-10", new BigDecimal("-100"), BigDecimal.ZERO, BigDecimal.ZERO, true));
        assertThrows(Exception.class, () -> svc.validasiSebelumSimpan(
                1, "2026-10", BigDecimal.ZERO, new BigDecimal("-1"), BigDecimal.ZERO, true));
        assertThrows(Exception.class, () -> svc.validasiSebelumSimpan(
                1, "2026-10", BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-5"), true));
    }

    @Test
    void karyawanNolDitolak() {
        assertThrows(Exception.class, () -> svc.validasiSebelumSimpan(
                0, "2026-10", BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, true));
    }
}
