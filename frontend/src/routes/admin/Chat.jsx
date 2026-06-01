import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { RefreshCw } from 'lucide-react';
import { api } from '../../lib/api.js';
import { useAuth } from '../../lib/auth.jsx';
import { ConversacionesLista } from '../../components/admin/chat/ConversacionesLista.jsx';
import { ChatPanel } from '../../components/admin/chat/ChatPanel.jsx';

/**
 * Módulo Chat. Layout de dos paneles:
 *  [ lista de conversaciones ]  [ panel del chat seleccionado ]
 *
 * Polling de la lista de conversaciones cada 6 segundos para
 * actualizar contadores de no-leídos.
 */
export default function Chat() {
  const { user } = useAuth();
  const [conversaciones, setConversaciones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [seleccionada, setSeleccionada] = useState(null); // { id, datos }

  async function cargar() {
    try {
      const { data } = await api.get('/chat/conversaciones');
      setConversaciones(data || []);
    } catch (_) {
      // silencio en polling
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    cargar();
    const iv = setInterval(cargar, 6000);
    return () => clearInterval(iv);
  }, []);

  function seleccionar(id, datos) {
    setSeleccionada({ id, datos });
  }

  return (
    <div className="pb-8 flex flex-col h-[calc(100vh-6rem)]">
      <motion.header
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="flex items-end justify-between mb-4 pt-2 gap-4 flex-wrap shrink-0"
      >
        <div>
          <p className="hud-label text-cockpit-cyan/70">// MÓDULO</p>
          <h1 className="font-display text-2xl xl:text-3xl tracking-wider text-white mt-1">
            Chat
          </h1>
          <p className="hud-readout mt-1">
            {conversaciones.length} {conversaciones.length === 1 ? 'conversación' : 'conversaciones'}
            {' · '}
            {conversaciones.reduce((s, c) => s + (c.noLeidos || 0), 0)} sin leer
          </p>
        </div>
        <button
          onClick={cargar}
          className="hud-btn hud-btn--cyan !px-3 !py-2"
          disabled={loading}
        >
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          <span className="hidden sm:inline">Refrescar</span>
        </button>
      </motion.header>

      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.1 }}
        className="flex-1 grid grid-cols-1 md:grid-cols-[320px_1fr] gap-4 min-h-0"
      >
        <ConversacionesLista
          conversaciones={conversaciones}
          seleccionadaId={seleccionada?.id}
          onSelect={seleccionar}
          loading={loading}
        />
        <ChatPanel
          clienteUsuarioId={seleccionada?.id}
          conversacion={seleccionada?.datos}
          miId={user?.id}
          onEnviado={cargar}
        />
      </motion.div>
    </div>
  );
}
