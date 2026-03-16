// src/config/publicApi.ts
// Axios instance WITHOUT auth/redirect logic.
// Used for PUBLIC endpoints (ex: /api/pec/public/**) so that a 401 doesn't
// redirect to /login when a hospital scans a QR code.

import axios from 'axios';

/**
 * URL de base du backend pour les appels publics (page de vérification PEC).
 * Dynamique : si pas de VITE_API_BASE_URL, on utilise le même hôte que la page avec le port backend,
 * pour que le scan QR depuis mobile (ex: 192.168.22.30:5173) appelle 192.168.22.30:8085 et non localhost.
 */
export function getPublicApiBaseUrl(): string {
  const fromEnv = import.meta.env.VITE_API_BASE_URL;
  if (fromEnv && typeof fromEnv === 'string' && fromEnv.startsWith('http')) {
    return fromEnv.replace(/\/+$/, '');
  }
  if (typeof globalThis.window !== 'undefined' && globalThis.window?.location) {
    const port = import.meta.env.VITE_PUBLIC_API_PORT || '8085';
    return `${globalThis.window.location.protocol}//${globalThis.window.location.hostname}:${port}`;
  }
  return 'http://localhost:8085';
}

export const PUBLIC_API_BASE_URL = getPublicApiBaseUrl();

const publicApi = axios.create({
  baseURL: PUBLIC_API_BASE_URL,
});

export default publicApi;
