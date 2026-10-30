package co.uniproject.nucleo.ejecucion.dominio;

import co.uniproject.nucleo.ejecucion.dependencias.NivelRiesgo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Tabla Tarea del diagrama de BD. */
@Entity
@Table(name = "tarea")
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarea")
    private Long idTarea;

    @Column(name = "id_hito_equipo")
    private Long idHitoEquipo;

    @Column(name = "id_encargado")
    private Long idEncargado;

    @Column(name = "id_tarea_origen")
    private Long idTareaOrigen;

    @Column(name = "titulo")
    private String titulo;

    @Column(name = "descripcion")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado")
    private EstadoTarea estado = EstadoTarea.PENDIENTE;

    @Column(name = "fecha_cambio_estado")
    private Instant fechaCambioEstado;

    @Column(name = "bloqueada_por_dependencia")
    private Boolean bloqueadaPorDependencia = false;

    @Column(name = "fecha_bloqueo")
    private Instant fechaBloqueo;

    @Column(name = "edicion_habilitada")
    private Boolean edicionHabilitada = true;

    @Column(name = "porcentaje_avance")
    private Integer porcentajeAvance = 0;

    @Column(name = "horas_estimadas")
    private BigDecimal horasEstimadas;

    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Column(name = "fecha_completada")
    private Instant fechaCompletada;

    @Column(name = "es_prerrequisito_critico")
    private Boolean esPrerrequisitoCritico = false;

    @Column(name = "en_ruta_critica")
    private Boolean enRutaCritica = false;

    @Column(name = "holgura_horas")
    private BigDecimal holguraHoras;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_riesgo")
    private NivelRiesgo nivelRiesgo;

    @Column(name = "peso")
    private BigDecimal peso;

    public Long getIdTarea() {
        return idTarea;
    }

    public void setIdTarea(Long idTarea) {
        this.idTarea = idTarea;
    }

    public Long getIdHitoEquipo() {
        return idHitoEquipo;
    }

    public void setIdHitoEquipo(Long idHitoEquipo) {
        this.idHitoEquipo = idHitoEquipo;
    }

    public Long getIdEncargado() {
        return idEncargado;
    }

    public void setIdEncargado(Long idEncargado) {
        this.idEncargado = idEncargado;
    }

    public Long getIdTareaOrigen() {
        return idTareaOrigen;
    }

    public void setIdTareaOrigen(Long idTareaOrigen) {
        this.idTareaOrigen = idTareaOrigen;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public EstadoTarea getEstado() {
        return estado;
    }

    public void setEstado(EstadoTarea estado) {
        this.estado = estado;
    }

    public Instant getFechaCambioEstado() {
        return fechaCambioEstado;
    }

    public void setFechaCambioEstado(Instant fechaCambioEstado) {
        this.fechaCambioEstado = fechaCambioEstado;
    }

    public Boolean getBloqueadaPorDependencia() {
        return bloqueadaPorDependencia;
    }

    public void setBloqueadaPorDependencia(Boolean bloqueadaPorDependencia) {
        this.bloqueadaPorDependencia = bloqueadaPorDependencia;
    }

    public Instant getFechaBloqueo() {
        return fechaBloqueo;
    }

    public void setFechaBloqueo(Instant fechaBloqueo) {
        this.fechaBloqueo = fechaBloqueo;
    }

    public Boolean getEdicionHabilitada() {
        return edicionHabilitada;
    }

    public void setEdicionHabilitada(Boolean edicionHabilitada) {
        this.edicionHabilitada = edicionHabilitada;
    }

    public Integer getPorcentajeAvance() {
        return porcentajeAvance;
    }

    public void setPorcentajeAvance(Integer porcentajeAvance) {
        this.porcentajeAvance = porcentajeAvance;
    }

    public BigDecimal getHorasEstimadas() {
        return horasEstimadas;
    }

    public void setHorasEstimadas(BigDecimal horasEstimadas) {
        this.horasEstimadas = horasEstimadas;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public Instant getFechaCompletada() {
        return fechaCompletada;
    }

    public void setFechaCompletada(Instant fechaCompletada) {
        this.fechaCompletada = fechaCompletada;
    }

    public Boolean getEsPrerrequisitoCritico() {
        return esPrerrequisitoCritico;
    }

    public void setEsPrerrequisitoCritico(Boolean esPrerrequisitoCritico) {
        this.esPrerrequisitoCritico = esPrerrequisitoCritico;
    }

    public Boolean getEnRutaCritica() {
        return enRutaCritica;
    }

    public void setEnRutaCritica(Boolean enRutaCritica) {
        this.enRutaCritica = enRutaCritica;
    }

    public BigDecimal getHolguraHoras() {
        return holguraHoras;
    }

    public void setHolguraHoras(BigDecimal holguraHoras) {
        this.holguraHoras = holguraHoras;
    }

    public NivelRiesgo getNivelRiesgo() {
        return nivelRiesgo;
    }

    public void setNivelRiesgo(NivelRiesgo nivelRiesgo) {
        this.nivelRiesgo = nivelRiesgo;
    }

    public BigDecimal getPeso() {
        return peso;
    }

    public void setPeso(BigDecimal peso) {
        this.peso = peso;
    }
}
