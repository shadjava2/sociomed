// Invitation à installer la PWA (téléphone ou machine) — dès l'ouverture si le navigateur le permet.
import React, { useEffect, useState } from 'react';
import { Download, X } from 'lucide-react';

const STORAGE_KEY_DISMISSED = 'pec_pwa_install_dismissed';
const DISMISS_DAYS = 7;

function wasDismissedRecently(): boolean {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_DISMISSED);
    if (!raw) return false;
    const t = Number(raw);
    if (Number.isNaN(t)) return false;
    return Date.now() - t < DISMISS_DAYS * 24 * 60 * 60 * 1000;
  } catch {
    return false;
  }
}

function setDismissed(): void {
  try {
    localStorage.setItem(STORAGE_KEY_DISMISSED, String(Date.now()));
  } catch {
    /* ignore */
  }
}

function isStandalone(): boolean {
  if (typeof window === 'undefined') return false;
  return (
    (window as Window & { standalone?: boolean }).standalone === true ||
    window.matchMedia('(display-mode: standalone)').matches
  );
}

export const InstallPWAPrompt: React.FC = () => {
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null);
  const [visible, setVisible] = useState(false);
  const [installing, setInstalling] = useState(false);

  useEffect(() => {
    if (isStandalone() || wasDismissedRecently()) return;

    const handler = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e as BeforeInstallPromptEvent);
      setVisible(true);
    };

    window.addEventListener('beforeinstallprompt', handler);
    return () => window.removeEventListener('beforeinstallprompt', handler);
  }, []);

  const handleInstall = async () => {
    if (!deferredPrompt) return;
    setInstalling(true);
    try {
      await deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      if (outcome === 'accepted') {
        setVisible(false);
        setDeferredPrompt(null);
      }
    } finally {
      setInstalling(false);
    }
  };

  const handleDismiss = () => {
    setDismissed();
    setVisible(false);
    setDeferredPrompt(null);
  };

  if (!visible || !deferredPrompt) return null;

  return (
    <div
      className="fixed bottom-4 left-4 right-4 z-[100] max-w-md mx-auto rounded-xl shadow-2xl border border-slate-200 bg-white p-4 flex items-center gap-4"
      role="dialog"
      aria-labelledby="pwa-install-title"
      style={{
        paddingBottom: 'max(1rem, env(safe-area-inset-bottom))',
      }}
    >
      <div className="flex-1 min-w-0">
        <h3 id="pwa-install-title" className="font-semibold text-slate-800 text-sm">
          Installer l'application
        </h3>
        <p className="text-slate-600 text-xs mt-0.5">
          Téléphone ou ordinateur : accès rapide, utilisation hors ligne.
        </p>
      </div>
      <div className="flex items-center gap-2 shrink-0">
        <button
          type="button"
          onClick={handleInstall}
          disabled={installing}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-[#800020] text-white text-sm font-medium hover:bg-[#600018] disabled:opacity-70 transition-colors"
        >
          <Download className="w-4 h-4" />
          {installing ? 'Installation…' : 'Installer'}
        </button>
        <button
          type="button"
          onClick={handleDismiss}
          className="p-2 rounded-lg text-slate-500 hover:bg-slate-100 transition-colors"
          aria-label="Fermer"
        >
          <X className="w-5 h-5" />
        </button>
      </div>
    </div>
  );
};

// Type pour l'événement beforeinstallprompt (n’existe pas dans les types DOM par défaut)
interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}
