/**
 * Badge HUD para mostrar un estado coloreado.
 * Soporta estados de pedido y de devolución.
 */
const ESTILOS = {
  // Pedidos
  PENDIENTE:  { color: '#fbbf24', label: 'Pendiente' },
  PAGADO:     { color: '#22d3ee', label: 'Pagado' },
  ENVIADO:    { color: '#a855f7', label: 'Enviado' },
  ENTREGADO:  { color: '#34d399', label: 'Entregado' },
  CANCELADO:  { color: '#ef4444', label: 'Cancelado' },
  // Devoluciones
  SOLICITADA: { color: '#fbbf24', label: 'Solicitada' },
  APROBADA:   { color: '#22d3ee', label: 'Aprobada' },
  RECHAZADA:  { color: '#ef4444', label: 'Rechazada' },
  COMPLETADA: { color: '#34d399', label: 'Completada' },
};

export function EstadoBadge({ estado, size = 'md', dot = true }) {
  const s = ESTILOS[estado] || { color: '#6b7280', label: estado };

  const sizes = {
    sm: 'px-1.5 py-0.5 text-[9px]',
    md: 'px-2 py-0.5 text-[10px]',
    lg: 'px-3 py-1 text-xs',
  };

  return (
    <span
      className={`${sizes[size]} inline-flex items-center gap-1.5 tracking-widest uppercase font-display border`}
      style={{
        borderColor: s.color + '66',
        color: s.color,
        background: s.color + '10',
        boxShadow: `0 0 12px ${s.color}33`,
      }}
    >
      {dot && (
        <span
          className="w-1.5 h-1.5 rounded-full"
          style={{
            background: s.color,
            boxShadow: `0 0 6px ${s.color}`,
            animation: estado === 'PENDIENTE' || estado === 'SOLICITADA'
              ? 'pulse 2s infinite'
              : undefined,
          }}
        />
      )}
      {s.label}
    </span>
  );
}
