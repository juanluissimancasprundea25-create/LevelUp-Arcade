import { useEffect, useRef, useState } from 'react';
import { motion } from 'framer-motion';
import { Send, MessageSquare, ArrowDown, Headphones, Loader2 } from 'lucide-react';
import { api } from '../../lib/api.js';
import { useAuth } from '../../lib/auth.jsx';
import { useToast } from '../../components/admin/Toast.jsx';

/**
 * Chat del cliente con el equipo de soporte.
 *
 * El cliente solo tiene UN hilo de conversación: él contra "el buzón
 * de admin". No hay lista de conversaciones — directamente el hilo.
 *
 *  - Polling cada 4 segundos para recibir nuevos mensajes
 *  - Burbujas violetas (yo) vs grises (admin)
 *  - Input con Enter para enviar
 *  - Auto-scroll al fondo si ya estabas abajo
 *
 * Endpoints reutilizados:
 *   GET  /chat/conversaciones/{miUsuarioId}/mensajes
 *   POST /chat/mensajes (destinatarioUsuarioId=null → buzón admin)
 */
export default function MisMensajes() {
  const { user } = useAuth();
  const [mensajes, setMensajes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [texto, setTexto] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [mostrarBotonBajar, setMostrarBotonBajar] = useState(false);
  const scrollRef = useRef(null);
  const toast = useToast();

  // Polling de mensajes
  useEffect(() => {
    if (!user?.id) return;
    let cancelado = false;

    async function cargar(esRefresco = false) {
      if (!esRefresco) setLoading(true);
      try {
        const { data } = await api.get(
          `/chat/conversaciones/${user.id}/mensajes`,
          { params: { size: 500, sort: 'fechaEnvio,asc' } }
        );
        if (cancelado) return;
        setMensajes(data?.content || data || []);
      } catch (_) {
        // silencio en polling
      } finally {
        if (!cancelado) setLoading(false);
      }
    }

    cargar();
    const iv = setInterval(() => cargar(true), 4000);
    return () => { cancelado = true; clearInterval(iv); };
  }, [user?.id]);

  // Auto-scroll al fondo cuando llegan mensajes nuevos (si estás abajo)
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
    if (!contenido || enviando) return;
    setEnviando(true);
    try {
      // destinatarioUsuarioId=null → al buzón de admin
      const { data } = await api.post('/chat/mensajes', {
        destinatarioUsuarioId: null,
        contenido,
      });
      setTexto('');
      setMensajes((arr) => [...arr, data]);
      setTimeout(() => {
        if (scrollRef.current) {
          scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
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

  return (
    <div className="space-y-3 flex flex-col h-[calc(100vh-13rem)]">
      <header className="shrink-0">
        <p className="hud-label text-cockpit-cyan/70">// SOPORTE</p>
        <h2 className="font-display text-xl tracking-wide text-white mt-0.5 flex items-center gap-2">
          <MessageSquare className="text-cockpit-neon" size={20} />
          Mensajes con el equipo
        </h2>
      </header>

      <motion.div
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="hud-panel flex-1 flex flex-col overflow-hidden"
      >
        {/* Cabecera del hilo */}
        <div className="p-3 border-b border-cockpit-line flex items-center gap-3">
          <div className="w-10 h-10 grid place-items-center border border-cockpit-neon/40 bg-cockpit-neon/5">
            <Headphones size={18} className="text-cockpit-neon" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="hud-label text-cockpit-cyan/70">// EQUIPO LEVELUP</p>
            <p className="text-sm text-white font-display tracking-wide">
              Soporte y atención al cliente
            </p>
          </div>
          <span className="hud-readout text-[10px] text-slate-500 shrink-0">
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
            <div className="text-center py-12">
              <MessageSquare size={48} className="mx-auto mb-3 opacity-20 text-slate-500" />
              <p className="hud-label text-slate-500 mb-2">// HISTORIAL VACÍO</p>
              <p className="text-xs text-slate-600 max-w-sm mx-auto">
                ¿Tienes alguna duda sobre un pedido o producto? Escríbenos abajo
                y el equipo te responderá lo antes posible.
              </p>
            </div>
          ) : (
            mensajes.map((m, i) => (
              <BurbujaMensaje
                key={m.id}
                mensaje={m}
                esMio={m.remitenteId === user?.id}
                mostrarFecha={i === 0 || diaDistinto(mensajes[i - 1].fechaEnvio, m.fechaEnvio)}
              />
            ))
          )}
        </div>

        {mostrarBotonBajar && (
          <button
            onClick={bajarAlFondo}
            className="absolute bottom-20 right-6 p-2 border border-cockpit-cyan/60 bg-cockpit-bg/90 text-cockpit-cyan hover:text-white z-10"
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
            placeholder="Escribe tu mensaje y pulsa Enter…"
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
            {enviando
              ? <Loader2 size={14} className="animate-spin" />
              : <Send size={14} />}
            <span className="hidden sm:inline">Enviar</span>
          </button>
        </div>
      </motion.div>
    </div>
  );
}

/* ---------- Burbuja ---------- */
function BurbujaMensaje({ mensaje: m, esMio, mostrarFecha }) {
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
          className="max-w-[75%] px-3 py-2"
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
