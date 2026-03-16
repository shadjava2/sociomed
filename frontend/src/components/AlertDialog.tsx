// src/components/AlertDialog.tsx
import React, { useEffect, useRef } from 'react';
import { AlertCircle, CheckCircle, Info } from 'lucide-react';

export type AlertDialogVariant = 'error' | 'success' | 'info';

export type AlertDialogProps = {
  open: boolean;
  title: string;
  message: string;
  variant?: AlertDialogVariant;
  okLabel?: string;
  onClose: () => void;
};

const variantStyles = {
  error: {
    icon: AlertCircle,
    iconBg: 'bg-red-100',
    iconColor: 'text-red-600',
    buttonClass: 'bg-red-600 hover:bg-red-700 focus:ring-red-500 text-white',
  },
  success: {
    icon: CheckCircle,
    iconBg: 'bg-emerald-100',
    iconColor: 'text-emerald-600',
    buttonClass: 'bg-emerald-600 hover:bg-emerald-700 focus:ring-emerald-500 text-white',
  },
  info: {
    icon: Info,
    iconBg: 'bg-blue-100',
    iconColor: 'text-blue-600',
    buttonClass: 'bg-[#800020] hover:bg-[#6b001b] focus:ring-[#800020] text-white',
  },
};

export const AlertDialog: React.FC<AlertDialogProps> = ({
  open,
  title,
  message,
  variant = 'info',
  okLabel = 'OK',
  onClose,
}) => {
  const overlayRef = useRef<HTMLDivElement>(null);
  const okRef = useRef<HTMLButtonElement>(null);

  const style = variantStyles[variant];
  const Icon = style.icon;

  useEffect(() => {
    if (!open) return;
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [open, onClose]);

  useEffect(() => {
    if (open) okRef.current?.focus();
  }, [open]);

  const handleOverlayClick = (e: React.MouseEvent) => {
    if (e.target === overlayRef.current) onClose();
  };

  if (!open) return null;

  return (
    <div
      ref={overlayRef}
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm"
      onClick={handleOverlayClick}
      onKeyDown={(e) => e.key === 'Escape' && onClose()}
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="alert-dialog-title"
      aria-describedby="alert-dialog-message"
    >
      <div
        className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden border border-slate-200"
        onClick={(e) => e.stopPropagation()}
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
                id="alert-dialog-title"
                className="text-xl font-bold text-slate-800 mb-2"
              >
                {title}
              </h2>
              <p id="alert-dialog-message" className="text-slate-600 leading-relaxed">
                {message}
              </p>
            </div>
          </div>

          <div className="flex justify-end mt-6 pt-6 border-t border-slate-200">
            <button
              ref={okRef}
              type="button"
              onClick={onClose}
              className={`px-5 py-2.5 font-medium rounded-xl focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors ${style.buttonClass}`}
            >
              {okLabel}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
