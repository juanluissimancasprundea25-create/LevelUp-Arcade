import { useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import { Float, Edges } from '@react-three/drei';
import * as THREE from 'three';

/**
 * ArcadeMachine: máquina recreativa modelada con primitivas
 * (cabinet + pantalla + marquesina + joystick + botones). Evita
 * descargar GLTF para que el bundle inicial sea minúsculo. Más
 * adelante, en Fase 8, se puede swap por un modelo real de Sketchfab.
 */
export function ArcadeMachine(props) {
  const ref = useRef();

  // Gemütliche idle: el cabinet "respira" y rota muy lento.
  useFrame((state) => {
    if (!ref.current) return;
    const t = state.clock.elapsedTime;
    ref.current.rotation.y = Math.sin(t * 0.3) * 0.12;
    ref.current.position.y = Math.sin(t * 0.8) * 0.08;
  });

  return (
    <Float speed={1.2} rotationIntensity={0.15} floatIntensity={0.5}>
      <group ref={ref} {...props} dispose={null}>
        {/* CABINET ---------------------------------------------------- */}
        <mesh position={[0, 0, 0]} castShadow>
          <boxGeometry args={[1.6, 3.2, 1.4]} />
          <meshStandardMaterial
            color="#0a0a1f"
            metalness={0.6}
            roughness={0.35}
            envMapIntensity={0.6}
          />
          <Edges threshold={15} color="#a855f7" scale={1.001} />
        </mesh>

        {/* MARQUESINA superior -------------------------------------- */}
        <mesh position={[0, 1.85, 0.05]}>
          <boxGeometry args={[1.55, 0.55, 1.45]} />
          <meshStandardMaterial
            color="#1a0d2e"
            emissive="#a855f7"
            emissiveIntensity={1.4}
            metalness={0.4}
            roughness={0.5}
          />
        </mesh>
        {/* "LEVELUP" pixel en marquesina (placas glow) */}
        <mesh position={[0, 1.85, 0.78]}>
          <planeGeometry args={[1.3, 0.4]} />
          <meshBasicMaterial color="#22d3ee" toneMapped={false} />
        </mesh>

        {/* PANTALLA (CRT) ------------------------------------------- */}
        <mesh position={[0, 0.55, 0.72]}>
          <boxGeometry args={[1.2, 0.9, 0.05]} />
          <meshStandardMaterial
            color="#02030a"
            emissive="#22d3ee"
            emissiveIntensity={0.9}
            metalness={0.3}
            roughness={0.2}
          />
          <Edges threshold={15} color="#22d3ee" scale={1.001} />
        </mesh>

        {/* Marco de pantalla */}
        <mesh position={[0, 0.55, 0.7]}>
          <boxGeometry args={[1.35, 1.05, 0.04]} />
          <meshStandardMaterial color="#0a0a1f" metalness={0.8} roughness={0.3} />
        </mesh>

        {/* PANEL DE CONTROL inclinado ------------------------------ */}
        <mesh position={[0, -0.3, 0.7]} rotation={[Math.PI * 0.08, 0, 0]}>
          <boxGeometry args={[1.4, 0.5, 0.5]} />
          <meshStandardMaterial color="#0a0a1f" metalness={0.7} roughness={0.4} />
        </mesh>

        {/* JOYSTICK */}
        <group position={[-0.3, -0.05, 0.85]} rotation={[Math.PI * 0.08, 0, 0]}>
          <mesh>
            <cylinderGeometry args={[0.06, 0.06, 0.25, 16]} />
            <meshStandardMaterial color="#1a1a2e" metalness={0.8} roughness={0.3} />
          </mesh>
          <mesh position={[0, 0.18, 0]}>
            <sphereGeometry args={[0.1, 16, 16]} />
            <meshStandardMaterial color="#ef4444" emissive="#ef4444" emissiveIntensity={0.5} />
          </mesh>
        </group>

        {/* BOTONES */}
        {[
          [0.15, '#22d3ee'],
          [0.32, '#fbbf24'],
          [0.49, '#34d399'],
        ].map(([x, color], i) => (
          <mesh key={i} position={[x, -0.05, 0.85]} rotation={[Math.PI * 0.08, 0, 0]}>
            <cylinderGeometry args={[0.07, 0.07, 0.05, 24]} />
            <meshStandardMaterial
              color={color}
              emissive={color}
              emissiveIntensity={0.6}
              metalness={0.4}
              roughness={0.4}
            />
          </mesh>
        ))}

        {/* PEDESTAL inferior con glow neón */}
        <mesh position={[0, -1.75, 0]}>
          <boxGeometry args={[1.7, 0.18, 1.5]} />
          <meshStandardMaterial
            color="#a855f7"
            emissive="#a855f7"
            emissiveIntensity={1.8}
            toneMapped={false}
          />
        </mesh>

        {/* Luz puntual interna para que la pantalla "ilumine" */}
        <pointLight position={[0, 0.55, 1]} intensity={1.2} distance={5} color="#22d3ee" />
        <pointLight position={[0, -1.7, 0]} intensity={1.5} distance={4} color="#a855f7" />
      </group>
    </Float>
  );
}
