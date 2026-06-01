import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from './api.js';

/**
 * Hook genérico para GET de la API. Devuelve { data, loading, error, refresh }.
 * Cancela peticiones pendientes si el componente se desmonta antes de tiempo.
 *
 *   const { data, loading, refresh } = useFetch('/productos');
 */
export function useFetch(path, { params } = {}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const mounted = useRef(true);

  // Serializamos params para que el efecto se re-dispare si cambian
  const paramsKey = params ? JSON.stringify(params) : '';

  const run = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api.get(path, { params });
      if (mounted.current) setData(res.data);
    } catch (e) {
      if (mounted.current) setError(e.response?.data?.mensaje || e.message);
    } finally {
      if (mounted.current) setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [path, paramsKey]);

  useEffect(() => {
    mounted.current = true;
    run();
    return () => { mounted.current = false; };
  }, [run]);

  return { data, loading, error, refresh: run };
}
