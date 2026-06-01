import { useEffect, useRef } from 'react';
import { useCockpit } from '../scenes/cockpitStore.js';

/**
 * Overlay HTML 2D que dibuja 80 "streaks" verticales que se estiran
 * cuando el cockpit entra en hyperdrive. Se combina con el zoom de
 * cámara 3D del CameraDirector para multiplicar la sensación.
 */
export function WarpOverlay() {
  const warping = useCockpit((s) => s.warping);
  const ref = useRef();

  useEffect(() => {
    if (!ref.current) return;
    ref.current.style.opacity = warping ? '1' : '0';
  }, [warping]);

  // Generamos las streaks una vez
  const streaks = Array.from({ length: 80 }, (_, i) => ({
    x: (i / 80) * 100,
    delay: Math.random() * 0.4,
    duration: 0.9 + Math.random() * 0.6,
    hue: i % 3 === 0 ? '#22d3ee' : i % 3 === 1 ? '#a855f7' : '#ffffff',
  }));

  return (
    <div
      ref={ref}
      className="pointer-events-none fixed inset-0 z-30 transition-opacity duration-200"
      style={{ opacity: 0 }}
    >
      {streaks.map((s, i) => (
        <div
          key={i}
          style={{
            position: 'absolute',
            left: `${s.x}%`,
            top: '50%',
            width: '2px',
            height: '6px',
            background: s.hue,
            boxShadow: `0 0 8px ${s.hue}, 0 0 20px ${s.hue}`,
            animation: warping
              ? `warp-streak ${s.duration}s ${s.delay}s cubic-bezier(.6,.05,.95,.55) forwards`
              : 'none',
            transformOrigin: 'center',
          }}
        />
      ))}
      {/* Flash blanco breve al final del salto */}
      <div
        className="absolute inset-0"
        style={{
          background: 'radial-gradient(circle, rgba(255,255,255,0.6), transparent 70%)',
          opacity: warping ? 0 : 0,
          animation: warping ? 'flash-warp 0.9s 0.5s forwards' : 'none',
        }}
      />
      <style>{`
        @keyframes flash-warp {
          0% { opacity: 0; }
          70% { opacity: 0.85; }
          100% { opacity: 0; }
        }
      `}</style>
    </div>
  );
}
