package com.grupo5e.morapack.tabusearch.reporte;

import com.grupo5e.morapack.tabusearch.config.ConfiguracionTabu;
import com.grupo5e.morapack.tabusearch.evaluacion.EvaluadorSolucion;
import com.grupo5e.morapack.tabusearch.modelo.AsignacionEnvio;
import com.grupo5e.morapack.tabusearch.modelo.Envio;
import com.grupo5e.morapack.tabusearch.modelo.Ruta;
import com.grupo5e.morapack.tabusearch.modelo.Solucion;
import com.grupo5e.morapack.tabusearch.modelo.Vuelo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Imprime por terminal y guarda en archivo un reporte legible de la
 * planificacion obtenida.
 *
 * Contenido por envio:
 *   - id, origen -> destino, cantidad, plazo maximo aplicable
 *   - instante de envio, minuto de llegada final, tiempo en red
 *   - indicador [OK] o [TARDE] segun cumpla o no
 *   - lista de vuelos usados en la ruta (origen-destino, horas locales)
 *
 * Cierra con un resumen: total, a tiempo, tarde, sin ruta y valor objetivo.
 *
 * El archivo de salida es texto plano UTF-8 para que sea facil de revisar,
 * copiar en el informe del caso o adjuntar en el entregable.
 */
public class GeneradorReporte {

    private final ConfiguracionTabu cfg;
    private final EvaluadorSolucion evaluador;

    public GeneradorReporte(ConfiguracionTabu cfg, EvaluadorSolucion evaluador) {
        this.cfg = cfg;
        this.evaluador = evaluador;
    }

    /**
     * Imprime en stdout y guarda el reporte en la ruta indicada.
     *
     * @param sol        solucion final del TabuSearch.
     * @param rutaArchivo archivo destino del reporte (se sobrescribe).
     */
    public void generar(Solucion sol, Path rutaArchivo) throws IOException {

        StringBuilder terminal = new StringBuilder();
        StringBuilder archivo = new StringBuilder();

        String encabezado = String.format(
                "%n============== REPORTE TABUSEARCH - RUTAS POR ENVIO ==============%n" +
                        "Total envios         : %d%n" +
                        "Entregados a tiempo  : %d%n" +
                        "Entregados TARDE     : %d%n" +
                        "Sin ruta asignada    : %d%n" +
                        "Valor objetivo final : %.2f%n" +
                        "==================================================================%n%n",
                sol.tamano(), sol.getEnviosEntregadosATiempo(),
                sol.getEnviosTarde(), sol.getEnviosSinRuta(),
                sol.getValorObjetivo());
        terminal.append(encabezado);
        archivo.append(encabezado);

        List<AsignacionEnvio> asignaciones = sol.lista();
        for (AsignacionEnvio a : asignaciones) {
            String bloque = formatearAsignacion(a);
            terminal.append(bloque).append('\n');
            archivo.append(bloque).append('\n');
        }

        // Pie con estadistica por estado
        int tarde = sol.getEnviosTarde();
        int sinR = sol.getEnviosSinRuta();
        int ok = sol.getEnviosEntregadosATiempo();
        String pie = String.format(
                "%n=== RESUMEN FINAL ===%n" +
                        "  A tiempo : %d%n" +
                        "  Tarde    : %d%n" +
                        "  Sin ruta : %d%n" +
                        "  Total    : %d%n",
                ok, tarde, sinR, sol.tamano());
        terminal.append(pie);
        archivo.append(pie);

        // Volcar a terminal
        System.out.println(terminal);

        // Volcar a archivo
        Files.createDirectories(rutaArchivo.toAbsolutePath().getParent());
        try (BufferedWriter w = Files.newBufferedWriter(rutaArchivo, StandardCharsets.UTF_8)) {
            w.write(archivo.toString());
        }
        System.out.println("[REPORTE] Guardado en: " + rutaArchivo.toAbsolutePath());
    }

    private String formatearAsignacion(AsignacionEnvio a) {
        Envio e = a.getEnvio();
        Ruta r = a.getRuta();
        int plazo = evaluador.calcularPlazo(e);
        String marca;
        if (r == null || r.esVacia()) marca = "[SIN RUTA]";
        else if (a.isLlegaATiempo()) marca = "[OK]";
        else marca = "[TARDE]";

        StringBuilder sb = new StringBuilder();
        sb.append("Envio ").append(e.getId())
                .append("  ").append(e.getOrigen()).append(" -> ").append(e.getDestino())
                .append("  x").append(e.getCantidad())
                .append("  plazo=").append(plazo).append("min")
                .append("  ").append(marca)
                .append('\n');
        sb.append("  instante generacion (GMT min): ").append(e.getInstanteGeneracionMinGmt())
                .append('\n');
        if (r == null || r.esVacia()) {
            sb.append("  RUTA: (no se pudo planificar una ruta viable)\n");
        } else {
            sb.append("  llegada final (GMT min)       : ").append(r.minutoLlegada()).append('\n');
            sb.append("  tiempo en red (con liberacion): ").append(a.getTiempoEntregaMin()).append(" min\n");
            int idx = 1;
            for (Vuelo v : r.getVuelos()) {
                sb.append("    Tramo ").append(idx++).append(": ")
                        .append(v.getOrigen()).append(" -> ").append(v.getDestino())
                        .append(" local ").append(v.getHoraSalidaLocalTxt()).append(" -> ")
                        .append(v.getHoraLlegadaLocalTxt())
                        .append("  [GMT ").append(v.getSalidaMinGmt()).append("->")
                        .append(v.getLlegadaMinGmt()).append("]")
                        .append("  cap=").append(v.getCapacidadMaxima())
                        .append('\n');
            }
        }
        return sb.toString();
    }
}
