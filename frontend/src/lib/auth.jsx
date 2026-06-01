import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api, getToken, setToken } from './api.js';

const AuthCtx = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);          // { email, rol, nombre }
  const [loading, setLoading] = useState(true);    // boot inicial
  const [error, setError] = useState(null);

  // Carga /api/auth/me cuando hay token (al arrancar o tras login).
  const refresh = useCallback(async () => {
    if (!getToken()) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const { data } = await api.get('/auth/me');
      setUser(data);
    } catch (_) {
      setUser(null);
      setToken(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
    const onUnauthorized = () => { setUser(null); };
    window.addEventListener('lvlup:unauthorized', onUnauthorized);
    return () => window.removeEventListener('lvlup:unauthorized', onUnauthorized);
  }, [refresh]);

  async function login(email, password) {
    setError(null);
    try {
      const { data } = await api.post('/auth/login', { email, password });
      setToken(data.token);
      await refresh();
      return true;
    } catch (e) {
      const msg = e.response?.data?.mensaje || e.response?.data?.message
        || 'Credenciales no válidas';
      setError(msg);
      setToken(null);
      setUser(null);
      return false;
    }
  }

  function logout() {
    setToken(null);
    setUser(null);
  }

  const value = { user, loading, error, login, logout, refresh };
  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthCtx);
  if (!ctx) throw new Error('useAuth fuera de AuthProvider');
  return ctx;
}
