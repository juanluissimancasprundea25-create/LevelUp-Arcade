import { useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from '../../components/admin/Sidebar.jsx';
import { useCockpit } from '../../scenes/cockpitStore.js';

/**
 * Layout común para todas las rutas /admin/*.
 *
 *  - Posiciona la cámara del cockpit en pose 'dashboard' (cámara lejana)
 *  - Aplica un velo oscuro semitransparente sobre el Canvas 3D para
 *    que el contenido denso (tablas, gráficos) sea legible sin matar
 *    la sensación cockpit.
 *  - Renderiza Sidebar persistente a la izquierda + Outlet para
 *    la ruta hija a la derecha.
 */
export function AdminLayout() {
  const setScene = useCockpit((s) => s.setScene);

  useEffect(() => {
    setScene('dashboard');
  }, [setScene]);

  return (
    <div className="relative w-full h-full pointer-events-none">
      {/* Velo oscuro sobre el cockpit 3D, no captura clicks salvo donde marquemos */}
      <div
        className="fixed inset-0 z-0 pointer-events-none"
        style={{
          background:
            'radial-gradient(ellipse at 30% 50%, rgba(2,4,8,0.55) 0%, rgba(2,4,8,0.85) 100%)',
        }}
      />

      <div className="relative z-10 w-full h-full pointer-events-auto">
        <Sidebar />

        {/* Contenido scrollable, dejando espacio para el sidebar */}
        <main
          className="ml-[84px] xl:ml-[250px] mr-4 my-4 h-[calc(100vh-2rem)]
                     overflow-y-auto pr-2"
        >
          <Outlet />
        </main>
      </div>
    </div>
  );
}
