import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowLeft, MapPin, CreditCard, Banknote, Wallet, Check, Loader2,
  ShieldCheck, AlertCircle, Package, Lock, Calendar, Mail, Hash,
} from 'lucide-react';
import { api } from '../lib/api.js';
import { useAuth } from '../lib/auth.jsx';
import { useCarrito } from '../lib/carrito.js';
import { useToast } from '../components/admin/Toast.jsx';
import { HeaderTienda } from '../components/tienda/HeaderTienda.jsx';
import { useCockpit } from '../scenes/cockpitStore.js';

/**
 * Checkout — URL: /cockpit/checkout
 *
 *  - Precarga la direccion del perfil (/api/mi-perfil).
 *  - Permite elegir Pagar ahora vs Dejar pendiente.
 *  - Si "Pagar ahora", muestra el formulario del metodo elegido:
 *      - TARJETA:       numero, vencimiento MM/AA, CVV
 *      - PAYPAL:        email asociado
 *      - TRANSFERENCIA: IBAN espanol (ES + 22 digitos)
 *  - Los datos de pago NO viajan al backend (sistema de
 *    demostracion academica): se validan en frontend y se envia
 *    solo el campo metodoPago en POST /pedidos/{id}/pagar.
 */
const METODOS_PAGO = [
  { value: 'TARJETA',       label: 'Tarjeta',       icon: CreditCard, hint: 'Credito o debito' },
  { value: 'PAYPAL',        label: 'PayPal',        icon: Wallet,     hint: 'Pago seguro online' },
  { value: 'TRANSFERENCIA', label: 'Transferencia', icon: Banknote,   hint: 'Pago bancario' },
];

// ---------- Helpers de tarjeta ----------

function soloDigitos(str) {
  return String(str || '').replace(/\D/g, '');
}

function formatearNumeroTarjeta(str) {
  const d = soloDigitos(str).slice(0, 16);
  return d.replace(/(.{4})/g, '$1 ').trim();
}

function formatearVencimiento(str) {
  const d = soloDigitos(str).slice(0, 4);
  if (d.length <= 2) return d;
  return d.slice(0, 2) + '/' + d.slice(2);
}

function validarVencimiento(mmYY) {
  const m = /^(\d{2})\/(\d{2})$/.exec(String(mmYY || '').trim());
  if (!m) return 'Formato MM/AA';
  const mes = parseInt(m[1], 10);
  const anoCorto = parseInt(m[2], 10);
  if (mes < 1 || mes > 12) return 'Mes invalido';
  const ahora = new Date();
  const anoActualCorto = ahora.getFullYear() % 100;
  const mesActual = ahora.getMonth() + 1;
  if (anoCorto < anoActualCorto) return 'Tarjeta caducada';
  if (anoCorto === anoActualCorto && mes < mesActual) return 'Tarjeta caducada';
  return null;
}

function enmascararTarjeta(numFormateado) {
  const d = soloDigitos(numFormateado);
  if (d.length < 4) return '•••• •••• •••• ••••';
  return '•••• •••• •••• ' + d.slice(-4);
}

// ---------- Helpers de PayPal ----------

function validarEmailPaypal(email) {
  const valor = String(email || '').trim();
  if (!valor) return 'El email es obligatorio';
  const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(valor);
  if (!ok) return 'Email con formato invalido';
  if (valor.length > 254) return 'Email demasiado largo';
  return null;
}

function enmascararEmail(email) {
  const valor = String(email || '').trim();
  if (!valor) return '••••@••••.com';
  const at = valor.indexOf('@');
  if (at <= 0) return '••••@••••.com';
  return valor.charAt(0) + '••••' + valor.slice(at);
}

// ---------- Helpers de IBAN espanol ----------

function formatearIban(str) {
  const limpio = String(str || '').replace(/\s+/g, '').toUpperCase().slice(0, 24);
  return limpio.replace(/(.{4})/g, '$1 ').trim();
}

function validarIban(ibanFormateado) {
  const limpio = String(ibanFormateado || '').replace(/\s+/g, '').toUpperCase();
  if (!limpio) return 'El IBAN es obligatorio';
  if (!limpio.startsWith('ES')) return 'Solo IBAN espanol (empieza por ES)';
  if (limpio.length !== 24) return 'El IBAN espanol tiene 24 caracteres';
  if (!/^ES\d{22}$/.test(limpio)) return 'Tras ES solo puede haber digitos';
  return null;
}

function enmascararIban(ibanFormateado) {
  const limpio = String(ibanFormateado || '').replace(/\s+/g, '').toUpperCase();
  if (limpio.length < 4) return 'ES•• •••• •••• •••• •••• ••••';
  return 'ES•• •••• •••• •••• •••• ' + limpio.slice(-4);
}

export default function Checkout() {
  const { user } = useAuth();
  const { items, totalEuros, totalUnidades, vaciar } = useCarrito();
  const toast = useToast();
  const navigate = useNavigate();

  const setScene = useCockpit((s) => s.setScene);
  useEffect(() => { setScene('checkout'); }, [setScene]);

  const [direccion, setDireccion] = useState('');
  const [metodoPago, setMetodoPago] = useState('TARJETA');
  const [pagarAhora, setPagarAhora] = useState(true);
  const [busy, setBusy] = useState(false);

  // Tarjeta
  const [tNumero, setTNumero] = useState('');
  const [tVenc, setTVenc] = useState('');
  const [tCvv, setTCvv] = useState('');

  // PayPal
  const [pEmail, setPEmail] = useState('');

  // Transferencia
  const [tIban, setTIban] = useState('');

  const [errores, setErrores] = useState({});

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

  useEffect(() => {
    let cancelado = false;
    async function cargarPerfil() {
      try {
        const { data } = await api.get('/mi-perfil');
        if (cancelado || !data) return;
        if (data.direccion) {
          let dir = data.direccion;
          if (data.ciudad) dir += ', ' + data.ciudad;
          if (data.codigoPostal) dir += ' ' + data.codigoPostal;
          if (data.pais) dir += ', ' + data.pais;
          setDireccion(dir);
        }
        if (data.email) setPEmail(data.email);
      } catch (_) { /* sin perfil */ }
    }
    if (user && esCliente) cargarPerfil();
    return () => { cancelado = true; };
  }, [user, esCliente]);

  useEffect(() => {
    setErrores({});
  }, [metodoPago, pagarAhora]);

  // ---------- Validacion en vivo ----------

  const metodoPagoValido = useMemo(() => {
    if (!pagarAhora) return true;
    if (metodoPago === 'TARJETA') {
      if (soloDigitos(tNumero).length !== 16) return false;
      if (validarVencimiento(tVenc) !== null) return false;
      if (soloDigitos(tCvv).length !== 3) return false;
      return true;
    }
    if (metodoPago === 'PAYPAL') {
      return validarEmailPaypal(pEmail) === null;
    }
    if (metodoPago === 'TRANSFERENCIA') {
      return validarIban(tIban) === null;
    }
    return false;
  }, [pagarAhora, metodoPago, tNumero, tVenc, tCvv, pEmail, tIban]);

  function validarConErrores() {
    if (!pagarAhora) return true;
    const e = {};
    if (metodoPago === 'TARJETA') {
      if (soloDigitos(tNumero).length !== 16) e.numero = 'El numero de tarjeta debe tener 16 digitos';
      const errVenc = validarVencimiento(tVenc);
      if (errVenc) e.venc = errVenc;
      if (soloDigitos(tCvv).length !== 3) e.cvv = 'CVV de 3 digitos';
    } else if (metodoPago === 'PAYPAL') {
      const errEmail = validarEmailPaypal(pEmail);
      if (errEmail) e.email = errEmail;
    } else if (metodoPago === 'TRANSFERENCIA') {
      const errIban = validarIban(tIban);
      if (errIban) e.iban = errIban;
    }
    setErrores(e);
    return Object.keys(e).length === 0;
  }

  async function confirmar() {
    if (busy) return;
    if (!direccion.trim()) {
      toast.warn('La direccion de envio es obligatoria');
      return;
    }
    if (!validarConErrores()) {
      toast.err('Revisa los datos del metodo de pago');
      return;
    }

    setBusy(true);
    try {
      const payloadPedido = {
        lineas: items.map(it => ({ productoId: it.productoId, cantidad: it.cantidad })),
        direccionEnvio: direccion.trim().slice(0, 255),
      };
      const { data: pedido } = await api.post('/pedidos', payloadPedido);

      if (pagarAhora) {
        try {
          await api.post(`/pedidos/${pedido.id}/pagar`, { metodoPago });
          toast.ok(`Pedido #${pedido.id} creado y pagado con ${metodoPago}`);
        } catch (eP) {
          toast.warn(`Pedido #${pedido.id} creado, pero el pago fallo: lo veras como Pendiente en tu cuenta`);
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
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mb-4">
          <Link to="/cockpit/carrito" className="inline-flex items-center gap-1.5 text-xs tracking-widest uppercase text-cockpit-cyan hover:text-white transition-colors">
            <ArrowLeft size={12} />
            Volver al carrito
          </Link>
        </motion.div>

        <motion.header
          initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.4 }}
          className="mb-5"
        >
          <p className="hud-label text-cockpit-cyan/70">// CHECKOUT</p>
          <h1 className="font-display text-3xl tracking-wider text-white mt-1 flex items-center gap-3">
            <ShieldCheck className="text-cockpit-neon" size={26} />
            Finalizar compra
          </h1>
          <p className="hud-readout mt-1">
            Paso final · {totalUnidades} {totalUnidades === 1 ? 'articulo' : 'articulos'}
          </p>
        </motion.header>

        <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-5">
          <motion.div
            initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.4, delay: 0.05 }}
            className="space-y-5"
          >
            {/* Direccion */}
            <div className="hud-panel p-5">
              <div className="flex items-start gap-3 mb-3">
                <div className="p-2 border border-cockpit-cyan/40">
                  <MapPin size={16} className="text-cockpit-cyan" />
                </div>
                <div>
                  <p className="hud-label">// PASO 1</p>
                  <h2 className="font-display text-lg text-white tracking-wide mt-0.5">Direccion de envio</h2>
                </div>
              </div>
              <textarea
                value={direccion}
                onChange={(e) => setDireccion(e.target.value)}
                placeholder="C/ Mayor 12, 3ºB, 28013 Madrid, Espana"
                rows={3}
                className="hud-input resize-none font-sans"
                maxLength={255}
                disabled={busy}
              />
              <p className="hud-readout text-[10px] text-slate-500 mt-2">
                {direccion ? `// ${direccion.length} / 255 caracteres` : '// Rellena la direccion donde quieres recibir el pedido'}
              </p>
            </div>

            {/* Metodo de pago */}
            <div className="hud-panel p-5">
              <div className="flex items-start gap-3 mb-4">
                <div className="p-2 border border-cockpit-neon/40">
                  <CreditCard size={16} className="text-cockpit-neon" />
                </div>
                <div>
                  <p className="hud-label">// PASO 2</p>
                  <h2 className="font-display text-lg text-white tracking-wide mt-0.5">Metodo de pago</h2>
                </div>
              </div>

              <div className="flex gap-2 mb-4">
                <button onClick={() => setPagarAhora(true)} className={`hud-btn !px-4 !py-2 flex-1 ${pagarAhora ? '' : 'opacity-40'}`} disabled={busy}>
                  Pagar ahora
                </button>
                <button onClick={() => setPagarAhora(false)} className={`hud-btn hud-btn--cyan !px-4 !py-2 flex-1 ${!pagarAhora ? '' : 'opacity-40'}`} disabled={busy}>
                  Dejar pendiente
                </button>
              </div>

              {pagarAhora ? (
                <>
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

                  {/* ---------- TARJETA ---------- */}
                  {metodoPago === 'TARJETA' && (
                    <div className="mt-4 p-4 border border-cockpit-line/60 bg-black/30 space-y-3">
                      <div className="flex items-center gap-2 mb-1">
                        <Lock size={12} className="text-cockpit-cyan" />
                        <p className="hud-label">// DATOS DE LA TARJETA</p>
                      </div>

                      <label className="block">
                        <span className="hud-label">Numero de tarjeta</span>
                        <input
                          type="text" inputMode="numeric" autoComplete="cc-number"
                          value={tNumero}
                          onChange={(e) => { setTNumero(formatearNumeroTarjeta(e.target.value)); setErrores(er => ({ ...er, numero: undefined })); }}
                          placeholder="1234 5678 9012 3456"
                          className="hud-input mt-1.5 font-mono tracking-widest"
                          maxLength={19}
                          disabled={busy}
                        />
                        {errores.numero && <p className="text-xs text-cockpit-danger mt-1">{errores.numero}</p>}
                      </label>

                      <div className="grid grid-cols-2 gap-3">
                        <label className="block">
                          <span className="hud-label flex items-center gap-1.5"><Calendar size={11} /> Vencimiento</span>
                          <input
                            type="text" inputMode="numeric" autoComplete="cc-exp"
                            value={tVenc}
                            onChange={(e) => { setTVenc(formatearVencimiento(e.target.value)); setErrores(er => ({ ...er, venc: undefined })); }}
                            placeholder="MM/AA"
                            className="hud-input mt-1.5 font-mono"
                            maxLength={5}
                            disabled={busy}
                          />
                          {errores.venc && <p className="text-xs text-cockpit-danger mt-1">{errores.venc}</p>}
                        </label>

                        <label className="block">
                          <span className="hud-label flex items-center gap-1.5"><Lock size={11} /> CVV</span>
                          <input
                            type="password" inputMode="numeric" autoComplete="cc-csc"
                            value={tCvv}
                            onChange={(e) => { setTCvv(soloDigitos(e.target.value).slice(0, 3)); setErrores(er => ({ ...er, cvv: undefined })); }}
                            placeholder="•••"
                            className="hud-input mt-1.5 font-mono tracking-widest"
                            maxLength={3}
                            disabled={busy}
                          />
                          {errores.cvv && <p className="text-xs text-cockpit-danger mt-1">{errores.cvv}</p>}
                        </label>
                      </div>

                      <p className="hud-readout text-[10px] text-slate-500 pt-1">
                        // Los datos no salen de tu navegador. Sistema de demostracion academica.
                      </p>
                    </div>
                  )}

                  {/* ---------- PAYPAL ---------- */}
                  {metodoPago === 'PAYPAL' && (
                    <div className="mt-4 p-4 border border-cockpit-line/60 bg-black/30 space-y-3">
                      <div className="flex items-center gap-2 mb-1">
                        <Wallet size={12} className="text-cockpit-cyan" />
                        <p className="hud-label">// CUENTA PAYPAL</p>
                      </div>

                      <label className="block">
                        <span className="hud-label flex items-center gap-1.5"><Mail size={11} /> Email de PayPal</span>
                        <input
                          type="email" autoComplete="email"
                          value={pEmail}
                          onChange={(e) => { setPEmail(e.target.value); setErrores(er => ({ ...er, email: undefined })); }}
                          placeholder="tu-email@ejemplo.com"
                          className="hud-input mt-1.5 font-sans"
                          maxLength={254}
                          disabled={busy}
                        />
                        {errores.email && <p className="text-xs text-cockpit-danger mt-1">{errores.email}</p>}
                      </label>

                      <p className="hud-readout text-[10px] text-slate-500 pt-1">
                        // Se te redirigira a PayPal para confirmar el pago. Modo demo: el cargo se simula.
                      </p>
                    </div>
                  )}

                  {/* ---------- TRANSFERENCIA ---------- */}
                  {metodoPago === 'TRANSFERENCIA' && (
                    <div className="mt-4 p-4 border border-cockpit-line/60 bg-black/30 space-y-3">
                      <div className="flex items-center gap-2 mb-1">
                        <Banknote size={12} className="text-cockpit-cyan" />
                        <p className="hud-label">// DATOS BANCARIOS</p>
                      </div>

                      <label className="block">
                        <span className="hud-label flex items-center gap-1.5"><Hash size={11} /> IBAN (Espana)</span>
                        <input
                          type="text" autoComplete="off"
                          value={tIban}
                          onChange={(e) => { setTIban(formatearIban(e.target.value)); setErrores(er => ({ ...er, iban: undefined })); }}
                          placeholder="ES12 3456 7890 1234 5678 9012"
                          className="hud-input mt-1.5 font-mono tracking-wider"
                          maxLength={29}
                          disabled={busy}
                        />
                        {errores.iban && <p className="text-xs text-cockpit-danger mt-1">{errores.iban}</p>}
                      </label>

                      <p className="hud-readout text-[10px] text-slate-500 pt-1">
                        // Recibiras los datos de la cuenta destino por email. Plazo de ingreso: 24 horas.
                      </p>
                    </div>
                  )}
                </>
              ) : (
                <div className="p-3 border border-cockpit-amber/40 bg-cockpit-amber/5">
                  <div className="flex gap-2">
                    <AlertCircle size={14} className="text-cockpit-amber shrink-0 mt-0.5" />
                    <p className="text-xs text-slate-300">
                      Tu pedido quedara en estado <strong className="text-cockpit-amber">Pendiente</strong>.
                      Podras pagarlo mas tarde desde "Mis pedidos". Reservamos el stock 24 horas.
                    </p>
                  </div>
                </div>
              )}
            </div>

            <div className="hud-panel p-3 !bg-black/30 flex gap-2">
              <ShieldCheck size={14} className="text-cockpit-ok shrink-0 mt-0.5" />
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Sistema de demostracion academica. No se realiza ningun cargo real
                ni se procesan datos bancarios — el pago se marca como completado directamente.
              </p>
            </div>
          </motion.div>

          {/* ---------- Resumen ---------- */}
          <motion.aside
            initial={{ opacity: 0, x: 8 }} animate={{ opacity: 1, x: 0 }} transition={{ duration: 0.4, delay: 0.1 }}
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
                    <p className="hud-readout text-[9px] text-slate-500">{Number(it.precio).toFixed(2)} €/u</p>
                  </div>
                  <span className="text-xs font-mono text-cockpit-cyan">{(it.precio * it.cantidad).toFixed(2)} €</span>
                </div>
              ))}
            </div>

            <div className="space-y-1 text-sm pt-3 border-t border-cockpit-line/40">
              <div className="flex justify-between text-slate-400">
                <span>Subtotal</span>
                <span className="font-mono">{totalEuros.toFixed(2)} €</span>
              </div>
              <div className="flex justify-between text-slate-400 text-xs">
                <span>Envio</span>
                <span className="font-mono">Gratis</span>
              </div>
            </div>

            {/* Metodo de pago (enmascarado) */}
            {pagarAhora && (
              <div className="pt-2 border-t border-cockpit-line/40">
                {metodoPago === 'TARJETA' && (
                  <>
                    <p className="hud-label mb-1">// TARJETA</p>
                    <p className="font-mono text-sm text-slate-300 tracking-widest">{enmascararTarjeta(tNumero)}</p>
                  </>
                )}
                {metodoPago === 'PAYPAL' && (
                  <>
                    <p className="hud-label mb-1">// PAYPAL</p>
                    <p className="font-mono text-sm text-slate-300">{enmascararEmail(pEmail)}</p>
                  </>
                )}
                {metodoPago === 'TRANSFERENCIA' && (
                  <>
                    <p className="hud-label mb-1">// IBAN</p>
                    <p className="font-mono text-sm text-slate-300 tracking-wider">{enmascararIban(tIban)}</p>
                  </>
                )}
              </div>
            )}

            <div className="flex justify-between items-end pt-3 border-t border-cockpit-line/40">
              <span className="hud-label">// TOTAL</span>
              <span className="font-display text-2xl text-cockpit-ok">{totalEuros.toFixed(2)} €</span>
            </div>

            <button
              onClick={confirmar}
              disabled={busy || !direccion.trim() || !metodoPagoValido}
              className="hud-btn !px-4 !py-3 w-full disabled:opacity-40 disabled:cursor-not-allowed"
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
