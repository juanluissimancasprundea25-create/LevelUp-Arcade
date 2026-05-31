import { useState } from 'react';
import { HudModal } from './HudModal.jsx';
import { Minus, Plus } from 'lucide-react';
import { api } from '../../lib/api.js';
import { useToast } from './Toast.jsx';

/**
 * Ajuste rápido de stock. Llama a POST /api/productos/{id}/stock
 * con { cantidad, motivo }. cantidad positiva = entrada, negativa = salida.
 */
export function StockAdjustModal({ open, onClose, producto, onSaved }) {
  const [delta, setDelta] = useState(1);
  const [motivo, setMotivo] = useState('');
  const [busy, setBusy] = useState(false);
  const toast = useToast();

  if (!producto) return null;

  const nuevoStock = (producto.stock || 0) + delta;
  const peligro = nuevoStock < 0;

  async function handleConfirm() {
    if (busy || peligro || delta === 0) return;
    setBusy(true);
    try {
      const { data } = await api.post(`/productos/${producto.id}/stock`, {
        cantidad: delta,
        motivo: motivo.trim() || (delta > 0 ? 'Ajuste manual (entrada)' : 'Ajuste manual (salida)'),
      });
      toast.ok(`Stock actualizado: ${data.stock} uds`);
      onSaved && onSaved(data);
      onClose();
      setDelta(1);
      setMotivo('');
    } catch (e) {
      toast.err(e.response?.data?.mensaje || 'No se pudo ajustar el stock');
    } finally {
      setBusy(false);
    }
  }

  return (
    <HudModal
      open={open}
      onClose={onClose}
      title={`Ajustar stock · ${producto.nombre}`}
      subtitle="STOCK"
      size="sm"
    >
      <div className="text-center mb-4">
        <p className="hud-label">// STOCK ACTUAL</p>
        <div className="mt-1 font-display text-3xl text-white">
          {producto.stock}
          <span className="text-slate-500 text-base"> uds</span>
        </div>
        {producto.stockMinimo != null && (
          <p className="hud-readout mt-1 opacity-70">mínimo: {producto.stockMinimo}</p>
        )}
      </div>

      {/* Selector +/- grande */}
      <div className="flex items-center justify-center gap-4 my-6">
        <button
          onClick={() => setDelta(delta - 1)}
          className="hud-btn !px-3 !py-3"
          type="button"
        >
          <Minus size={20} />
        </button>
        <input
          type="number"
          value={delta}
          onChange={(e) => setDelta(parseInt(e.target.value) || 0)}
          className="hud-input !w-32 text-center text-2xl font-display"
        />
        <button
          onClick={() => setDelta(delta + 1)}
          className="hud-btn hud-btn--cyan !px-3 !py-3"
          type="button"
        >
          <Plus size={20} />
        </button>
      </div>

      <div className={`text-center my-4 ${peligro ? 'text-cockpit-danger' : 'text-cockpit-ok'}`}>
        <p className="hud-label opacity-70">// NUEVO STOCK</p>
        <div className="mt-1 font-display text-2xl">
          {nuevoStock} uds
        </div>
        {peligro && (
          <p className="text-xs mt-1">⚠ no puede quedar negativo</p>
        )}
      </div>

      <label className="block mb-4">
        <span className="hud-label">Motivo (opcional)</span>
        <input
          type="text"
          value={motivo}
          onChange={(e) => setMotivo(e.target.value)}
          placeholder="Entrada de pedido, rotura, etc."
          className="hud-input mt-1.5"
          maxLength={500}
        />
      </label>

      <div className="flex gap-3 justify-end mt-6">
        <button onClick={onClose} className="hud-btn hud-btn--cyan !px-4 !py-2" disabled={busy}>
          Cancelar
        </button>
        <button
          onClick={handleConfirm}
          className="hud-btn !px-4 !py-2"
          disabled={busy || peligro || delta === 0}
        >
          {busy ? 'Aplicando…' : 'Aplicar ajuste'}
        </button>
      </div>
    </HudModal>
  );
}
