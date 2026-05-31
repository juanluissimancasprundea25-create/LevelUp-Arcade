import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Plus, Search, RefreshCw, Edit, Trash2, X, Users,
  Mail, Phone, MapPin,
} from 'lucide-react';
import { api } from '../../lib/api.js';
import { useFetch } from '../../lib/hooks.js';
import { useAuth } from '../../lib/auth.jsx';
import { useToast } from '../../components/admin/Toast.jsx';
import { ClienteFormModal } from '../../components/admin/ClienteFormModal.jsx';
import { ConfirmModal } from '../../components/admin/ConfirmModal.jsx';
import { PasswordTemporalModal } from '../../components/admin/PasswordTemporalModal.jsx';

export default function Clientes() {
  const { user } = useAuth();
  const esAdmin = user?.rol === 'ADMIN';
  const toast = useToast();

  const [busqueda, setBusqueda] = useState('');
  const [soloActivos, setSoloActivos] = useState(false);

  const [editando, setEditando] = useState(null);    // null | {} | {cliente}
  const [borrar, setBorrar] = useState(null);
  const [borrando, setBorrando] = useState(false);
  // Resultado del POST: muestra la password temporal una vez
  const [creadoConPassword, setCreadoConPassword] = useState(null);

  const clientes = useFetch('/clientes');

  const filtrados = useMemo(() => {
    if (!clientes.data) return [];
    let arr = clientes.data;
    if (soloActivos) arr = arr.filter(c => c.activo);
    const q = busqueda.trim().toLowerCase();
    if (q) {
      arr = arr.filter(c =>
        (c.email || '').toLowerCase().includes(q) ||
        (c.nombreCompleto || '').toLowerCase().includes(q) ||
        (c.nif || '').toLowerCase().includes(q) ||
        (c.ciudad || '').toLowerCase().includes(q) ||
        (c.telefono || '').includes(q)
      );
    }
    return arr;
  }, [clientes.data, busqueda, soloActivos]);

  async function confirmarBorrado() {
    if (!borrar || borrando) return;
    setBorrando(true);
    try {
      await api.delete(`/clientes/${borrar.id}`);
      toast.ok(`Cliente "${borrar.nombreCompleto || borrar.email}" eliminado`);
      setBorrar(null);
      clientes.refresh();
    } catch (e) {
      const msg = e.response?.data?.mensaje || 'No se pudo eliminar';
      // Si tiene pedidos, el backend devuelve error de FK — recomendamos desactivar
      toast.err(`${msg}. Si tiene pedidos asociados, márcalo como Inactivo.`);
    } finally {
      setBorrando(false);
    }
  }

  return (
    <div className="pb-8">
      <motion.header
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="flex items-end justify-between mb-5 pt-2 gap-4 flex-wrap"
      >
        <div>
          <p className="hud-label text-cockpit-cyan/70">// MÓDULO</p>
          <h1 className="font-display text-2xl xl:text-3xl tracking-wider text-white mt-1">
            Clientes
          </h1>
          <p className="hud-readout mt-1">
            {clientes.loading
              ? 'Sincronizando…'
              : `${filtrados.length} de ${clientes.data?.length || 0} clientes`}
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => clientes.refresh()}
            className="hud-btn hud-btn--cyan !px-3 !py-2"
            disabled={clientes.loading}
          >
            <RefreshCw size={14} className={clientes.loading ? 'animate-spin' : ''} />
            <span className="hidden sm:inline">Refrescar</span>
          </button>
          {esAdmin && (
            <button onClick={() => setEditando({})} className="hud-btn !px-4 !py-2">
              <Plus size={14} /> Nuevo cliente
            </button>
          )}
        </div>
      </motion.header>

      {/* Filtros */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.1 }}
        className="hud-panel p-4 mb-5"
      >
        <div className="relative">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none" />
          <input
            type="text"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar nombre, email, NIF, ciudad o teléfono…"
            className="hud-input !pl-9"
          />
          {busqueda && (
            <button
              onClick={() => setBusqueda('')}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white"
              aria-label="Limpiar"
            >
              <X size={14} />
            </button>
          )}
        </div>
        <label className="mt-3 flex items-center gap-2 cursor-pointer select-none w-fit">
          <input
            type="checkbox"
            checked={soloActivos}
            onChange={(e) => setSoloActivos(e.target.checked)}
            className="accent-cockpit-ok w-4 h-4"
          />
          <span className="hud-label">// SOLO ACTIVOS</span>
        </label>
      </motion.div>

      {/* Tabla */}
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, delay: 0.15 }}
        className="hud-panel overflow-hidden"
      >
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead>
              <tr className="border-b border-cockpit-line">
                <th className="hud-label text-left p-3">Cliente</th>
                <th className="hud-label text-left p-3 hidden md:table-cell">Contacto</th>
                <th className="hud-label text-left p-3 hidden lg:table-cell">Ubicación</th>
                <th className="hud-label text-left p-3 hidden md:table-cell">NIF</th>
                <th className="hud-label text-center p-3">Estado</th>
                <th className="hud-label text-right p-3">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {clientes.loading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-cockpit-line/40">
                    <td className="p-3" colSpan={6}>
                      <div className="h-10 bg-white/5 animate-pulse rounded" />
                    </td>
                  </tr>
                ))
              ) : filtrados.length === 0 ? (
                <tr>
                  <td colSpan={6} className="p-10 text-center">
                    <Users size={36} className="mx-auto mb-3 opacity-30 text-slate-500" />
                    <p className="hud-label text-slate-500">
                      {clientes.data?.length === 0
                        ? '// NO HAY CLIENTES · DA DE ALTA EL PRIMERO'
                        : '// NINGÚN RESULTADO'}
                    </p>
                  </td>
                </tr>
              ) : (
                filtrados.map((c) => (
                  <ClienteRow
                    key={c.id}
                    cliente={c}
                    esAdmin={esAdmin}
                    onEditar={() => setEditando({ cliente: c })}
                    onBorrar={() => setBorrar(c)}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      </motion.div>

      {/* Modales */}
      <ClienteFormModal
        open={!!editando}
        cliente={editando?.cliente}
        onClose={() => setEditando(null)}
        onSaved={() => clientes.refresh()}
        onCreated={(payload) => {
          // payload = { cliente, passwordTemporal }
          clientes.refresh();
          setCreadoConPassword(payload);
        }}
      />
      <ConfirmModal
        open={!!borrar}
        onClose={() => setBorrar(null)}
        onConfirm={confirmarBorrado}
        title={`¿Eliminar a "${borrar?.nombreCompleto || borrar?.email}"?`}
        message="Se eliminará el cliente y su usuario asociado. Si tiene pedidos en el historial, márcalo como Inactivo en su lugar."
        busy={borrando}
      />
      <PasswordTemporalModal
        open={!!creadoConPassword}
        cliente={creadoConPassword?.cliente}
        password={creadoConPassword?.passwordTemporal}
        onClose={() => setCreadoConPassword(null)}
      />
    </div>
  );
}

/* ---------- Fila ---------- */
function ClienteRow({ cliente: c, esAdmin, onEditar, onBorrar }) {
  // Avatar generado con inicial
  const inicial = (c.nombre?.[0] || c.email?.[0] || '?').toUpperCase();

  return (
    <tr className="border-b border-cockpit-line/40 hover:bg-white/[0.025] transition-colors">
      {/* Cliente */}
      <td className="p-3">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 grid place-items-center font-display text-cockpit-neon border border-cockpit-neon/40 bg-cockpit-neon/5">
            {inicial}
          </div>
          <div className="min-w-0">
            <div className="text-sm text-white truncate">
              {c.nombreCompleto || c.nombre || '—'}
            </div>
            <div className="text-xs text-cockpit-cyan/80 hud-readout truncate">{c.email}</div>
          </div>
        </div>
      </td>

      {/* Contacto */}
      <td className="p-3 hidden md:table-cell">
        <div className="space-y-0.5">
          {c.email && (
            <div className="flex items-center gap-1.5 text-xs text-slate-400">
              <Mail size={11} className="opacity-60" /> <span className="truncate">{c.email}</span>
            </div>
          )}
          {c.telefono && (
            <div className="flex items-center gap-1.5 text-xs text-slate-400">
              <Phone size={11} className="opacity-60" /> {c.telefono}
            </div>
          )}
        </div>
      </td>

      {/* Ubicación */}
      <td className="p-3 hidden lg:table-cell">
        {(c.ciudad || c.codigoPostal) ? (
          <div className="flex items-start gap-1.5 text-xs text-slate-400">
            <MapPin size={11} className="opacity-60 mt-0.5 shrink-0" />
            <div>
              {c.ciudad && <div>{c.ciudad}</div>}
              {c.codigoPostal && <div className="hud-readout opacity-70">{c.codigoPostal}</div>}
            </div>
          </div>
        ) : (
          <span className="text-slate-600 text-xs">—</span>
        )}
      </td>

      {/* NIF */}
      <td className="p-3 hidden md:table-cell">
        {c.nif ? (
          <span className="hud-readout text-slate-300">{c.nif}</span>
        ) : (
          <span className="text-slate-600 text-xs">—</span>
        )}
      </td>

      {/* Estado */}
      <td className="p-3 text-center">
        <span
          className={[
            'inline-block px-2 py-0.5 text-[10px] tracking-widest uppercase',
            c.activo
              ? 'border border-cockpit-ok/40 text-cockpit-ok'
              : 'border border-slate-600 text-slate-500',
          ].join(' ')}
        >
          {c.activo ? 'Activo' : 'Inactivo'}
        </span>
      </td>

      {/* Acciones */}
      <td className="p-3 text-right whitespace-nowrap">
        <button
          onClick={onEditar}
          className="p-1.5 mr-1 text-cockpit-neon hover:text-white hover:bg-cockpit-neon/10 transition-colors"
          title={esAdmin ? 'Editar' : 'Ver detalle'}
        >
          <Edit size={15} />
        </button>
        {esAdmin && (
          <button
            onClick={onBorrar}
            className="p-1.5 text-slate-500 hover:text-cockpit-danger hover:bg-cockpit-danger/10 transition-colors"
            title="Eliminar"
          >
            <Trash2 size={15} />
          </button>
        )}
      </td>
    </tr>
  );
}
