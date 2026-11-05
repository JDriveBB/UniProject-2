package co.uniproject.nucleo.seguridad.cuentas;

import co.uniproject.nucleo.comun.error.RecursoNoEncontradoException;
import co.uniproject.nucleo.notificaciones.PersonasEnEventos;
import co.uniproject.nucleo.seguridad.dominio.Usuario;
import co.uniproject.nucleo.seguridad.repositorio.UsuarioRepositorio;
import co.uniproject.nucleo.trazabilidad.auditoria.AccionAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.EntradaAuditoria;
import co.uniproject.nucleo.trazabilidad.auditoria.RegistroAuditoria;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Derechos de conocer y rectificar (Ley 1581 de 2012, art. 8) desde el perfil. La supresión está en ServicioSupresion. */
@Service
public class ServicioPerfil {

    private static final String NO_EXISTE = "La cuenta ya no existe";

    private final UsuarioRepositorio usuarios;
    private final JdbcClient jdbc;
    private final ServicioCuentas cuentas;
    private final RegistroAuditoria auditoria;
    private final PersonasEnEventos eventos;

    public ServicioPerfil(UsuarioRepositorio usuarios, JdbcClient jdbc, ServicioCuentas cuentas,
                          RegistroAuditoria auditoria, PersonasEnEventos eventos) {
        this.usuarios = usuarios;
        this.jdbc = jdbc;
        this.cuentas = cuentas;
        this.auditoria = auditoria;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    public DatosPersonales datos(long idUsuario) {
        Usuario u = usuarios.findById(idUsuario).orElseThrow(() -> new RecursoNoEncontradoException(NO_EXISTE));
        return new DatosPersonales(u.getIdUsuario(), u.getNombre(), u.getEmail(), u.getRolPlataforma(),
                u.getTelegramChatId() != null, Boolean.TRUE.equals(u.getAutorizaTratamientoDatos()),
                u.getFechaAutorizacion(), u.getFechaRegistro());
    }

    /** Rectifica el nombre y lo propaga, por id, a la outbox y a los avisos que salieron de ella. La auditoría no guarda ningún nombre. */
    @Transactional
    public UsuarioSesion cambiarNombre(long idUsuario, String nombreSolicitado) {
        String nuevo = nombreSolicitado.strip();
        String anterior = jdbc.sql("SELECT nombre FROM usuario WHERE id_usuario = :id FOR UPDATE")
                .param("id", idUsuario)
                .query(String.class)
                .optional()
                .orElseThrow(() -> new RecursoNoEncontradoException(NO_EXISTE));
        if (!anterior.equals(nuevo)) {
            jdbc.sql("UPDATE usuario SET nombre = :nombre WHERE id_usuario = :id")
                    .param("nombre", nuevo)
                    .param("id", idUsuario)
                    .update();
            eventos.reemplazar(idUsuario, nuevo, false);
            auditoria.registrar(EntradaAuditoria.cambio(idUsuario, "usuario", idUsuario, AccionAuditoria.ACTUALIZAR,
                    "nombre", null, null));
        }
        return cuentas.sesion(idUsuario);
    }
}
