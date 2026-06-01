import { MessageSquare, Search } from 'lucide-react';
import { useState } from 'react';

/**
 * Panel lateral con la lista de conversaciones. Cada item muestra:
 *  - Avatar con la inicial del cliente
 *  - Nombre + último mensaje (truncado)
 *  - Hora del último mensaje
 *  - Badge naranja con número de no-leídos (si los hay)
 *
 * Click en una conversación dispara onSelect(clienteUsuarioId).
 */
export function ConversacionesLista({ conversaciones, seleccionadaId, onSelect, loading }) {
  const [busqueda, setBusqueda] = useState('');

  const filtradas = (conversaciones || []).filter(c =>
    !busqueda.trim() ||
    (c.clienteNombre || '').toLowerCase().includes(busqueda.toLowerCase())
  );

  return (
    <div className="hud-panel flex flex-col h-full overflow-hidden">
      {/* Cabecera */}
      <div className="p-3 border-b border-cockpit-line">
        <p className="hud-label mb-2">// CONVERSACIONES</p>
        <div className="relative">
          <Search size={12} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar…"
            className="hud-input !pl-7 !py-1.5 !text-xs"
          />
        </div>
      </div>

      {/* Lista */}
      <div className="flex-1 overflow-y-auto">
        {loading && conversaciones?.length === 0 ? (
          Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="p-3 border-b border-cockpit-line/30">
              <div className="h-3 w-32 bg-white/5 animate-pulse rounded mb-2" />
              <div className="h-2 w-48 bg-white/5 animate-pulse rounded" />
            </div>
          ))
        ) : filtradas.length === 0 ? (
          <div className="p-6 text-center">
            <MessageSquare size={28} className="mx-auto opacity-30 text-slate-500 mb-2" />
            <p className="hud-label text-slate-500">// SIN CONVERSACIONES</p>
          </div>
        ) : (
          filtradas.map((c) => (
            <ConversacionItem
              key={c.clienteUsuarioId}
              conv={c}
              activa={seleccionadaId === c.clienteUsuarioId}
              onClick={() => onSelect(c.clienteUsuarioId, c)}
            />
          ))
        )}
      </div>
    </div>
  );
}

function ConversacionItem({ conv, activa, onClick }) {
  const inicial = (conv.clienteNombre?.[0] || '?').toUpperCase();
  const hora = conv.fechaUltimoMensaje
    ? new Date(conv.fechaUltimoMensaje).toLocaleTimeString('es-ES', {
        hour: '2-digit', minute: '2-digit'
      })
    : '';

  return (
    <button
      onClick={onClick}
      className={[
        'w-full p-3 border-b border-cockpit-line/30 text-left transition-colors',
        'flex items-start gap-3',
        activa
          ? 'bg-cockpit-neon/10 border-l-2 border-l-cockpit-neon'
          : 'hover:bg-white/[0.025]',
      ].join(' ')}
    >
      <div
        className={[
          'w-9 h-9 grid place-items-center font-display border shrink-0 mt-0.5',
          activa
            ? 'border-cockpit-neon text-cockpit-neon bg-cockpit-neon/15'
            : 'border-cockpit-line text-slate-400',
        ].join(' ')}
      >
        {inicial}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-2">
          <span className="text-sm text-white truncate">
            {conv.clienteNombre || 'Cliente'}
          </span>
          <span className="hud-readout text-[10px] text-slate-500 shrink-0">{hora}</span>
        </div>
        <p className="text-xs text-slate-400 truncate mt-0.5">
          {conv.ultimoMensaje || <span className="italic opacity-60">— sin mensajes —</span>}
        </p>
      </div>
      {conv.noLeidos > 0 && (
        <span
          className="ml-1 px-1.5 py-0.5 text-[10px] font-mono font-bold text-cockpit-bg shrink-0 mt-0.5"
          style={{
            background: '#fbbf24',
            boxShadow: '0 0 10px rgba(251,191,36,0.6)',
          }}
        >
          {conv.noLeidos > 99 ? '99+' : conv.noLeidos}
        </span>
      )}
    </button>
  );
}
