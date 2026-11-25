package co.uniproject.eventos.interno;

/**
 * Endpoint interno del núcleo (decisiones-tecnicas-java sección 4). Cuerpos JSON; respuestas sin cuerpo.
 * POST RUTA_VINCULACIONES: 204 vinculado · 400 cuerpo inválido · 401 token ausente o incorrecto · 404 código inexistente · 409 conflicto transitorio (el mismo chat se vinculó a la vez a otra cuenta; el código sigue sin usar: se puede reintentar) · 410 código usado o vencido.
 * POST RUTA_BLOQUEOS:      204 (siempre, exista o no el chat) · 400 cuerpo inválido · 401 token.
 * POST RUTA_RECUPERACIONES (bloque 28): 200 {"codigo"} (único endpoint interno con cuerpo; Cache-Control no-store) · 400 cuerpo inválido · 401 token · 404 solicitud inexistente · 410 vencida, usada, anulada, sin entregas, cuenta anonimizada o chat distinto del vinculado.
 */
public final class ContratoInterno {
    private ContratoInterno() {}
    public static final String CABECERA_TOKEN = "X-UniProject-Token-Interno";
    public static final String RUTA_VINCULACIONES = "/interno/telegram/vinculaciones";
    public static final String RUTA_BLOQUEOS = "/interno/telegram/bloqueos";
    public static final String RUTA_RECUPERACIONES = "/interno/telegram/recuperaciones";
    /** Código del enlace profundo: 24 bytes aleatorios en Base64 URL sin relleno. */
    public static final String PATRON_CODIGO = "^[A-Za-z0-9_-]{32}$";
    /** Código de recuperación de contraseña: 32 bytes aleatorios (selector 8 + verificador 24) en Base64 URL sin relleno. */
    public static final String PATRON_CODIGO_RECUPERACION = "^[A-Za-z0-9_-]{43}$";
}
