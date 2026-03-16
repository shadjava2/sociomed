// src/config/api.ts
import axios, { type InternalAxiosRequestConfig } from 'axios';
import { toast, getErrorMessage } from '../lib/toastApi';

const STORAGE_KEY_API_BASE = 'pec_api_base_url';
const DEFAULT_TIMEOUT = 15000;
const RETRY_COUNT = 2;
const RETRY_DELAY_MS = 1000;

/** URL de l’API : .env > param ?api= > sessionStorage > localhost (pour accès mobile via tunnel) */
function getApiBaseUrl(): string {
  const fromEnv = import.meta.env.VITE_API_BASE_URL;
  if (fromEnv && fromEnv.startsWith('http')) return fromEnv;

  if (typeof window !== 'undefined') {
    const params = new URLSearchParams(window.location.search);
    const fromQuery = params.get('api');
    if (fromQuery && fromQuery.startsWith('http')) {
      try {
        sessionStorage.setItem(STORAGE_KEY_API_BASE, fromQuery.replace(/\/$/, ''));
      } catch {
        /* ignore */
      }
      return fromQuery.replace(/\/$/, '');
    }
    const fromStorage = sessionStorage.getItem(STORAGE_KEY_API_BASE);
    if (fromStorage) return fromStorage;
  }

  return 'http://localhost:8085';
}

const API_BASE_URL = getApiBaseUrl();

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: DEFAULT_TIMEOUT,
});

// ✅ Récupération token compatible (certains écrans stockent sous token/accessToken)
function getToken(): string | null {
  return (
    localStorage.getItem('jwt_token') ||
    localStorage.getItem('token') ||
    localStorage.getItem('accessToken')
  );
}

// ------------ Intercepteur requête : ajoute systématiquement le JWT ------------
api.interceptors.request.use(
  (config) => {
    const token = getToken();

    if (token) {
      // Axios v1 compat
      // @ts-ignore
      if (config.headers?.set) {
        // @ts-ignore
        config.headers.set('Authorization', `Bearer ${token}`);
      } else {
        config.headers = config.headers || {};
        // @ts-ignore
        config.headers['Authorization'] = `Bearer ${token}`;
      }
    }

    // 👇 IMPORTANT : si on envoie un FormData, on enlève tout Content-Type,
    // le navigateur mettra "multipart/form-data; boundary=..."
    const isFormData =
      typeof FormData !== 'undefined' && config.data instanceof FormData;

    if (isFormData) {
      // Axios v1 compat
      // @ts-ignore
      if (config.headers?.delete) {
        // @ts-ignore
        config.headers.delete('Content-Type');
      } else if (config.headers) {
        // @ts-ignore
        delete config.headers['Content-Type'];
      }
    } else {
      // Sinon, on peut laisser Axios inférer "application/json" tout seul
      config.headers = config.headers || {};
      // @ts-ignore
      if (!config.headers['Content-Type']) {
        // @ts-ignore
        config.headers['Content-Type'] = 'application/json';
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// ------------ Intercepteur réponse : gestion 401/403 ------------
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const status = error?.response?.status;
    const method = (error?.config?.method || 'get').toLowerCase();
    const url = String(error?.config?.url || '');
    const onLoginPage = (window.location?.pathname || '').startsWith('/login');

    const isAuthEndpoint =
      url.includes('/api/auth/signin') || url.includes('/api/auth/signout');

    // ✅ IMPORTANT : ne pas rediriger sur /login pour les endpoints 2FA,
    // sinon un 401 pendant setup/verify te "déconnecte" et tu ne vois jamais l’erreur.
    const isTwoFaEndpoint = url.includes('/api/2fa/');

    if (error?.code === 'ECONNABORTED' || error?.message?.includes('timeout')) {
      error.message = 'Délai dépassé. Vérifiez votre connexion.';
    }
    const isGet = method === 'get';
    const retries = (error?.config?.__retryCount as number) ?? 0;
    if (
      isGet &&
      retries < RETRY_COUNT &&
      (!error?.response || error?.response?.status >= 500 || error?.code === 'ECONNABORTED' || error?.code === 'ERR_NETWORK')
    ) {
      (error.config as InternalAxiosRequestConfig & { __retryCount?: number }).__retryCount = retries + 1;
      await new Promise((r) => setTimeout(r, RETRY_DELAY_MS));
      return api.request(error.config);
    }

    if (
      (status === 401 || status === 403) &&
      !isAuthEndpoint &&
      !isTwoFaEndpoint &&
      !onLoginPage
    ) {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('token');
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }

    // Toast pour toute erreur API (sauf redirection login) afin que l'utilisateur soit informé
    if (!isAuthEndpoint) {
      toast.error(getErrorMessage(error), 'Erreur');
    }

    return Promise.reject(error);
  }
);

export { API_BASE_URL };
export default api;
