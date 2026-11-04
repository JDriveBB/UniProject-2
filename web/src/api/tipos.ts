/** Tipos de la API (docs/openapi/openapi.yaml). Las pantallas 26a–26f agregan aquí los que usen. */
export type RolPlataforma = 'ADMINISTRADOR' | 'DOCENTE' | 'ESTUDIANTE' | 'MONITOR';

export interface RespuestaError {
  codigo: string;
  mensaje: string;
  detalles: string[];
}

export interface UsuarioSesion {
  idUsuario: number;
  nombre: string;
  email: string;
  rol: RolPlataforma;
  telegramVinculado: boolean;
}
