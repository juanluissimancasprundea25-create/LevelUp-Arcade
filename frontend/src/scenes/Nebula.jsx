import { useMemo, useRef } from 'react';
import { useFrame } from '@react-three/fiber';
import * as THREE from 'three';

/**
 * Nebula: plano enorme detrás de las estrellas con shader procedural
 * (noise simplificado) que pulsa lento. Es la "atmósfera espacial".
 */
const vertexShader = /* glsl */ `
  varying vec2 vUv;
  void main() {
    vUv = uv;
    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
  }
`;

const fragmentShader = /* glsl */ `
  precision highp float;
  varying vec2 vUv;
  uniform float uTime;
  uniform vec3  uColorA;   // violeta
  uniform vec3  uColorB;   // cyan
  uniform vec3  uColorC;   // negro azulado

  // Hash + noise estándar
  float hash(vec2 p){ return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
  float noise(vec2 p){
    vec2 i = floor(p); vec2 f = fract(p);
    float a = hash(i), b = hash(i + vec2(1.0,0.0));
    float c = hash(i + vec2(0.0,1.0)), d = hash(i + vec2(1.0,1.0));
    vec2 u = f*f*(3.0-2.0*f);
    return mix(a,b,u.x) + (c-a)*u.y*(1.0-u.x) + (d-b)*u.x*u.y;
  }
  float fbm(vec2 p){
    float v = 0.0; float a = 0.5;
    for(int i = 0; i < 5; i++) { v += a * noise(p); p *= 2.0; a *= 0.5; }
    return v;
  }

  void main() {
    vec2 uv = vUv * 2.5;
    float t = uTime * 0.04;
    float n = fbm(uv + vec2(t, -t * 0.6));
    float m = fbm(uv * 1.7 + vec2(-t * 0.8, t));
    float mask = smoothstep(0.25, 0.85, n * 0.7 + m * 0.5);

    vec3 col = mix(uColorC, uColorA, mask);
    col = mix(col, uColorB, smoothstep(0.55, 0.95, m));

    // Viñeta sutil
    float vig = smoothstep(1.2, 0.2, length(vUv - 0.5) * 1.6);
    col *= vig * 0.9 + 0.1;

    gl_FragColor = vec4(col, 1.0);
  }
`;

export function Nebula() {
  const matRef = useRef();
  const uniforms = useMemo(() => ({
    uTime:   { value: 0 },
    uColorA: { value: new THREE.Color('#3b1a78') },  // violeta profundo
    uColorB: { value: new THREE.Color('#0e4a6b') },  // cyan apagado
    uColorC: { value: new THREE.Color('#020408') },  // casi negro
  }), []);

  useFrame((state) => {
    if (matRef.current) matRef.current.uniforms.uTime.value = state.clock.elapsedTime;
  });

  return (
    <mesh position={[0, 0, -60]}>
      <planeGeometry args={[260, 160]} />
      <shaderMaterial
        ref={matRef}
        vertexShader={vertexShader}
        fragmentShader={fragmentShader}
        uniforms={uniforms}
        depthWrite={false}
      />
    </mesh>
  );
}
