// src/contexts/AppDialogContext.tsx
import React, { createContext, useContext, useState, useCallback, useRef } from 'react';
import { AlertDialog, AlertDialogVariant } from '../components/AlertDialog';
import { QuestionDialog } from '../components/QuestionDialog';

type AlertState = {
  open: boolean;
  title: string;
  message: string;
  variant: AlertDialogVariant;
};

type ConfirmState = {
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
};

type AppDialogContextType = {
  showError: (message: string, title?: string) => void;
  showSuccess: (message: string, title?: string) => void;
  showInfo: (message: string, title?: string) => void;
  showConfirm: (options: {
    title: string;
    message: string;
    confirmLabel?: string;
    cancelLabel?: string;
  }) => Promise<boolean>;
};

const defaultAlert: AlertState = {
  open: false,
  title: '',
  message: '',
  variant: 'info',
};

const defaultConfirm: ConfirmState = {
  open: false,
  title: '',
  message: '',
};

const AppDialogContext = createContext<AppDialogContextType | undefined>(undefined);

export const AppDialogProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [alert, setAlert] = useState<AlertState>(defaultAlert);
  const [confirm, setConfirm] = useState<ConfirmState>(defaultConfirm);
  const resolveRef = useRef<(value: boolean) => void>(() => {});

  const closeAlert = useCallback(() => {
    setAlert((a) => ({ ...a, open: false }));
  }, []);

  const showError = useCallback((message: string, title = 'Erreur') => {
    setAlert({ open: true, title, message, variant: 'error' });
  }, []);

  const showSuccess = useCallback((message: string, title = 'Succès') => {
    setAlert({ open: true, title, message, variant: 'success' });
  }, []);

  const showInfo = useCallback((message: string, title = 'Information') => {
    setAlert({ open: true, title, message, variant: 'info' });
  }, []);

  const showConfirm = useCallback(
    (options: {
      title: string;
      message: string;
      confirmLabel?: string;
      cancelLabel?: string;
    }) => {
      return new Promise<boolean>((resolve) => {
        resolveRef.current = resolve;
        setConfirm({
          open: true,
          title: options.title,
          message: options.message,
          confirmLabel: options.confirmLabel,
          cancelLabel: options.cancelLabel,
        });
      });
    },
    []
  );

  const handleConfirmOk = useCallback(() => {
    resolveRef.current(true);
    resolveRef.current = () => {};
    setConfirm(defaultConfirm);
  }, []);

  const handleConfirmCancel = useCallback(() => {
    resolveRef.current(false);
    resolveRef.current = () => {};
    setConfirm(defaultConfirm);
  }, []);

  return (
    <AppDialogContext.Provider value={{ showError, showSuccess, showInfo, showConfirm }}>
      {children}
      <AlertDialog
        open={alert.open}
        title={alert.title}
        message={alert.message}
        variant={alert.variant}
        onClose={closeAlert}
      />
      <QuestionDialog
        open={confirm.open}
        title={confirm.title}
        message={confirm.message}
        variant="default"
        confirmLabel={confirm.confirmLabel ?? 'Oui'}
        cancelLabel={confirm.cancelLabel ?? 'Non'}
        onConfirm={handleConfirmOk}
        onCancel={handleConfirmCancel}
      />
    </AppDialogContext.Provider>
  );
};

export const useAppDialog = () => {
  const ctx = useContext(AppDialogContext);
  if (!ctx) {
    throw new Error('useAppDialog must be used within AppDialogProvider');
  }
  return ctx;
};
