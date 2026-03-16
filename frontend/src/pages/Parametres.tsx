import React, { useEffect, useMemo, useState } from 'react';
import { twoFaService, TwoFaSetupResponse, TwoFaStatusResponse } from '../services/twoFaService';
import { ShieldCheck, QrCode, KeyRound, Loader2, RefreshCw } from 'lucide-react';
import { Skeleton } from '../components/ui/Skeleton';

/** Skeleton de la page Paramètres (sécurité 2FA) */
const ParametresSkeleton: React.FC = () => (
  <div className="max-w-3xl mx-auto p-4 sm:p-6" aria-hidden>
    <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 sm:p-6">
      <div className="flex items-start gap-3">
        <Skeleton className="h-10 w-10 rounded-xl shrink-0" />
        <div className="space-y-2 flex-1 min-w-0">
          <Skeleton className="h-6 w-48" />
          <Skeleton className="h-4 w-full max-w-sm" />
          <Skeleton className="h-4 w-36 mt-2" />
        </div>
      </div>
      <div className="mt-6 space-y-4">
        <Skeleton className="h-10 w-56 rounded-xl" />
        <div className="rounded-2xl border border-slate-200 p-4 space-y-2">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-full max-w-md" />
        </div>
      </div>
    </div>
  </div>
);

export const Parametres: React.FC = () => {
  const [statusLoading, setStatusLoading] = useState(true);
  const [loading, setLoading] = useState(false);
  const [setupData, setSetupData] = useState<TwoFaSetupResponse | null>(null);
  const [status, setStatus] = useState<TwoFaStatusResponse | null>(null);

  const [code, setCode] = useState('');
  const [message, setMessage] = useState<string>('');
  const [error, setError] = useState<string>('');

  const canSubmit = useMemo(() => code.trim().length === 6, [code]);

  const refreshStatus = () => {
    setStatusLoading(true);
    twoFaService
      .status()
      .then((st) => { if (st) setStatus(st); })
      .finally(() => setStatusLoading(false));
  };

  useEffect(() => {
    twoFaService
      .status()
      .then((st) => {
        if (st) setStatus(st);
      })
      .finally(() => setStatusLoading(false));
  }, []);

  const handleSetup = async () => {
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const data = await twoFaService.setup();
      setSetupData(data);
      setMessage('Scanne le QR code avec Google Authenticator, puis saisis le code à 6 chiffres pour activer.');
      // Essaye de rafraîchir le status si l'endpoint existe
      const st = await twoFaService.status();
      if (st) setStatus(st);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Impossible de générer le QR code.');
    } finally {
      setLoading(false);
    }
  };

  const handleEnable = async () => {
    setError('');
    setMessage('');
    setLoading(true);
    try {
      await twoFaService.verify(code);
      setMessage('✅ Google Authenticator activé avec succès.');
      setCode('');
      const st = await twoFaService.status();
      if (st) setStatus(st);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Code invalide ou activation impossible.');
    } finally {
      setLoading(false);
    }
  };

  const handleDisable = async () => {
    setError('');
    setMessage('');
    setLoading(true);
    try {
      await twoFaService.disable(code);
      setMessage('✅ Google Authenticator désactivé.');
      setCode('');
      setSetupData(null);
      const st = await twoFaService.status();
      if (st) setStatus(st);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Code invalide ou désactivation impossible.');
    } finally {
      setLoading(false);
    }
  };

  if (statusLoading) {
    return <ParametresSkeleton />;
  }

  return (
    <div className="max-w-3xl mx-auto p-4 sm:p-6">
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 p-5 sm:p-6">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3">
          <div className="flex items-start gap-3">
            <div className="p-2 rounded-xl bg-slate-50 border border-slate-200">
              <ShieldCheck className="w-6 h-6 text-slate-700" />
            </div>
            <div>
              <h2 className="text-xl font-semibold text-slate-900">Paramètres de sécurité</h2>
              <p className="text-sm text-slate-600 mt-1">
                Active Google Authenticator (TOTP) pour renforcer la connexion.
              </p>
              {status && (
                <p className="mt-2 text-xs">
                  <span className="font-semibold">État 2FA :</span>{' '}
                  <span className={status.twoFactorEnabled ? 'text-emerald-700' : 'text-slate-600'}>
                    {status.twoFactorEnabled ? 'Activé' : 'Désactivé'}
                  </span>
                </p>
              )}
            </div>
          </div>
          <button
            type="button"
            onClick={refreshStatus}
            disabled={statusLoading}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300 disabled:opacity-60 min-h-[44px] shrink-0"
            title="Actualiser l'état 2FA"
          >
            {statusLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : <RefreshCw className="w-4 h-4" />}
            Actualiser
          </button>
        </div>

        <div className="mt-6 space-y-4">
          <button
            onClick={handleSetup}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 min-h-[44px] rounded-xl bg-[#800020] text-white hover:opacity-95 disabled:opacity-60"
          >
            {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <QrCode className="w-4 h-4" />}
            Générer / Régénérer le QR code
          </button>

          {setupData && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 flex items-center justify-center">
                <img
                  src={setupData.qrCodeDataUrl}
                  alt="QR Google Authenticator"
                  className="max-w-full h-auto"
                />
              </div>

              <div className="rounded-2xl border border-slate-200 p-4">
                <p className="text-sm text-slate-700">
                  <span className="font-semibold">Secret:</span>{' '}
                  <span className="font-mono break-all">{setupData.secret}</span>
                </p>
                <p className="text-xs text-slate-500 mt-2">
                  Si tu ne peux pas scanner, tu peux entrer le secret manuellement dans Google Authenticator.
                </p>

                <div className="mt-4">
                  <label className="block text-sm font-medium text-slate-700 mb-1">
                    Code à 6 chiffres
                  </label>
                  <div className="relative">
                    <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400">
                      <KeyRound className="w-5 h-5" />
                    </span>
                    <input
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      maxLength={6}
                      className="w-full pl-10 pr-4 py-2 rounded-xl border border-slate-200 focus:outline-none focus:ring-2 focus:ring-[#800020]/30 focus:border-[#800020]"
                      placeholder="123456"
                      value={code}
                      onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    />
                  </div>

                  <div className="mt-3 flex flex-wrap gap-2">
                    <button
                      onClick={handleEnable}
                      disabled={loading || !canSubmit}
                      className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-600 text-white hover:opacity-95 disabled:opacity-60"
                    >
                      {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <ShieldCheck className="w-4 h-4" />}
                      Activer
                    </button>

                    <button
                      onClick={handleDisable}
                      disabled={loading || !canSubmit}
                      className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-slate-700 text-white hover:opacity-95 disabled:opacity-60"
                    >
                      Désactiver
                    </button>
                  </div>
                </div>
              </div>
            </div>
          )}

          {(message || error) && (
            <div
              className={`rounded-xl p-3 text-sm ${
                error ? 'bg-red-50 text-red-700 border border-red-200' : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
              }`}
            >
              {error || message}
            </div>
          )}

          <div className="rounded-2xl border border-slate-200 p-4">
            <p className="text-sm text-slate-700">
              <span className="font-semibold">Note :</span> Après activation, lors du login, l’application demandera un code
              Google Authenticator (6 chiffres).
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
