import { useMemo } from 'react';
import { motion } from 'framer-motion';
import { Package, Users, ShoppingCart, RefreshCw } from 'lucide-react';
import { useFetch } from '../../lib/hooks.js';
import { useAuth } from '../../lib/auth.jsx';
import { KpiCard } from '../../components/admin/KpiCard.jsx';
import { EstadoPedidosDonut } from '../../components/admin/EstadoPedidosDonut.jsx';
import { IngresosMes } from '../../components/admin/IngresosMes.jsx';
import { StockAlertList } from '../../components/admin/StockAlertList.jsx';
import { CategoriasBar } from '../../components/admin/CategoriasBar.jsx';

export default function DashboardHome() {
  const { user } = useAuth();

  // Llamadas paralelas a la API real. Si alguna falla, el resto
  // sigue mostrandose con sus datos.
  const productos    = useFetch('/productos');
  const stockBajo    = useFetch('/productos', { params: { bajoStock: true } });
  const clientes     = useFetch('/clientes');
  const pedidos      = useFetch('/pedidos');

  const cargandoAlgo = productos.loading || pedidos.loading;

  function refreshAll() {
    productos.refresh();
    stockBajo.refresh();
    clientes.refresh();
    pedidos.refresh();
  }

  // Pedidos de hoy (para footer del KPI de pedidos)
  const pedidosHoy = useMemo(() => {
    if (!pedidos.data) return 0;
    const hoy = new Date().toDateString();
    return pedidos.data.filter(p => new Date(p.fechaPedido).toDateString() === hoy).length;
  }, [pedidos.data]);

  const stockBajoCount = stockBajo.data?.length ?? 0;
  const stockTone = stockBajoCount === 0 ? 'ok' : stockBajoCount < 5 ? 'warn' : 'danger';

  return (
    <div className="pb-8">
      {/* Header */}
      <motion.header
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="flex items-end justify-between mb-6 pt-2"
      >
        <div>
          <p className="hud-label text-cockpit-cyan/70">// PUENTE DE MANDO</p>
          <h1 className="font-display text-2xl xl:text-3xl tracking-wider text-white mt-1">
            Hola, <span className="text-cockpit-neon">{user?.nombre || 'piloto'}</span>
          </h1>
          <p className="hud-readout mt-1">
            {new Date().toLocaleDateString('es-ES', {
              weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
            }).toUpperCase()}
          </p>
        </div>

        <button
          onClick={refreshAll}
          disabled={cargandoAlgo}
          className="hud-btn hud-btn--cyan !px-4 !py-2"
        >
          <RefreshCw size={14} className={cargandoAlgo ? 'animate-spin' : ''} />
          <span className="hidden md:inline">{cargandoAlgo ? 'Sincronizando' : 'Refrescar'}</span>
        </button>
      </motion.header>

      {/* KPIs (4 tarjetas: productos, stock bajo, clientes, pedidos) */}
      <section className="grid grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <KpiCard
          label="// PRODUCTOS"
          value={productos.data?.length ?? 0}
          icon={Package}
          loading={productos.loading}
          footer={productos.data ? `${productos.data.filter(p => p.activo).length} activos` : null}
          delay={0.05}
        />
        <KpiCard
          label="// STOCK BAJO"
          value={stockBajoCount}
          icon={Package}
          tone={stockTone}
          loading={stockBajo.loading}
          footer={stockBajoCount === 0 ? 'nivel nominal' : 'requieren atencion'}
          delay={0.1}
        />
        <KpiCard
          label="// CLIENTES"
          value={clientes.data?.length ?? 0}
          icon={Users}
          loading={clientes.loading}
          delay={0.15}
        />
        <KpiCard
          label="// PEDIDOS"
          value={pedidos.data?.length ?? 0}
          icon={ShoppingCart}
          loading={pedidos.loading}
          footer={pedidosHoy ? `+${pedidosHoy} hoy` : 'sin pedidos hoy'}
          delay={0.2}
        />
      </section>

      {/* Ingresos del mes ocupa todo el ancho ahora */}
      <section className="mb-6">
        <IngresosMes pedidos={pedidos.data} loading={pedidos.loading} />
      </section>

      {/* Fila de graficos */}
      <section className="grid grid-cols-1 xl:grid-cols-3 gap-4">
        <EstadoPedidosDonut pedidos={pedidos.data} loading={pedidos.loading} />
        <StockAlertList productos={stockBajo.data} loading={stockBajo.loading} />
        <CategoriasBar productos={productos.data} loading={productos.loading} />
      </section>

      {/* Si alguna llamada fallo, lo decimos sin romper la pagina */}
      {[productos, stockBajo, clientes, pedidos]
        .filter(q => q.error)
        .map((q, i) => (
          <div
            key={i}
            className="mt-4 px-3 py-2 border border-cockpit-danger/60 text-cockpit-danger
                       text-xs tracking-wider bg-cockpit-danger/10"
          >
            Error al cargar datos: {q.error}
          </div>
        ))}
    </div>
  );
}
