import { create } from 'zustand';

/**
 * Store global ligero para coordinar:
 *  - `scene`: en qué pose está la cámara ('landing' | 'login' | 'warp' | 'dashboard')
 *  - `warping`: si está en plena transición hyperdrive (bloquea inputs)
 *
 * Cada ruta dispara setScene(...) cuando se monta. CameraDirector
 * dentro del <Canvas> lo escucha y hace tween con GSAP.
 */
export const useCockpit = create((set) => ({
  scene: 'landing',
  warping: false,
  setScene: (scene) => set({ scene }),
  warpTo: async (targetScene) => {
    set({ warping: true, scene: 'warp' });
    await new Promise((r) => setTimeout(r, 900));
    set({ scene: targetScene, warping: false });
  },
}));
