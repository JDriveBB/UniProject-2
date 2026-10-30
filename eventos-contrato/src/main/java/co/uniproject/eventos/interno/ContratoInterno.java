package co.uniproject.eventos.interno;

/**
 * Endpoint interno del núcleo (decisiones-tecnicas-java §4). Cuerpos JSON; respuestas sin cuerpo.
 * POST RUTA_VINCULACIONES: 204 vinculado · 400 cuerpo inválido · 401 token ausente o incorrecto · 404 código inexistente · 410 código usado o vencido.
 * POST RUTA_BLOQUEOS:      204 (siempre, exista o no el chat) · 400 cuerpo inválido · 401 token.
 */
public final class ContratoInterno {
    private ContratoInterno() {}
    public static final String CABECERA_TOKEN = "X-UniProject-Token-Interno";
    public static final String RUTA_VINCULACIONES = "/interno/telegram/vinculaciones";
    public static final String RUTA_BLOQUEOS = "/interno/telegram/bloqueos";
    /** Código del enlace profundo: 24 bytes aleatorios en Base64 URL sin relleno. */
    public static final String PATRON_CODIGO = "^[A-Za-z0-9_-]{32}$";
}
