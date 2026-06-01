import axios from 'axios';

// En dev Vite hace proxy /api -> :8080. En prod la SPA y la API
// viven en el mismo origen, así que basta con baseURL relativa.
export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

const TOKEN_KEY = 'lvlup_token';

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) sessionStorage.setItem(TOKEN_KEY, token);
  else sessionStorage.removeItem(TOKEN_KEY);
}

// Adjunta el JWT en cada request si existe.
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Si el backend nos devuelve 401, limpiamos token y disparamos un
// evento global que el AuthProvider escucha para volver al landing.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response && err.response.status === 401) {
      setToken(null);
      window.dispatchEvent(new CustomEvent('lvlup:unauthorized'));
    }
    return Promise.reject(err);
  }
);
