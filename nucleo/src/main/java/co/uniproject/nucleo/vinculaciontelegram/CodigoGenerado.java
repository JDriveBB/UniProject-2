package co.uniproject.nucleo.vinculaciontelegram;

import java.time.Instant;

/** enlace = https://t.me/<usuario-bot>?start=<codigo> (plan §8.8 paso 3). */
public record CodigoGenerado(String codigo, Instant venceEn, String enlace) {}
