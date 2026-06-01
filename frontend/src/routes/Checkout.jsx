import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowLeft, MapPin, CreditCard, Banknote, Wallet, Check, Loader2,
  ShieldCheck, AlertCircle, Package,
} from 'lucide-react';
import { api } from '../lib/api.js';
import { useAuth } from '../lib/auth.jsx';
import { useCarrito } from '../lib/carrito.js';
import { useToast } from '../components/admin/Toast.jsx';
import { HeaderTienda } from '../components/tienda/HeaderTienda.jsx';

/**
 * Checkout — URL: /cockpit/checkout
 *
 *  Usa /api/clientes/me (Fase 7d) para precargar la dirección del perfil.
 */
const METODOS_PAGO = [
  { value: 'TARJETA',       label: 'Tarjeta',       icon: CreditCard, hint: 'Crédito o débito' },
  { value: 'PAYPAL',        label: 'PayPal',        icon: Wallet,     hint: 'Pago seguro online' },
  { value: 'TRANSFERENCIA', label: 'Transferencia', icon: Banknote,   hint: 'Pago bancario' },
];

export default function Checkout() {
  const { user } = useAuth();
  const { items, totalEuros, totalUnidades, vaciar } = useCarrito();
  const toast = useToast();
  const navigate = useNavigate();

  const [direccion, setDireccion] = useState('');
  const [metodoPago, setMetodoPago] = useState('TARJETA');
  const [pagarAhora, setPagarAhora] = useState(true);
  const [busy, setBusy] = useState(false);

  const rolNormalizado = String(user?.rol || '').replace(/^ROLE_/, '').toUpperCase();
  const esCliente = rolNormalizado === 'CLIENTE';

  useEffect(() => {
    if (items.length === 0 && !busy) {
      navigate('/cockpit/carrito', { replace: true });
      return;
    }
    if (!user) {
      navigate('/cockpit/login?vuelve=/cockpit/checkout', { replace: true });
      return;
    }
    if (!esCliente) {
      toast.warn('Solo los clientes pueden tramitar pedidos');
      navigate('/cockpit/tienda', { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [items.length, user]);

  // Precargar dirección del perfil (endpoint Fase 7d /clientes/me)
  useEffect(() => {
    let cancelado = false;
    async function cargarPerfil() {
      try {
        const { data } = await api.get('/mi-perfil');
        if (cancelado || !data?.direccion) return;
        let dir = data.direccion;
        if (data.ciudad) dir += ', ' + data.ciudad;
        if (data.codigoPostal) dir += ' ' + data.codigoPostal;
        if (data.pais) dir += ', ' + data.pais;
        setDireccion(dir);
      } catch (_) {
        // sin perfil cargado, queda en blanco
      }
    }
    if (user && esCliente) cargarPerfil();
    return () => { cancelado = true; };
  }, [user, esCliente]);

  async function confirmar() {
    if (busy) return;
    if (!direccion.trim()) {
      toast.warn('La dirección de envío es obligatoria');
      return;
    }

    setBusy(true);
    try {
      const payloadPedido = {
        lineas: items.map(it => ({
          productoId: it.productoId,
          cantidad:   it.cantidad,
        })),
        direccionEnvio: direccion.trim().slice(0, 255),
      };
      const { data: pedido } = await api.post('/pedidos', payloadPedido);

      if (pagarAhora) {
        try {
          await api.post(`/pedidos/${pedido.id}/pagar`, { metodoPago });
          toast.ok(`Pedido #${pedido.id} creado y pagado con ${metodoPago}`);
        } catch (eP) {
          toast.warn(`Pedido #${pedido.id} creado, pero el pago falló: lo verás como Pendiente en tu cuenta`);
        }
      } else {
        toast.ok(`Pedido #${pedido.id} creado. Puedes pagarlo desde "Mis pedidos"`);
      }

      vaciar();
      navigate(`/cockpit/mi-cuenta/pedidos/${pedido.id}`);
    } catch (e) {
      const msg = e.response?.data?.mensaje || e.response?.data?.message || 'No se pudo completar el pedido';
      toast.err(msg);
    } finally {
      setBusy(false);
    }
  }

  if (items.length === 0) return null;

  return (
    <div className="min-h-screen flex flex-col">
      <HeaderTienda />

      <main className="flex-1 max-w-6xl mx-auto w-full px-4 sm:px-6 py-6">
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="mb-4"
        >
          <Link
            to="/cockpit/carrito"
            className="inline-flex items-center gap-1.5 text-xs tracking-widest uppercase text-cockpit-cyan hover:text-white transition-colors"
          >
            <ArrowLeft size={12} />
            Volver al carrito
          </Link>
        </motion.div>

        <motion.header
          initial={{ opacity: 0, y: -6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
          className="mb-5"
        >
          <p className="hud-label text-cockpit-cyan/70">// CHECKOUT</p>
          <h1 className="font-display text-3xl tracking-wider text-white mt-1 flex items-center gap-3">
            <ShieldCheck className="text-cockpit-neon" size={26} />
            Finalizar compra
          </h1>
          <p className="hud-readout mt-1">
            Paso final · {totalUnidades} {totalUnidades === 1 ? 'artículo' : 'artículos'}
          </p>
        </motion.header>

        <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-5">
          {/* Izquierda */}
          <motion.div
            initial={{ opacity: 0, x: -8 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.4, delay: 0.05 }}
            className="space-y-5"
          >
            {/* Dirección */}
            <div className="hud-panel p-5">
              <div className="flex items-start gap-3 mb-3">
                <div className="p-2 border border-cockpit-cyan/40">
                  <MapPin size={16} className="text-cockpit-cyan" />
                </div>
                <div>
                  <p className="hud-label">// PASO 1</p>
                  <h2 className="font-display text-lg text-white tracking-wide mt-0.5">
                    Dirección de envío
                  </h2>
                </div>
              </div>
              <textarea
                value={direccion}
                onChange={(e) => setDireccion(e.target.value)}
                placeholder="C/ Mayor 12, 3ºB, 28013 Madrid, España"
                rows={3}
                className="hud-input resize-none font-sans"
                maxLength={255}
                disabled={busy}
              />
              <p className="hud-readout text-[10px] text-slate-500 mt-2">
                {direccion ? `// ${direccion.length} / 255 caracteres` : '// Rellena la dirección donde quieres recibir el pedido'}
              </p>
            </div>

            {/* Método de pago */}
            <div className="hud-panel p-5">
              <div className="flex items-start gap-3 mb-4">
                <div className="p-2 border border-cockpit-neon/40">
                  <CreditCard size={16} className="text-cockpit-neon" />
                </div>
                <div>
                  <p className="hud-label">// PASO 2</p>
                  <h2 className="font-display text-lg text-white tracking-wide mt-0.5">
                    Método de pago
                  </h2>
                </div>
              </div>

              <div className="flex gap-2 mb-4">
                <button
                  onClick={() => setPagarAhora(true)}
                  className={`hud-btn !px-4 !py-2 flex-1 ${pagarAhora ? '' : 'opacity-40'}`}
                  disabled={busy}
                >
                  Pagar ahora
                </button>
                <button
                  onClick={() => setPagarAhora(false)}
                  className={`hud-btn hud-btn--cyan !px-4 !py-2 flex-1 ${!pagarAhora ? '' : 'opacity-40'}`}
                  disabled={busy}
                >
                  Dejar pendiente
                </button>
              </div>

              {pagarAhora ? (
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  {METODOS_PAGO.map((m) => (
                    <button
                      key={m.value}
                      onClick={() => setMetodoPago(m.value)}
                      disabled={busy}
                      className="hud-panel p-3 text-left transition-all !bg-black/30 hover:bg-white/[0.04]"
                      style={metodoPago === m.value ? {
                        borderColor: 'rgba(168,85,247,0.6)',
                        boxShadow: '0 0 16px rgba(168,85,247,0.18)',
                      } : {}}
                    >
                      <div className="flex items-center gap-2">
                        <m.icon size={14} className={metodoPago === m.value ? 'text-cockpit-neon' : 'text-slate-500'} />
                        <span className={`text-sm ${metodoPago === m.value ? 'text-white' : 'text-slate-400'}`}>
                          {m.label}
                        </span>
                        {metodoPago === m.value && <Check size={12} className="text-cockpit-neon ml-auto" />}
                      </div>
                      <p className="text-[10px] text-slate-500 mt-1">{m.hint}</p>
                    </button>
                  ))}
                </div>
              ) : (
                <div className="p-3 border border-cockpit-amber/40 bg-cockpit-amber/5">
                  <div className="flex gap-2">
                    <AlertCircle size={14} className="text-cockpit-amber shrink-0 mt-0.5" />
                    <p className="text-xs text-slate-300">
                      Tu pedido quedará en estado <strong className="text-cockpit-amber">Pendiente</strong>.
                      Podrás pagarlo más tarde desde "Mis pedidos". Reservamos el stock 24 horas.
                    </p>
                  </div>
                </div>
              )}
            </div>

            <div className="hud-panel p-3 !bg-black/30 flex gap-2">
              <ShieldCheck size={14} className="text-cockpit-ok shrink-0 mt-0.5" />
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Sistema de demostración académica. No se realiza ningún cargo real
                ni se procesan datos bancarios — el pago se marca como completado directamente.
              </p>
            </div>
          </motion.div>

          {/* Resumen */}
          <motion.aside
            initial={{ opacity: 0, x: 8 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.4, delay: 0.1 }}
            className="hud-panel p-5 h-fit sticky top-20 space-y-4"
          >
            <p className="hud-label">// RESUMEN</p>

            <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
              {items.map((it) => (
                <div key={it.productoId} className="flex items-start gap-2 pb-2 border-b border-cockpit-line/30 last:border-0">
                  <div className="w-8 h-8 border border-cockpit-line bg-black/40 grid place-items-center text-[10px] font-mono text-cockpit-cyan shrink-0">
                    ×{it.cantidad}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs text-white truncate">{it.nombre}</p>
                    <p className="hud-readout text-[9px] text-slate-500">
                      {Number(it.precio).toFixed(2)} €/u
                    </p>
                  </div>
                  <span className="text-xs font-mono text-cockpit-cyan">
                    {(it.precio * it.cantidad).toFixed(2)} €
                  </span>
                </div>
              ))}
            </div>

            <div className="space-y-1 text-sm pt-3 border-t border-cockpit-line/40">
              <div className="flex justify-between text-slate-400">
                <span>Subtotal</span>
                <span className="font-mono">{totalEuros.toFixed(2)} €</span>
              </div>
              <div className="flex justify-between text-slate-400 text-xs">
                <span>Envío</span>
                <span className="font-mono">Gratis</span>
              </div>
            </div>

            <div className="flex justify-between items-end pt-3 border-t border-cockpit-line/40">
              <span className="hud-label">// TOTAL</span>
              <span className="font-display text-2xl text-cockpit-ok">
                {totalEuros.toFixed(2)} €
              </span>
            </div>

            <button
              onClick={confirmar}
              disabled={busy || !direccion.trim()}
              className="hud-btn !px-4 !py-3 w-full"
            >
              {busy
                ? <><Loader2 size={14} className="animate-spin" /> Procesando…</>
                : <><Package size={14} /> Confirmar pedido</>
              }
            </button>
          </motion.aside>
        </div>
      </main>
    </div>
  );
}
