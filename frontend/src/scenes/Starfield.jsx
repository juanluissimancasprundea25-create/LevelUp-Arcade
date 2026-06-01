import { useMemo, useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';

/**
 * Starfield procedural: 4000 puntos distribuidos en una esfera grande
 * con tres tonos (blanco, cyan, violeta). Rota lentamente alrededor
 * del eje Y para dar sensación de deriva del cockpit.
 */
export function Starfield({ count = 4000, radius = 80 }) {
  const ref = useRef();

  const { positions, colors } = useMemo(() => {
    const positions = new Float32Array(count * 3);
    const colors = new Float32Array(count * 3);
    const palette = [
      new THREE.Color('#ffffff'),
      new THREE.Color('#a855f7'),
      new THREE.Color('#22d3ee'),
      new THREE.Color('#fbbf24'),
    ];
    for (let i = 0; i < count; i++) {
      // Punto aleatorio en superficie esférica para evitar densidad central rara
      const r = radius * (0.4 + Math.random() * 0.6);
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.acos(2 * Math.random() - 1);
      positions[i * 3]     = r * Math.sin(phi) * Math.cos(theta);
      positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta);
      positions[i * 3 + 2] = r * Math.cos(phi);

      const c = palette[Math.floor(Math.random() * palette.length)];
      // Algunas estrellas más tenues
      const brightness = 0.4 + Math.random() * 0.6;
      colors[i * 3]     = c.r * brightness;
      colors[i * 3 + 1] = c.g * brightness;
      colors[i * 3 + 2] = c.b * brightness;
    }
    return { positions, colors };
  }, [count, radius]);

  useFrame((_, delta) => {
    if (ref.current) {
      ref.current.rotation.y += delta * 0.012;
      ref.current.rotation.x += delta * 0.004;
    }
  });

  return (
    <points ref={ref}>
      <bufferGeometry>
        <bufferAttribute attach="attributes-position" args={[positions, 3]} />
        <bufferAttribute attach="attributes-color" args={[colors, 3]} />
      </bufferGeometry>
      <pointsMaterial
        size={0.18}
        sizeAttenuation
        vertexColors
        transparent
        opacity={0.95}
        depthWrite={false}
        blending={THREE.AdditiveBlending}
      />
    </points>
  );
}
