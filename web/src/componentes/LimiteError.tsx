import { Component } from 'react';
import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { Alerta } from './Alerta';
import { Boton } from './Boton';
import { EncabezadoPagina } from './EncabezadoPagina';

/**
 * Protege el contenido del marco ante fallos de renderizado para conservar la barra superior.
 * Se usa un componente de clase porque BrowserRouter no admite errorElement de data routers.
 */
export class LimiteError extends Component<{ children: ReactNode }, { fallo: boolean }> {
  state = { fallo: false };

  static getDerivedStateFromError(): { fallo: boolean } {
    return { fallo: true };
  }

  render() {
    if (this.state.fallo) {
      return (
        <div className="pagina">
          <EncabezadoPagina titulo="No se pudo mostrar esta pantalla" />
          <Alerta tipo="error">
            Ocurrió un error inesperado al mostrar esta pantalla. Intenta de nuevo; si sigue pasando, vuelve al inicio.
          </Alerta>
          <div className="pagina__acciones">
            <Boton onClick={() => this.setState({ fallo: false })}>Reintentar</Boton>
            <Link to="/" className="boton boton--secundario">
              Ir al inicio
            </Link>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
