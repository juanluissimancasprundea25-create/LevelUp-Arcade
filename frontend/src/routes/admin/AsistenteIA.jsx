import { useState } from 'react';
import { motion } from 'framer-motion';
import { Sparkles, Tag, Copy, Check, Loader2, Wand2, Brain } from 'lucide-react';
import { api } from '../../lib/api.js';
import { useFetch } from '../../lib/hooks.js';
import { useToast } from '../../components/admin/Toast.jsx';

/**
 * Panel del Asistente IA.
 *
 * Dos herramientas independientes para probar el LLM sin estar dentro
 * de un producto: generar descripción comercial y sugerir categoría.
 *
 * Útil para previsualizar respuestas, ajustar nombres antes de crear
 * el producto, o simplemente experimentar con el modelo configurado.
 */
export default function AsistenteIA() {
  return (
    <div className="pb-8">
      <motion.header
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="mb-5 pt-2"
      >
        <p className="hud-label text-cockpit-cyan/70">// MÓDULO</p>
        <h1 className="font-display text-2xl xl:text-3xl tracking-wider text-white mt-1 flex items-center gap-3">
          <Brain className="text-cockpit-neon" size={28} />
          Asistente IA
        </h1>
        <p className="hud-readout mt-1">
          Pruebas directas contra el modelo OpenRouter configurado en el servidor
        </p>
      </motion.header>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.1 }}
        >
          <GeneradorDescripcion />
        </motion.div>
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.2 }}
        >
          <SugeridorCategoria />
        </motion.div>
      </div>
    </div>
  );
}

/* ---------- Herramienta 1: Generador de descripción ---------- */
function GeneradorDescripcion() {
  const [nombre, setNombre] = useState('');
  const [categoria, setCategoria] = useState('');
  const [resultado, setResultado] = useState(null);
  const [loading, setLoading] = useState(false);
  const [copiado, setCopiado] = useState(false);
  const toast = useToast();

  const categorias = useFetch('/categorias');

  async function generar() {
    if (!nombre.trim() || loading) return;
    setLoading(true);
    setResultado(null);
    try {
      const { data } = await api.post('/llm/descripcion-producto', {
        nombreProducto: nombre.trim(),
        categoria: categoria || null,
      });
      setResultado(data);
    } catch (e) {
      toast.err(e.response?.data?.mensaje || 'La IA no respondió');
    } finally {
      setLoading(false);
    }
  }

  async function copiar() {
    if (!resultado?.resultado) return;
    try {
      await navigator.clipboard.writeText(resultado.resultado);
      setCopiado(true);
      toast.ok('Copiado al portapapeles');
      setTimeout(() => setCopiado(false), 2000);
    } catch (_) {
      toast.err('No se pudo copiar');
    }
  }

  return (
    <div className="hud-panel p-5 h-full flex flex-col">
      <div className="flex items-start gap-3 mb-4">
        <div className="p-2 border border-cockpit-neon/40">
          <Sparkles size={18} className="text-cockpit-neon" />
        </div>
        <div className="flex-1">
          <p className="hud-label">// HERRAMIENTA</p>
          <h2 className="font-display text-lg text-white tracking-wide mt-0.5">
            Generador de descripción
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Dale un nombre de producto y opcionalmente una categoría. La IA escribirá una
            descripción comercial para tu tienda.
          </p>
        </div>
      </div>

      <div className="space-y-3 mb-4">
        <label className="block">
          <span className="hud-label">Nombre del producto</span>
          <input
            type="text"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            placeholder="Mando Pro Inalámbrico XB-7"
            className="hud-input mt-1.5"
            disabled={loading}
          />
        </label>

        <label className="block">
          <span className="hud-label">Categoría (opcional)</span>
          <select
            value={categoria}
            onChange={(e) => setCategoria(e.target.value)}
            className="hud-input mt-1.5"
            disabled={loading || categorias.loading}
          >
            <option value="">— sin contexto de categoría —</option>
            {(categorias.data || []).map((c) => (
              <option key={c.id} value={c.nombre}>{c.nombre}</option>
            ))}
          </select>
        </label>
      </div>

      <button
        onClick={generar}
        disabled={!nombre.trim() || loading}
        className="hud-btn !px-4 !py-2 w-full"
      >
        {loading
          ? <><Loader2 size={14} className="animate-spin" /> Generando…</>
          : <><Wand2 size={14} /> Generar descripción</>
        }
      </button>

      {/* Resultado */}
      {resultado && (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          className="mt-4 hud-panel !bg-black/40 p-4 flex-1"
          style={{
            borderColor: 'rgba(168,85,247,0.4)',
            boxShadow: '0 0 16px rgba(168,85,247,0.15)',
          }}
        >
          <div className="flex items-center justify-between mb-2">
            <p className="hud-label text-cockpit-neon">// RESPUESTA DE LA IA</p>
            <div className="flex items-center gap-2">
              <span className="hud-readout text-[10px] text-slate-500" title={resultado.modelo}>
                {(resultado.modelo || '').split('/').pop()?.slice(0, 24)}
              </span>
              <button
                onClick={copiar}
                className="p-1.5 text-cockpit-cyan hover:text-white"
                title="Copiar"
              >
                {copiado ? <Check size={14} /> : <Copy size={14} />}
              </button>
            </div>
          </div>
          <p className="text-sm text-slate-100 leading-relaxed whitespace-pre-wrap">
            {resultado.resultado}
          </p>
        </motion.div>
      )}
    </div>
  );
}

/* ---------- Herramienta 2: Sugeridor de categoría ---------- */
function SugeridorCategoria() {
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [resultado, setResultado] = useState(null);
  const [loading, setLoading] = useState(false);
  const toast = useToast();

  const categorias = useFetch('/categorias');

  async function sugerir() {
    if (!nombre.trim() || loading) return;
    setLoading(true);
    setResultado(null);
    try {
      const { data } = await api.post('/llm/sugerir-categoria', {
        nombreProducto: nombre.trim(),
        descripcion: descripcion.trim() || null,
      });
      // Buscar coincidencia con categoría existente
      const match = (categorias.data || []).find(
        c => c.nombre.toLowerCase() === (data.resultado || '').trim().toLowerCase()
      );
      setResultado({ ...data, match });
    } catch (e) {
      toast.err(e.response?.data?.mensaje || 'La IA no respondió');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="hud-panel p-5 h-full flex flex-col">
      <div className="flex items-start gap-3 mb-4">
        <div className="p-2 border border-cockpit-cyan/40">
          <Tag size={18} className="text-cockpit-cyan" />
        </div>
        <div className="flex-1">
          <p className="hud-label">// HERRAMIENTA</p>
          <h2 className="font-display text-lg text-white tracking-wide mt-0.5">
            Sugeridor de categoría
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            La IA recibe la lista real de categorías de tu tienda y elige la más apropiada
            para el producto que describas.
          </p>
        </div>
      </div>

      <div className="space-y-3 mb-4 flex-1">
        <label className="block">
          <span className="hud-label">Nombre del producto</span>
          <input
            type="text"
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            placeholder="Funko Pop Pikachu Edición 25 Aniversario"
            className="hud-input mt-1.5"
            disabled={loading}
          />
        </label>

        <label className="block">
          <span className="hud-label">Descripción (opcional, ayuda a precisar)</span>
          <textarea
            value={descripcion}
            onChange={(e) => setDescripcion(e.target.value)}
            placeholder="Figura coleccionable de vinilo, 10 cm, caja original numerada…"
            rows={3}
            className="hud-input mt-1.5 resize-none font-sans"
            disabled={loading}
            maxLength={2000}
          />
        </label>
      </div>

      <button
        onClick={sugerir}
        disabled={!nombre.trim() || loading}
        className="hud-btn hud-btn--cyan !px-4 !py-2 w-full"
      >
        {loading
          ? <><Loader2 size={14} className="animate-spin" /> Analizando…</>
          : <><Tag size={14} /> Sugerir categoría</>
        }
      </button>

      {resultado && (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          className="mt-4 hud-panel !bg-black/40 p-4"
          style={{
            borderColor: resultado.match ? 'rgba(52,211,153,0.5)' : 'rgba(251,191,36,0.5)',
            boxShadow: resultado.match
              ? '0 0 16px rgba(52,211,153,0.15)'
              : '0 0 16px rgba(251,191,36,0.15)',
          }}
        >
          <div className="flex items-center justify-between mb-2">
            <p className="hud-label" style={{ color: resultado.match ? '#34d399' : '#fbbf24' }}>
              // {resultado.match ? 'CATEGORÍA SUGERIDA · EXISTE' : 'CATEGORÍA SUGERIDA · NUEVA'}
            </p>
            <span className="hud-readout text-[10px] text-slate-500">
              {(resultado.modelo || '').split('/').pop()?.slice(0, 24)}
            </span>
          </div>
          <p className="font-display text-2xl text-white mt-2 tracking-wide">
            {resultado.resultado}
          </p>
          {!resultado.match && (
            <p className="text-xs text-cockpit-amber mt-2">
              ℹ Esta categoría no existe todavía en tu sistema. Considera crearla antes de usarla.
            </p>
          )}
        </motion.div>
      )}
    </div>
  );
}
