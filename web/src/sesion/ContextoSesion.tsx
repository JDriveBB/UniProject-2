import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { api, EVENTO_SESION_EXPIRADA } from '../api/cliente';
import type { UsuarioSesion } from '../api/tipos';

export type EstadoSesion =
  | { estado: 'cargando' }
  | { estado: 'anonimo' }
  | { estado: 'autenticado'; usuario: UsuarioSesion };

interface ValorSesion {
  sesion: EstadoSesion;
  ingresar(email: string, contrasena: string): Promise<UsuarioSesion>;
  salir(): Promise<void>;
  refrescar(): Promise<UsuarioSesion>;
}

const ContextoSesion = createContext<ValorSesion | null>(null);

/** El usuario vive solo en memoria (decisión 25-9); al recargar se vuelve a pedir /api/sesion/yo. */
export function ProveedorSesion({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<EstadoSesion>({ estado: 'cargando' });

  useEffect(() => {
    let vigente = true;
    api.prepararCsrf()
      .then(() => api.get<UsuarioSesion>('/api/sesion/yo'))
      .then((usuario) => {
        if (vigente) {
          setSesion({ estado: 'autenticado', usuario });
        }
      })
      .catch(() => {
        if (vigente) {
          setSesion({ estado: 'anonimo' });
        }
      });
    const alExpirar = () => setSesion({ estado: 'anonimo' });
    window.addEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
    return () => {
      vigente = false;
      window.removeEventListener(EVENTO_SESION_EXPIRADA, alExpirar);
    };
  }, []);

  const ingresar = useCallback(async (email: string, contrasena: string) => {
    const usuario = await api.post<UsuarioSesion>('/api/sesion/login', { email, contrasena });
    await api.prepararCsrf();
    setSesion({ estado: 'autenticado', usuario });
    return usuario;
  }, []);

  const salir = useCallback(async () => {
    try {
      await api.post<void>('/api/sesion/logout');
    } finally {
      await api.prepararCsrf().catch(() => undefined);
      setSesion({ estado: 'anonimo' });
    }
  }, []);

  const refrescar = useCallback(async () => {
    const usuario = await api.get<UsuarioSesion>('/api/sesion/yo');
    setSesion({ estado: 'autenticado', usuario });
    return usuario;
  }, []);

  const valor = useMemo(() => ({ sesion, ingresar, salir, refrescar }), [sesion, ingresar, salir, refrescar]);
  return <ContextoSesion value={valor}>{children}</ContextoSesion>;
}

export function useSesion(): ValorSesion {
  const valor = useContext(ContextoSesion);
  if (valor === null) {
    throw new Error('useSesion se usó fuera de ProveedorSesion');
  }
  return valor;
}

/** Solo dentro de una RutaProtegida. */
export function useUsuario(): UsuarioSesion {
  const { sesion } = useSesion();
  if (sesion.estado !== 'autenticado') {
    throw new Error('useUsuario se usó sin sesión');
  }
  return sesion.usuario;
}
