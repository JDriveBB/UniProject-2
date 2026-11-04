export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <div className="cargando" role="status" aria-live="polite">
      <span className="cargando__indicador" aria-hidden="true" />
      {texto}
    </div>
  );
}
