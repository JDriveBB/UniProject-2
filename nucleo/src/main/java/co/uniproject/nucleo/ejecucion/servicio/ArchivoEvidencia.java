package co.uniproject.nucleo.ejecucion.servicio;

import java.nio.file.Path;

/** Lo que el controlador necesita para servir un archivo de evidencia. */
public record ArchivoEvidencia(Path ruta, String mime, String nombre) {}
