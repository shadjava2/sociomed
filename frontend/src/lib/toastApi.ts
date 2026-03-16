/**
 * API impérative pour les toasts (utilisable hors React, ex. intercepteur axios).
 * Le ToastProvider enregistre les implémentations au montage.
 */

type ToastHandler = {
  error: (message: string, title?: string) => void;
  success: (message: string, title?: string) => void;
  info: (message: string, title?: string) => void;
};

let handler: ToastHandler | null = null;

export function setToastHandler(h: ToastHandler | null) {
  handler = h;
}

export const toast = {
  error: (message: string, title?: string) => handler?.error(message, title),
  success: (message: string, title?: string) => handler?.success(message, title),
  info: (message: string, title?: string) => handler?.info(message, title),
};

export function getErrorMessage(err: any): string {
  const msg =
    err?.response?.data?.error ??
    err?.response?.data?.message ??
    err?.message;
  return typeof msg === 'string' && msg.length > 0 ? msg : 'Erreur serveur';
}
