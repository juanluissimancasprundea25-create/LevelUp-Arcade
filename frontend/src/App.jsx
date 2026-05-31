import { Routes, Route, Navigate } from 'react-router-dom';
import { CockpitScene } from './scenes/CockpitScene.jsx';
import { WarpOverlay } from './components/WarpOverlay.jsx';
import { useAuth } from './lib/auth.jsx';
import Landing from './routes/Landing.jsx';
import Login from './routes/Login.jsx';
import Registro from './routes/Registro.jsx';
import Tienda from './routes/Tienda.jsx';
import TiendaProducto from './routes/TiendaProducto.jsx';
import Carrito from './routes/Carrito.jsx';
import Checkout from './routes/Checkout.jsx';
import { MiCuentaLayout } from './routes/mi-cuenta/MiCuentaLayout.jsx';
import MiCuentaHome from './routes/mi-cuenta/MiCuentaHome.jsx';
import MisPedidos from './routes/mi-cuenta/MisPedidos.jsx';
import MiPedidoDetalle from './routes/mi-cuenta/MiPedidoDetalle.jsx';
import MisFacturas from './routes/mi-cuenta/MisFacturas.jsx';
import MisDevoluciones from './routes/mi-cuenta/MisDevoluciones.jsx';
import MisMensajes from './routes/mi-cuenta/MisMensajes.jsx';
import MiPerfil from './routes/mi-cuenta/MiPerfil.jsx';
import { AdminLayout } from './routes/admin/AdminLayout.jsx';
import DashboardHome from './routes/admin/DashboardHome.jsx';
import Productos from './routes/admin/Productos.jsx';
import Categorias from './routes/admin/Categorias.jsx';
import Clientes from './routes/admin/Clientes.jsx';
import Proveedores from './routes/admin/Proveedores.jsx';
import Pedidos from './routes/admin/Pedidos.jsx';
import Facturas from './routes/admin/Facturas.jsx';
import Devoluciones from './routes/admin/Devoluciones.jsx';
import Chat from './routes/admin/Chat.jsx';
import AsistenteIA from './routes/admin/AsistenteIA.jsx';
import Auditoria from './routes/admin/Auditoria.jsx';
import { ToastProvider } from './components/admin/Toast.jsx';

function Protected({ children, roles }) {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (!user) return <Navigate to="/cockpit/login" replace />;
  if (roles) {
    const rolNormalizado = String(user.rol || '').replace(/^ROLE_/, '').toUpperCase();
    if (!roles.includes(rolNormalizado)) {
      return <Navigate to="/cockpit" replace />;
    }
  }
  return children;
}

export default function App() {
  return (
    <ToastProvider>
      <div className="w-screen h-screen overflow-hidden">
        <CockpitScene />
        <div className="crt-scanlines" />
        <WarpOverlay />

        <div className="relative z-10 w-full h-full overflow-y-auto">
          <Routes>
            {/* Públicas */}
            <Route path="/cockpit" element={<Landing />} />
            <Route path="/cockpit/login" element={<Login />} />
            <Route path="/cockpit/registro" element={<Registro />} />
            <Route path="/cockpit/tienda" element={<Tienda />} />
            <Route path="/cockpit/tienda/producto/:id" element={<TiendaProducto />} />
            <Route path="/cockpit/carrito" element={<Carrito />} />

            <Route
              path="/cockpit/checkout"
              element={
                <Protected roles={['CLIENTE']}>
                  <Checkout />
                </Protected>
              }
            />

            {/* Mi cuenta — ahora completa */}
            <Route
              path="/cockpit/mi-cuenta"
              element={
                <Protected roles={['CLIENTE']}>
                  <MiCuentaLayout />
                </Protected>
              }
            >
              <Route index             element={<MiCuentaHome />} />
              <Route path="pedidos"    element={<MisPedidos />} />
              <Route path="pedidos/:id" element={<MiPedidoDetalle />} />
              <Route path="facturas"    element={<MisFacturas />} />
              <Route path="devoluciones" element={<MisDevoluciones />} />
              <Route path="chat"        element={<MisMensajes />} />
              <Route path="perfil"      element={<MiPerfil />} />
            </Route>

            {/* Admin */}
            <Route
              path="/cockpit/admin"
              element={
                <Protected roles={['ADMIN', 'EMPLEADO']}>
                  <AdminLayout />
                </Protected>
              }
            >
              <Route index element={<DashboardHome />} />
              <Route path="productos"    element={<Productos />} />
              <Route path="categorias"   element={<Categorias />} />
              <Route path="clientes"     element={<Clientes />} />
              <Route path="proveedores"  element={<Proveedores />} />
              <Route path="pedidos"      element={<Pedidos />} />
              <Route path="facturas"     element={<Facturas />} />
              <Route path="devoluciones" element={<Devoluciones />} />
              <Route path="chat"         element={<Chat />} />
              <Route path="ia"           element={
                <Protected roles={['ADMIN']}>
                  <AsistenteIA />
                </Protected>
              } />
              <Route path="auditoria"    element={
                <Protected roles={['ADMIN']}>
                  <Auditoria />
                </Protected>
              } />
            </Route>

            <Route path="*" element={<Navigate to="/cockpit" replace />} />
          </Routes>
        </div>
      </div>
    </ToastProvider>
  );
}
