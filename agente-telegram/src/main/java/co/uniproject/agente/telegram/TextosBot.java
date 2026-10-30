package co.uniproject.agente.telegram;

public final class TextosBot {

    private TextosBot() {}

    public static final String VINCULADO = "Listo: tu cuenta de UniProject quedó vinculada. Desde ahora recibirás aquí los avisos de tus proyectos.";
    public static final String CODIGO_INEXISTENTE = "Ese código de vinculación no existe. Genera uno nuevo con el botón «Vincular Telegram» de tu perfil en UniProject.";
    public static final String CODIGO_VENCIDO = "Ese código ya se usó o venció (dura 15 minutos). Genera uno nuevo con el botón «Vincular Telegram» de tu perfil en UniProject.";
    public static final String ERROR = "No pudimos vincular tu cuenta en este momento. Intenta de nuevo en unos minutos.";
    public static final String SIN_CODIGO = "Para vincular tu cuenta, usa el botón «Vincular Telegram» de tu perfil en UniProject.";
    public static final String SOLO_AVISOS = "Este bot solo envía avisos de UniProject";
}
