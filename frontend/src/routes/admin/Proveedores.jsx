import { useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  Plus, Search, RefreshCw, Edit, Trash2, X, Truck,
  Mail, Phone, MapPin, Package,
} from 'lucide-react';
import { api } from '../../lib/api.js';
import { useFetch } from '../../lib/hooks.js';
import { useAuth } from '../../lib/auth.jsx';
import { useToast } from '../../components/admin/Toast.jsx';
import { ProveedorFormModal } from '../../components/admin/ProveedorFormModal.jsx';
import { ConfirmModal } from '../../components/admin/ConfirmModal.jsx';

export default function Proveedores() {
  const { user } = useAuth();
  const esAdmin = user?.rol === 'ADMIN';
  const toast = useToast();

  const [busqueda, setBusqueda] = useState('');
  const [soloActivos, setSoloActivos] = useState(false);

  const [editando, setEditando] = useState(null);
  const [borrar, setBorrar] = useState(null);
  const [borrando, setBorrando] = useState(false);

  const proveedores = useFetch('/proveedores');
  const productos   = useFetch('/productos');

  // Conteo de productos por proveedor
  const conteoProductos = useMemo(() => {
    if (!productos.data) return {};
    return productos.data.reduce((acc, p) => {
      if (p.proveedorId != null) acc[p.proveedorId] = (acc[p.proveedorId] || 0) + 1;
      return acc;
    }, {});
  }, [productos.data]);

  const filtrados = useMemo(() => {
    if (!proveedores.data) return [];
    let arr = proveedores.data;
    if (soloActivos) arr = arr.filter(p => p.activo);
    const q = busqueda.trim().toLowerCase();
    if (q) {
      arr = arr.filter(p =>
        (p.nombreEmpresa || '').toLowerCase().includes(q) ||
        (p.cif || '').toLowerCase().includes(q) ||
        (p.emailContacto || '').toLowerCase().includes(q) ||
        (p.ciudad || '').toLowerCase().includes(q) ||
        (p.telefono || '').includes(q)
      );
    }
    return arr;
  }, [proveedores.data, busqueda, soloActivos]);

  async function confirmarBorrado() {
    if (!borrar || borrando) return;
    const enUso = conteoProductos[borrar.id] || 0;
    if (enUso > 0) {
      toast.warn(`No se puede eliminar: ${enUso} productos lo usan. Márcalo como Inactivo.`);
      setBorrar(null);
      return;
    }
    setBorrando(true);
    try {
      await api.delete(`/proveedores/${borrar.id}`);
      toast.ok(`"${borrar.nombreEmpresa}" eliminado`);
      setBorrar(null);
      proveedores.refresh();
    } catch (e) {
      toast.err(e.response?.data?.mensaje || 'No se pudo eliminar');
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
            Proveedores
          </h1>
          <p className="hud-readout mt-1">
            {proveedores.loading
              ? 'Sincronizando…'
              : `${filtrados.length} de ${proveedores.data?.length || 0} proveedores`}
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => { proveedores.refresh(); productos.refresh(); }}
            className="hud-btn hud-btn--cyan !px-3 !py-2"
            disabled={proveedores.loading}
          >
            <RefreshCw size={14} className={proveedores.loading ? 'animate-spin' : ''} />
            <span className="hidden sm:inline">Refrescar</span>
          </button>
          {esAdmin && (
            <button onClick={() => setEditando({})} className="hud-btn !px-4 !py-2">
              <Plus size={14} /> Nuevo proveedor
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
            placeholder="Buscar empresa, CIF, email, ciudad o teléfono…"
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
                <th className="hud-label text-left p-3">Empresa</th>
                <th className="hud-label text-left p-3 hidden md:table-cell">Contacto</th>
                <th className="hud-label text-left p-3 hidden lg:table-cell">Ubicación</th>
                <th className="hud-label text-center p-3">Productos</th>
                <th className="hud-label text-center p-3">Estado</th>
                <th className="hud-label text-right p-3">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {proveedores.loading ? (
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
                    <Truck size={36} className="mx-auto mb-3 opacity-30 text-slate-500" />
                    <p className="hud-label text-slate-500">
                      {proveedores.data?.length === 0
                        ? '// NO HAY PROVEEDORES · DA DE ALTA EL PRIMERO'
                        : '// NINGÚN RESULTADO'}
                    </p>
                  </td>
                </tr>
              ) : (
                filtrados.map((p) => (
                  <ProveedorRow
                    key={p.id}
                    proveedor={p}
                    numProductos={conteoProductos[p.id] || 0}
                    esAdmin={esAdmin}
                    onEditar={() => setEditando({ proveedor: p })}
                    onBorrar={() => setBorrar(p)}
                  />
                ))
              )}
            </tbody>
          </table>
        </div>
      </motion.div>

      <ProveedorFormModal
        open={!!editando}
        proveedor={editando?.proveedor}
        onClose={() => setEditando(null)}
        onSaved={() => proveedores.refresh()}
      />
      <ConfirmModal
        open={!!borrar}
        onClose={() => setBorrar(null)}
        onConfirm={confirmarBorrado}
        title={`¿Eliminar "${borrar?.nombreEmpresa}"?`}
        message={
          (conteoProductos[borrar?.id] || 0) > 0
            ? `Tiene ${conteoProductos[borrar?.id]} productos asociados. No se podrá eliminar — márcalo como Inactivo.`
            : 'Esta acción es permanente.'
        }
        busy={borrando}
      />
    </div>
  );
}

/* ---------- Fila ---------- */
function ProveedorRow({ proveedor: p, numProductos, esAdmin, onEditar, onBorrar }) {
  return (
    <tr className="border-b border-cockpit-line/40 hover:bg-white/[0.025] transition-colors">
      <td className="p-3">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 grid place-items-center border border-cockpit-cyan/40 bg-cockpit-cyan/5">
            <Truck size={14} className="text-cockpit-cyan" />
          </div>
          <div className="min-w-0">
            <div className="text-sm text-white truncate">{p.nombreEmpresa}</div>
            <div className="hud-readout text-slate-400 truncate">{p.cif}</div>
          </div>
        </div>
      </td>

      <td className="p-3 hidden md:table-cell">
        <div className="space-y-0.5">
          {p.emailContacto && (
            <div className="flex items-center gap-1.5 text-xs text-slate-400">
              <Mail size={11} className="opacity-60" /> <span className="truncate">{p.emailContacto}</span>
            </div>
          )}
          {p.telefono && (
            <div className="flex items-center gap-1.5 text-xs text-slate-400">
              <Phone size={11} className="opacity-60" /> {p.telefono}
            </div>
          )}
          {!p.emailContacto && !p.telefono && (
            <span className="text-slate-600 text-xs">—</span>
          )}
        </div>
      </td>

      <td className="p-3 hidden lg:table-cell">
        {(p.ciudad || p.codigoPostal) ? (
          <div className="flex items-start gap-1.5 text-xs text-slate-400">
            <MapPin size={11} className="opacity-60 mt-0.5 shrink-0" />
            <div>
              {p.ciudad && <div>{p.ciudad}</div>}
              {p.codigoPostal && <div className="hud-readout opacity-70">{p.codigoPostal}</div>}
            </div>
          </div>
        ) : (
          <span className="text-slate-600 text-xs">—</span>
        )}
      </td>

      <td className="p-3 text-center">
        <div className="inline-flex items-center gap-1.5 text-cockpit-cyan">
          <Package size={12} />
          <span className="font-mono text-sm">{numProductos}</span>
        </div>
      </td>

      <td className="p-3 text-center">
        <span
          className={[
            'inline-block px-2 py-0.5 text-[10px] tracking-widest uppercase',
            p.activo
              ? 'border border-cockpit-ok/40 text-cockpit-ok'
              : 'border border-slate-600 text-slate-500',
          ].join(' ')}
        >
          {p.activo ? 'Activo' : 'Inactivo'}
        </span>
      </td>

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
