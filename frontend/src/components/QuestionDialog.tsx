import React, { useEffect, useRef, useState } from 'react';
import { AlertTriangle, HelpCircle, Loader2 } from 'lucide-react';

export type QuestionDialogVariant = 'danger' | 'default' | 'warning';

export type QuestionDialogProps = {
  open: boolean;
  title: string;
  message: string;
  variant?: QuestionDialogVariant;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void | Promise<void>;
  onCancel: () => void;
  loading?: boolean;
};

const variantStyles = {
  danger: {
    icon: AlertTriangle,
    iconBg: 'bg-red-100',
    iconColor: 'text-red-600',
    buttonClass: 'bg-red-600 hover:bg-red-700 focus:ring-red-500',
  },
  warning: {
    icon: AlertTriangle,
    iconBg: 'bg-amber-100',
    iconColor: 'text-amber-600',
    buttonClass: 'bg-amber-600 hover:bg-amber-700 focus:ring-amber-500',
  },
  default: {
    icon: HelpCircle,
    iconBg: 'bg-blue-100',
    iconColor: 'text-blue-600',
    buttonClass: 'bg-blue-600 hover:bg-blue-700 focus:ring-blue-500',
  },
};

export const QuestionDialog: React.FC<QuestionDialogProps> = ({
  open,
  title,
  message,
  variant = 'default',
  confirmLabel,
  cancelLabel = 'Annuler',
  onConfirm,
  onCancel,
  loading = false,
}) => {
  const overlayRef = useRef<HTMLDivElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);

  const style = variantStyles[variant];
  const Icon = style.icon;

  const defaultConfirmLabel = variant === 'danger' ? 'Supprimer' : 'OK';

  useEffect(() => {
    if (!open) return;
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onCancel();
    };
    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [open, onCancel]);

  useEffect(() => {
    if (open) cancelRef.current?.focus();
  }, [open]);

  const handleOverlayClick = (e: React.MouseEvent) => {
    if (e.target === overlayRef.current) onCancel();
  };

  const [busy, setBusy] = useState(false);
  const isBusy = loading === true || busy;

  const handleConfirm = async () => {
    setBusy(true);
    try {
      await Promise.resolve(onConfirm());
      onCancel();
    } finally {
      setBusy(false);
    }
  };

  if (!open) return null;

  const handleOverlayKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') onCancel();
  };

  return (
    <div
      ref={overlayRef}
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      onClick={handleOverlayClick}
      onKeyDown={handleOverlayKeyDown}
      role="dialog"
      aria-modal="true"
      aria-labelledby="question-dialog-title"
      aria-describedby="question-dialog-message"
    >
      <div
        className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden border border-slate-200"
        onClick={(e) => e.stopPropagation()}
        onKeyDown={(e) => e.stopPropagation()}
      >
        <div className="p-6">
          <div className="flex items-start gap-4">
            <div
              className={`flex-shrink-0 w-12 h-12 rounded-full flex items-center justify-center ${style.iconBg} ${style.iconColor}`}
              aria-hidden
            >
              <Icon className="w-6 h-6" />
            </div>
            <div className="flex-1 min-w-0">
              <h2
                id="question-dialog-title"
                className="text-xl font-bold text-slate-800 mb-2"
              >
                {title}
              </h2>
              <p id="question-dialog-message" className="text-slate-600 leading-relaxed">
                {message}
              </p>
            </div>
          </div>

          <div className="flex justify-end gap-3 mt-6 pt-6 border-t border-slate-200">
            <button
              ref={cancelRef}
              type="button"
              onClick={onCancel}
              disabled={isBusy}
              className="px-5 py-2.5 border border-slate-300 text-slate-700 font-medium rounded-lg hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-slate-400 transition-colors disabled:opacity-50"
            >
              {cancelLabel}
            </button>
            <button
              type="button"
              onClick={handleConfirm}
              disabled={isBusy}
              className={`inline-flex items-center justify-center gap-2 px-5 py-2.5 text-white font-medium rounded-lg focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors disabled:opacity-50 ${style.buttonClass}`}
            >
              {isBusy ? (
                <>
                  <Loader2 className="w-5 h-5 shrink-0 animate-spin" aria-hidden />
                  <span>En cours…</span>
                </>
              ) : (
                confirmLabel ?? defaultConfirmLabel
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
