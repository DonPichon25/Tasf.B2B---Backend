package com.grupo5e.morapack.tabusearch;

import com.grupo5e.morapack.tabusearch.algoritmo.BuscadorRutas;
import com.grupo5e.morapack.tabusearch.algoritmo.ConstructorInicial;
import com.grupo5e.morapack.tabusearch.algoritmo.EstadoCapacidades;
import com.grupo5e.morapack.tabusearch.algoritmo.GrafoVuelos;
import com.grupo5e.morapack.tabusearch.algoritmo.TabuSearchSolver;
import com.grupo5e.morapack.tabusearch.config.ConfiguracionTabu;
import com.grupo5e.morapack.tabusearch.evaluacion.EvaluadorSolucion;
import com.grupo5e.morapack.tabusearch.io.CargadorAeropuertos;
import com.grupo5e.morapack.tabusearch.io.CargadorEnvios;
import com.grupo5e.morapack.tabusearch.io.CargadorVuelos;
import com.grupo5e.morapack.tabusearch.modelo.Aeropuerto;
import com.grupo5e.morapack.tabusearch.modelo.Envio;
import com.grupo5e.morapack.tabusearch.modelo.Solucion;
import com.grupo5e.morapack.tabusearch.modelo.Vuelo;
import com.grupo5e.morapack.tabusearch.reporte.GeneradorReporte;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Punto de entrada del planificador basado en TabuSearch para el caso
 * TASF-B2B (c.1inf54.26-1.b.situacion.autentica_v20260324).
 *
 * Flujo:
 *   1. Cargar configuracion y resolver rutas de archivos del caso.
 *   2. Leer aeropuertos.txt (UTF-16 BE).
 *   3. Leer planes_vuelo.txt, normalizando horarios a GMT.
 *      Se carga en tandas: una primera tanda y, si hace falta mas, otra.
 *   4. Leer _envios_XXXX_.txt (30 archivos) respetando limites de carga.
 *   5. Construir solucion inicial factible con heuristica BFS.
 *   6. Optimizar con TabuSearch.
 *   7. Imprimir reporte de rutas por envio en consola y archivo.
 *
 * Argumentos opcionales (posicionales):
 *   args[0] : directorio con aeropuertos.txt, planes_vuelo.txt y
 *             subcarpeta _envios_preliminar (default: Caso/DATA)
 *   args[1] : archivo de reporte a generar (default: reporte_rutas_tabusearch.txt)
 *
 * Uso tipico:
 *   java com.grupo5e.morapack.tabusearch.MainTabuSearch Caso/DATA reporte.txt
 */
public class MainTabuSearch {

    public static void main(String[] args) throws Exception {
        ConfiguracionTabu cfg = new ConfiguracionTabu();
        if (args.length > 0) cfg.dirDatos = args[0];
        if (args.length > 1) cfg.archivoReporte = args[1];

        Path dir = Paths.get(cfg.dirDatos);
        Path archAeropuertos = dir.resolve("aeropuertos.txt");
        Path archVuelos = dir.resolve("planes_vuelo.txt");
        Path dirEnvios = dir.resolve("_envios_preliminar");

        System.out.println("[CFG] Directorio datos : " + dir.toAbsolutePath());
        System.out.println("[CFG] Plazos           : mismoCont=" + cfg.plazoMismoContinenteMin
                + "min, distintoCont=" + cfg.plazoDistintoContinenteMin + "min");
        System.out.println("[CFG] Escala/Liberacion: " + cfg.tiempoEscalaMin + "/"
                + cfg.tiempoLiberacionDestinoMin + " min");
        System.out.println("[CFG] Envios x archivo : " + cfg.maxEnviosPorArchivo
                + " (tope global " + cfg.limiteGlobalEnvios + ")");
        System.out.println("[CFG] Tabu max iter    : " + cfg.maxIteracionesTabu
                + "  lista=" + cfg.tamanoListaTabu);

        // 1) Aeropuertos
        Map<String, Aeropuerto> aeropuertos = new CargadorAeropuertos().cargar(archAeropuertos);
        System.out.println("[DATOS] Aeropuertos cargados: " + aeropuertos.size());

        // 2) Vuelos - en tandas (primero una tanda grande; si no alcanza, cargamos mas)
        CargadorVuelos cargVuelos = new CargadorVuelos();
        int tamTanda = 1500;
        CargadorVuelos.Tanda t1 = cargVuelos.cargarEnTandas(archVuelos, aeropuertos, tamTanda, 0);
        List<Vuelo> vuelos;
        if (t1.finArchivo) {
            vuelos = t1.vuelos;
        } else {
            // Tanda 2: el resto
            CargadorVuelos.Tanda t2 = cargVuelos.cargarEnTandas(archVuelos, aeropuertos,
                    Integer.MAX_VALUE, t1.siguienteIndice);
            vuelos = new java.util.ArrayList<>(t1.vuelos.size() + t2.vuelos.size());
            vuelos.addAll(t1.vuelos);
            vuelos.addAll(t2.vuelos);
        }
        System.out.println("[DATOS] Vuelos cargados     : " + vuelos.size());

        // 3) Envios
        CargadorEnvios.Resultado envRes = new CargadorEnvios().cargar(
                dirEnvios, aeropuertos, cfg.maxEnviosPorArchivo, cfg.limiteGlobalEnvios);
        List<Envio> envios = envRes.envios;
        System.out.println("[DATOS] Envios cargados     : " + envios.size()
                + "  (fechaBase=" + envRes.fechaBase + ")");

        // 4) Construccion grafos, evaluador, capacidades
        GrafoVuelos grafo = new GrafoVuelos(vuelos);
        EstadoCapacidades caps = new EstadoCapacidades(aeropuertos);
        EvaluadorSolucion eval = new EvaluadorSolucion(aeropuertos, cfg);
        BuscadorRutas buscador = new BuscadorRutas(grafo, cfg);

        // 5) Solucion inicial
        ConstructorInicial constructor = new ConstructorInicial(buscador, caps, eval, cfg);
        Solucion inicial = constructor.construir(envios);

        // 6) TabuSearch
        TabuSearchSolver solver = new TabuSearchSolver(cfg, buscador, caps, eval);
        Solucion mejor = solver.optimizar(inicial);

        // 7) Reporte
        GeneradorReporte reporte = new GeneradorReporte(cfg, eval);
        Path archReporte = Paths.get(cfg.archivoReporte).toAbsolutePath();
        reporte.generar(mejor, archReporte);

        System.out.println();
        System.out.println("[FIN] Planificacion TabuSearch completada.");
    }
}
