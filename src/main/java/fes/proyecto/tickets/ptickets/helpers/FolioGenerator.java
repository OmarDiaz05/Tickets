package fes.proyecto.tickets.ptickets.helpers;

import java.time.Year;
import java.util.Locale;

/**
 * Genera folios de ticket con el formato FESA-&lt;año&gt;-&lt;consecutivo&gt;.
 *
 * El consecutivo reinicia cada año y se escribe en base 36 (0-9 y A-Z, en
 * mayúsculas), con al menos 3 caracteres:
 * 1 → FESA-2026-001, 397 → FESA-2026-0B1, 46655 → FESA-2026-ZZZ,
 * 46656 → FESA-2026-1000 (crece sin truncar).
 *
 * Dentro de un mismo año y una misma longitud, el orden alfabético de los
 * folios coincide con el orden numérico del consecutivo.
 */
public final class FolioGenerator {

    public static final String PREFIJO = "FESA";
    private static final int BASE = 36;
    private static final int LONGITUD_MINIMA = 3;

    private FolioGenerator() {
    }

    /** "FESA-2026-": parte fija de todos los folios del año. */
    public static String prefijoDelAnio(int anio) {
        return PREFIJO + "-" + anio + "-";
    }

    /** Folio para el consecutivo indicado (1 en adelante). */
    public static String generar(int anio, int consecutivo) {
        if (consecutivo < 1) {
            throw new IllegalArgumentException("El consecutivo debe ser mayor o igual a 1");
        }
        String sufijo = Integer.toString(consecutivo, BASE).toUpperCase(Locale.ROOT);
        if (sufijo.length() < LONGITUD_MINIMA) {
            sufijo = "0".repeat(LONGITUD_MINIMA - sufijo.length()) + sufijo;
        }
        return prefijoDelAnio(anio) + sufijo;
    }

    /**
     * Siguiente folio del año a partir del consecutivo máximo ya usado
     * (0 si todavía no hay tickets ese año).
     */
    public static String siguiente(int anio, int consecutivoMaximo) {
        return generar(anio, consecutivoMaximo + 1);
    }

    /** Siguiente folio del año en curso. */
    public static String siguiente(int consecutivoMaximo) {
        return siguiente(Year.now().getValue(), consecutivoMaximo);
    }

    /** Extrae el consecutivo de un folio: "FESA-2026-0B1" → 397. */
    public static int consecutivoDe(String folio) {
        if (folio == null) {
            throw new IllegalArgumentException("El folio no puede ser nulo");
        }
        int separador = folio.lastIndexOf('-');
        if (!folio.startsWith(PREFIJO + "-") || separador < 0 || separador == folio.length() - 1) {
            throw new IllegalArgumentException("Folio con formato inválido: " + folio);
        }
        try {
            return Integer.parseInt(folio.substring(separador + 1), BASE);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Folio con formato inválido: " + folio, ex);
        }
    }
}
