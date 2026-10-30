package co.uniproject.agente.plantillas;

/** Aviso listo para Telegram: html ya escapado, texto del botón y ruta relativa (de RutasWeb). */
public record Aviso(String html, String textoBoton, String ruta) {}
