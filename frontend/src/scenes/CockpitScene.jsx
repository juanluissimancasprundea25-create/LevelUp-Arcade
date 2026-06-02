import { Suspense, useEffect, useRef } from 'react';
import { Canvas } from '@react-three/fiber';
import { Environment, PerspectiveCamera, Sparkles } from '@react-three/drei';
import gsap from 'gsap';
import { Starfield } from './Starfield.jsx';
import { Nebula } from './Nebula.jsx';
import { ArcadeMachine } from './ArcadeMachine.jsx';
import { useCockpit } from './cockpitStore.js';

/**
 * Componente que solo vive dentro del <Canvas> y mueve la camara
 * con GSAP segun la "escena" actual del store. Cada ruta de la SPA
 * lanza un cambio de escena y la camara hace tween a su pose.
 *
 * Las poses estan ajustadas para que la maquina arcade (situada
 * en world coords ~ [2.2, -0.3, -1.5]) quede SIEMPRE fuera del
 * area central del contenido HUD. En rutas con mucho texto encima
 * (tienda, checkout) la camara mira fuerte al cielo izquierdo para
 * dejar el centro limpio.
 */
function CameraDirector() {
  const cameraRef = useRef();
  const scene = useCockpit((s) => s.scene);

  // Poses por escena: [posX, posY, posZ, lookX, lookY, lookZ]
  // - landing: maquina visible decorativa a la derecha
  // - login:   ligeramente desplazada para hueco al formulario
  // - tienda:  camara mira al starfield, maquina fuera de plano
  // - checkout: igual que tienda pero un toque mas amplia
  // - warp:    transicion hyperdrive
  // - dashboard: vista zenital para el panel admin
  const poses = {
    landing:   { pos: [-1.2, 0.5, 7.5],  look: [1.5, 0.2, 0]  },
    login:     { pos: [-0.5, 0.6, 5.5],  look: [2.5, 0.4, 0]  },
    tienda:    { pos: [-3.5, 1.2, 9.0],  look: [-5.0, 0.8, -4] },
    checkout:  { pos: [-4.0, 1.5, 10.0], look: [-6.0, 1.0, -5] },
    warp:      { pos: [0, 0.5, 1.2],     look: [0, 0.5, -10]  },
    dashboard: { pos: [0, 2.2, 9],       look: [0, 0, -2]     },
  };

  useEffect(() => {
    if (!cameraRef.current) return;
    const pose = poses[scene] || poses.landing;
    const cam = cameraRef.current;

    const isWarp = scene === 'warp';
    gsap.to(cam.position, {
      x: pose.pos[0], y: pose.pos[1], z: pose.pos[2],
      duration: isWarp ? 0.9 : 1.6,
      ease: isWarp ? 'power4.in' : 'power3.inOut',
    });
    const lookProxy = { x: 0, y: 0.3, z: 0 };
    gsap.to(lookProxy, {
      x: pose.look[0], y: pose.look[1], z: pose.look[2],
      duration: isWarp ? 0.9 : 1.6,
      ease: isWarp ? 'power4.in' : 'power3.inOut',
      onUpdate: () => {
        cam.lookAt(lookProxy.x, lookProxy.y, lookProxy.z);
      },
    });
  }, [scene]);

  return <PerspectiveCamera ref={cameraRef} makeDefault position={[0, 0.5, 6.5]} fov={55} />;
}

export function CockpitScene() {
  return (
    <Canvas
      gl={{ antialias: true, alpha: false, powerPreference: 'high-performance' }}
      dpr={[1, 1.75]}
      shadows={false}
      style={{ position: 'fixed', inset: 0, zIndex: 0 }}
    >
      <color attach="background" args={['#020408']} />
      <fog attach="fog" args={['#020408', 18, 60]} />

      <Suspense fallback={null}>
        <CameraDirector />

        <ambientLight intensity={0.25} />
        <directionalLight position={[5, 8, 5]} intensity={0.8} color="#c4b5fd" />
        <pointLight position={[-6, 2, -2]} intensity={1.2} color="#22d3ee" />
        <pointLight position={[6, -2, -1]} intensity={1.2} color="#a855f7" />

        <Nebula />
        <Starfield count={4500} />
        <Sparkles count={140} scale={20} size={3} speed={0.25} color="#a855f7" opacity={0.6} />

        <ArcadeMachine position={[2.2, -0.3, -1.5]} scale={0.7} />

        <Environment preset="night" />
      </Suspense>
    </Canvas>
  );
}
