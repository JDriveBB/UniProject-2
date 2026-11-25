-- V10: endurece el CHECK de V9. Con selector puesto y hash NULL, octet_length(NULL) daba NULL y el CHECK pasaba.
-- Ahora la rama con enlace exige selector y verificador no nulos, además de sus longitudes y el vencimiento.
ALTER TABLE correo_habilitado DROP CONSTRAINT correo_habilitado_enlace_ck;

ALTER TABLE correo_habilitado
    ADD CONSTRAINT correo_habilitado_enlace_ck CHECK (
        (enlace_selector IS NULL AND enlace_verificador_hash IS NULL AND enlace_vence_en IS NULL)
        OR (enlace_selector IS NOT NULL
            AND enlace_verificador_hash IS NOT NULL
            AND octet_length(enlace_selector) = 8
            AND octet_length(enlace_verificador_hash) = 32
            AND enlace_vence_en IS NOT NULL));
