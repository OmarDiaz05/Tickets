package fes.proyecto.tickets.ptickets.helpers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;

class FolioGeneratorTest {

    @ParameterizedTest
    @CsvSource({
            "1,      FESA-2026-001",
            "10,     FESA-2026-00A",
            "35,     FESA-2026-00Z",
            "36,     FESA-2026-010",
            "397,    FESA-2026-0B1",
            "999,    FESA-2026-0RR",
            "1000,   FESA-2026-0RS",
            "46655,  FESA-2026-ZZZ",
            "46656,  FESA-2026-1000",
            "1679616, FESA-2026-10000"
    })
    void generar_ConsecutivoEnBase36ConAlMenos3Caracteres(int consecutivo, String esperado) {
        assertEquals(esperado, FolioGenerator.generar(2026, consecutivo));
    }

    @Test
    void generar_PasaDeLaCapacidadDe3CaracteresSinTruncar() {
        String folio = FolioGenerator.generar(2026, 46656);

        assertEquals("FESA-2026-1000", folio);
        assertEquals(46656, FolioGenerator.consecutivoDe(folio));
    }

    @Test
    void generar_ConsecutivoMaximoDeInt_CabeEnLaColumnaDe20() {
        String folio = FolioGenerator.generar(2026, Integer.MAX_VALUE);

        assertTrue(folio.length() <= 20, "El folio debe caber en la columna folio (length 20)");
        assertEquals(Integer.MAX_VALUE, FolioGenerator.consecutivoDe(folio));
    }

    @Test
    void generar_SoloDigitosYLetrasMayusculas() {
        for (int i = 1; i <= 2000; i++) {
            assertTrue(FolioGenerator.generar(2026, i).matches("FESA-2026-[0-9A-Z]{3,}"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void generar_ConsecutivoMenorA1_LanzaExcepcion(int consecutivo) {
        assertThrows(IllegalArgumentException.class, () -> FolioGenerator.generar(2026, consecutivo));
    }

    @Test
    void siguiente_SinTicketsEnElAnio_EmpiezaEn001() {
        assertEquals("FESA-2026-001", FolioGenerator.siguiente(2026, 0));
    }

    @Test
    void siguiente_IncrementaElConsecutivoMaximo() {
        assertEquals("FESA-2026-0B2", FolioGenerator.siguiente(2026, 397));
        assertEquals("FESA-2026-1000", FolioGenerator.siguiente(2026, 46655));
    }

    @Test
    void siguiente_SinAnio_UsaElAnioActual() {
        assertEquals("FESA-" + Year.now().getValue() + "-001", FolioGenerator.siguiente(0));
    }

    @Test
    void prefijoDelAnio_RetornaParteFija() {
        assertEquals("FESA-2027-", FolioGenerator.prefijoDelAnio(2027));
    }

    @Test
    void consecutivoDe_EsInversoDeGenerar() {
        for (int i = 1; i <= 50000; i += 97) {
            assertEquals(i, FolioGenerator.consecutivoDe(FolioGenerator.generar(2026, i)));
        }
    }

    @Test
    void ordenAlfabetico_CoincideConOrdenNumericoAIgualLongitud() {
        // El repositorio ordena por longitud y luego alfabéticamente para hallar el máximo
        for (int i = 1; i < 46655; i += 13) {
            String actual = FolioGenerator.generar(2026, i);
            String siguiente = FolioGenerator.generar(2026, i + 1);
            assertTrue(actual.compareTo(siguiente) < 0, actual + " debe ir antes que " + siguiente);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "FESA-2026-", "OTRO-2026-001", "FESA-2026-0B-", "FESA-2026-#1"})
    void consecutivoDe_FolioInvalido_LanzaExcepcion(String folio) {
        assertThrows(IllegalArgumentException.class, () -> FolioGenerator.consecutivoDe(folio));
    }

    @Test
    void consecutivoDe_FolioNulo_LanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> FolioGenerator.consecutivoDe(null));
    }
}
