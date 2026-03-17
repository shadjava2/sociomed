// src/config/publicApi.ts
// Axios instance WITHOUT auth/redirect logic.
// Used for PUBLIC endpoints (ex: /api/pec/public/**) so that a 401 doesn't
// redirect to /login when a hospital scans a QR code.

import axios from 'axios';

/**
 * URL de base pour les appels publics (page de vérification PEC).
 * Dynamique : même origine que la page (pas de CORS). En prod derrière Caddy, /api/* est sur la même origine.
 * En dev (Vite sur 5173), on pointe vers le port backend 8085.
 */
export function getPublicApiBaseUrl(): string {
  const fromEnv = import.meta.env.VITE_API_BASE_URL;
  if (fromEnv && typeof fromEnv === 'string' && fromEnv.startsWith('http')) {
    return fromEnv.replace(/\/api\/?$/, '').replace(/\/+$/, '');
  }
  if (typeof globalThis.window !== 'undefined' && globalThis.window?.location) {
    // Même origine en prod (Caddy sert front + /api) ; en dev, backend sur 8085
    const isDev = import.meta.env.DEV || globalThis.window.location.port === '5173';
    if (isDev) {
      const port = import.meta.env.VITE_PUBLIC_API_PORT || '8085';
      return `${globalThis.window.location.protocol}//${globalThis.window.location.hostname}:${port}`;
    }
    return globalThis.window.location.origin;
  }
  return 'http://localhost:8085';
}

export const PUBLIC_API_BASE_URL = getPublicApiBaseUrl();

const publicApi = axios.create({
  baseURL: PUBLIC_API_BASE_URL,
});

export default publicApi;
