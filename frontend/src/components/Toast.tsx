// src/components/Toast.tsx
import React, { useEffect, useState, useCallback, createContext, useContext } from 'react';
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react';
import { setToastHandler, getErrorMessage } from '../lib/toastApi';

export type ToastItem = {
  id: string;
  type: 'success' | 'error' | 'info';
  message: string;
  title?: string;
};

const TOAST_DURATION_MS = 5000;

const ToastIcon = ({ type }: { type: ToastItem['type'] }) => {
  switch (type) {
    case 'success':
      return <CheckCircle className="w-5 h-5 text-emerald-500 flex-shrink-0" />;
    case 'error':
      return <AlertCircle className="w-5 h-5 text-red-500 flex-shrink-0" />;
    default:
      return <Info className="w-5 h-5 text-blue-500 flex-shrink-0" />;
  }
};

const ToastOne: React.FC<{
  item: ToastItem;
  onDismiss: (id: string) => void;
}> = ({ item, onDismiss }) => {
  useEffect(() => {
    const t = setTimeout(() => onDismiss(item.id), TOAST_DURATION_MS);
    return () => clearTimeout(t);
  }, [item.id, onDismiss]);

  const bg =
    item.type === 'success'
      ? 'bg-white border-emerald-200'
      : item.type === 'error'
        ? 'bg-white border-red-200'
        : 'bg-white border-slate-200';

  return (
    <div
      className={`flex items-start gap-3 px-4 py-3 rounded-xl border shadow-lg ${bg}`}
      role="alert"
    >
      <ToastIcon type={item.type} />
      <div className="flex-1 min-w-0">
        {item.title && (
          <p className="font-semibold text-slate-800 text-sm">{item.title}</p>
        )}
        <p className={item.title ? 'text-slate-600 text-sm mt-0.5' : 'text-slate-700 text-sm'}>
          {item.message}
        </p>
      </div>
      <button
        type="button"
        onClick={() => onDismiss(item.id)}
        className="p-1 rounded-lg hover:bg-slate-100 text-slate-500"
        aria-label="Fermer"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};

type ToastContextType = {
  toasts: ToastItem[];
  addToast: (item: Omit<ToastItem, 'id'>) => void;
  removeToast: (id: string) => void;
};

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const addToast = useCallback((item: Omit<ToastItem, 'id'>) => {
    const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    setToasts((prev) => [...prev, { ...item, id }]);
  }, []);

  useEffect(() => {
    setToastHandler({
      error: (message, title) => addToast({ type: 'error', message, title: title ?? 'Erreur' }),
      success: (message, title) => addToast({ type: 'success', message, title: title ?? 'Succès' }),
      info: (message, title) => addToast({ type: 'info', message, title: title ?? 'Information' }),
    });
    return () => setToastHandler(null);
  }, [addToast]);

  return (
    <ToastContext.Provider value={{ toasts, addToast, removeToast }}>
      {children}
      <div
        className="fixed bottom-4 right-4 z-[110] flex flex-col gap-2 max-w-sm w-full pointer-events-none"
        aria-live="polite"
      >
        <div className="flex flex-col gap-2 pointer-events-auto">
          {toasts.map((t) => (
            <ToastOne key={t.id} item={t} onDismiss={removeToast} />
          ))}
        </div>
      </div>
    </ToastContext.Provider>
  );
};

export const useToast = () => {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
};

export { getErrorMessage };
