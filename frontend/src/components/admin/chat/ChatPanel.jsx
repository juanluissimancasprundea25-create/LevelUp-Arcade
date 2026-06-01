import { useEffect, useRef, useState } from 'react';
import { Send, MessageSquare, ArrowDown } from 'lucide-react';
import { api } from '../../../lib/api.js';
import { useToast } from '../Toast.jsx';

/**
 * Panel principal del chat. Muestra:
 *  - Cabecera con el cliente seleccionado
 *  - Lista de mensajes con burbujas (yo a la derecha, otro a la izquierda)
 *  - Input de envío con Enter para mandar
 *
 * Polling cada 4 segundos para cargar nuevos mensajes.
 * Auto-scroll al fondo cuando llegan mensajes nuevos si ya estás abajo.
 */
export function ChatPanel({ clienteUsuarioId, conversacion, miId, onEnviado }) {
  const [mensajes, setMensajes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [texto, setTexto] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [mostrarBotonBajar, setMostrarBotonBajar] = useState(false);
  const scrollRef = useRef(null);
  const toast = useToast();

  // Cargar mensajes y poll cada 4s
  useEffect(() => {
    if (!clienteUsuarioId) {
      setMensajes([]);
      return;
    }
    let cancelado = false;

    async function cargar(esRefresco = false) {
      if (!esRefresco) setLoading(true);
      try {
        const { data } = await api.get(
          `/chat/conversaciones/${clienteUsuarioId}/mensajes`,
          { params: { size: 200, sort: 'fechaEnvio,asc' } }
        );
        if (cancelado) return;
        const lista = data?.content || data || [];
        setMensajes(lista);
      } catch (_) {
        // no toast en polling para no spammear
      } finally {
        if (!cancelado) setLoading(false);
      }
    }

    cargar();
    const iv = setInterval(() => cargar(true), 4000);
    return () => { cancelado = true; clearInterval(iv); };
  }, [clienteUsuarioId]);

  // Auto-scroll al fondo cuando cambian los mensajes (si ya estabas abajo)
  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    const cercaDelFondo = el.scrollHeight - el.scrollTop - el.clientHeight < 80;
    if (cercaDelFondo) {
      el.scrollTop = el.scrollHeight;
    }
  }, [mensajes.length]);

  function bajarAlFondo() {
    const el = scrollRef.current;
    if (!el) return;
    el.scrollTop = el.scrollHeight;
    setMostrarBotonBajar(false);
  }

  function onScroll() {
    const el = scrollRef.current;
    if (!el) return;
    const distancia = el.scrollHeight - el.scrollTop - el.clientHeight;
    setMostrarBotonBajar(distancia > 200);
  }

  async function enviar() {
    const contenido = texto.trim();
    if (!contenido || enviando || !clienteUsuarioId) return;
    setEnviando(true);
    try {
      const { data } = await api.post('/chat/mensajes', {
        destinatarioUsuarioId: clienteUsuarioId,
        contenido,
      });
      setTexto('');
      setMensajes((arr) => [...arr, data]);
      onEnviado && onEnviado();
      // forzar scroll al fondo tras enviar
      setTimeout(() => {
        if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
      }, 50);
    } catch (e) {
      toast.err(e.response?.data?.mensaje || 'No se pudo enviar');
    } finally {
      setEnviando(false);
    }
  }

  function onKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      enviar();
    }
  }

  // === Estado vacío ===
  if (!clienteUsuarioId) {
    return (
      <div className="hud-panel flex flex-col h-full items-center justify-center text-center p-10">
        <MessageSquare size={56} className="opacity-20 text-slate-500 mb-4" />
        <p className="hud-label text-slate-500">// SIN CONVERSACIÓN SELECCIONADA</p>
        <p className="text-xs text-slate-600 mt-2 max-w-sm">
          Selecciona una conversación de la lista para ver los mensajes y responder al cliente.
        </p>
      </div>
    );
  }

  return (
    <div className="hud-panel flex flex-col h-full overflow-hidden">
      {/* Cabecera */}
      <div className="p-3 border-b border-cockpit-line flex items-center gap-3">
        <div className="w-9 h-9 grid place-items-center font-display border border-cockpit-cyan/40 bg-cockpit-cyan/5 text-cockpit-cyan">
          {(conversacion?.clienteNombre?.[0] || '?').toUpperCase()}
        </div>
        <div className="flex-1 min-w-0">
          <p className="hud-label text-cockpit-cyan/70">// CHAT CON</p>
          <p className="text-sm text-white truncate font-display tracking-wide">
            {conversacion?.clienteNombre || 'Cliente'}
          </p>
        </div>
        <span className="hud-readout text-[10px] text-slate-500">
          {mensajes.length} {mensajes.length === 1 ? 'mensaje' : 'mensajes'}
        </span>
      </div>

      {/* Mensajes */}
      <div
        ref={scrollRef}
        onScroll={onScroll}
        className="flex-1 overflow-y-auto p-4 space-y-2 relative"
        style={{ scrollBehavior: 'smooth' }}
      >
        {loading && mensajes.length === 0 ? (
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className={i % 2 === 0 ? 'flex justify-start' : 'flex justify-end'}>
                <div className="h-10 w-48 bg-white/5 animate-pulse rounded" />
              </div>
            ))}
          </div>
        ) : mensajes.length === 0 ? (
          <div className="text-center py-10">
            <p className="hud-label text-slate-600">// SIN MENSAJES TODAVÍA</p>
            <p className="text-xs text-slate-700 mt-1">Escribe abajo para empezar la conversación.</p>
          </div>
        ) : (
          mensajes.map((m, i) => (
            <BurbjaMensaje
              key={m.id}
              mensaje={m}
              esMio={m.remitenteId === miId}
              mostrarFecha={i === 0 || diaDistinto(mensajes[i - 1].fechaEnvio, m.fechaEnvio)}
            />
          ))
        )}
      </div>

      {mostrarBotonBajar && (
        <button
          onClick={bajarAlFondo}
          className="absolute bottom-20 right-6 p-2 border border-cockpit-cyan/60 bg-cockpit-bg/90 text-cockpit-cyan hover:text-white"
          style={{ boxShadow: '0 0 12px rgba(34,211,238,0.4)' }}
          title="Bajar al final"
        >
          <ArrowDown size={14} />
        </button>
      )}

      {/* Input */}
      <div className="p-3 border-t border-cockpit-line flex gap-2">
        <input
          type="text"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder="Escribe un mensaje y pulsa Enter…"
          className="hud-input flex-1"
          maxLength={4000}
          disabled={enviando}
        />
        <button
          onClick={enviar}
          disabled={enviando || !texto.trim()}
          className="hud-btn !px-4 !py-2"
          title="Enviar (Enter)"
        >
          <Send size={14} />
          <span className="hidden sm:inline">{enviando ? 'Enviando…' : 'Enviar'}</span>
        </button>
      </div>
    </div>
  );
}

/* ---------- Burbuja de mensaje ---------- */
function BurbjaMensaje({ mensaje: m, esMio, mostrarFecha }) {
  const hora = m.fechaEnvio
    ? new Date(m.fechaEnvio).toLocaleTimeString('es-ES', {
        hour: '2-digit', minute: '2-digit'
      })
    : '';

  const fechaCompleta = m.fechaEnvio
    ? new Date(m.fechaEnvio).toLocaleDateString('es-ES', {
        day: '2-digit', month: 'long', year: 'numeric'
      })
    : '';

  return (
    <>
      {mostrarFecha && (
        <div className="text-center my-3">
          <span className="hud-label text-slate-600 px-3 py-1 border border-cockpit-line/50">
            // {fechaCompleta}
          </span>
        </div>
      )}
      <div className={esMio ? 'flex justify-end' : 'flex justify-start'}>
        <div
          className="max-w-[75%] px-3 py-2 relative"
          style={{
            background: esMio
              ? 'linear-gradient(135deg, rgba(168,85,247,0.18), rgba(168,85,247,0.08))'
              : 'rgba(255,255,255,0.04)',
            border: esMio
              ? '1px solid rgba(168,85,247,0.45)'
              : '1px solid rgba(120,135,160,0.18)',
            boxShadow: esMio ? '0 0 12px rgba(168,85,247,0.15)' : 'none',
            clipPath: 'polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px))',
          }}
        >
          <p className="text-sm text-white whitespace-pre-wrap break-words leading-relaxed">
            {m.contenido}
          </p>
          <div className="flex items-center justify-end gap-1 mt-1">
            <span className="hud-readout text-[10px] text-slate-500">{hora}</span>
            {esMio && (
              <span className={`text-[10px] ${m.leido ? 'text-cockpit-cyan' : 'text-slate-600'}`}>
                {m.leido ? '✓✓' : '✓'}
              </span>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

function diaDistinto(fechaA, fechaB) {
  if (!fechaA || !fechaB) return false;
  const a = new Date(fechaA).toDateString();
  const b = new Date(fechaB).toDateString();
  return a !== b;
}
