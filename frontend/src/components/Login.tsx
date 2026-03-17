// src/components/Login.tsx
import React, { useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Lock, User, Eye, EyeOff, Loader2, AlertCircle } from 'lucide-react';

const SENAT_BURGUNDY = '#800020';

export const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [totpCode, setTotpCode] = useState('');
  const [totpRequired, setTotpRequired] = useState(false);

  const [error, setError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation() as any;
  const from = location.state?.from?.pathname || '/tableau-de-bord';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await login(username, password, totpRequired ? totpCode : undefined);
      navigate(from, { replace: true });
    } catch (err: any) {
      const isNetworkError =
        !err?.response &&
        (err?.message === 'Network Error' || err?.code === 'ERR_NETWORK' || err?.message?.includes('Failed to fetch'));

      if (isNetworkError) {
        setError("Connexion au serveur impossible. Vérifiez votre connexion ou contactez l'administrateur.");
        setIsLoading(false);
        return;
      }

      const data = err?.response?.data;
      const msg: string = data?.message || data?.error || 'Erreur de connexion';

      const twoFactorRequired =
        data?.twoFactorRequired === true ||
        data?.code === 'TOTP_REQUIRED' ||
        /otp\s+requis/i.test(msg) ||
        /code\s+google\s+authenticator\s+requis/i.test(msg);

      if (twoFactorRequired) {
        setTotpRequired(true);
        setError('Code Google Authenticator requis.');
        return;
      }

      if (/otp\s+invalide/i.test(msg) || /invalide\s+ou\s+manquant/i.test(msg)) {
        setTotpRequired(true);
      }

      setError(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div
      className="min-h-screen flex items-center justify-center p-2 sm:p-4 overflow-y-auto"
      style={{
        paddingTop: 'max(0.5rem, env(safe-area-inset-top))',
        paddingBottom: 'max(0.5rem, env(safe-area-inset-bottom))',
        paddingLeft: 'max(0.5rem, env(safe-area-inset-left))',
        paddingRight: 'max(0.5rem, env(safe-area-inset-right))',
        background: `linear-gradient(165deg, #1e293b 0%, #334155 35%, #475569 70%, #64748b 100%)`,
        position: 'relative',
      }}
    >
      {/* Motif institutionnel discret */}
      <div
        className="absolute inset-0 opacity-[0.03] pointer-events-none"
        style={{
          backgroundImage: `repeating-linear-gradient(
            0deg,
            transparent,
            transparent 2px,
            rgba(255,255,255,0.5) 2px,
            rgba(255,255,255,0.5) 4px
          )`,
        }}
        aria-hidden
      />

      {/* Bandeau supérieur institutionnel */}
      <div
        className="absolute top-0 left-0 right-0 h-1 opacity-90"
        style={{ backgroundColor: SENAT_BURGUNDY }}
        aria-hidden
      />

      <main className="relative z-10 w-full max-w-md my-auto flex flex-col max-h-[calc(100vh-1rem)]">
        <div
          className="bg-white rounded-lg shadow-2xl border border-slate-200/80 flex flex-col min-h-0 shrink-0"
          style={{ boxShadow: '0 25px 50px -12px rgba(0,0,0,0.35), 0 0 0 1px rgba(255,255,255,0.05) inset' }}
        >
          {/* En-tête compact */}
          <div
            className="px-4 sm:px-6 py-3 sm:py-4 text-center shrink-0"
            style={{ backgroundColor: SENAT_BURGUNDY }}
          >
            <img
              src="/assets/senat-logo.png"
              alt=""
              role="presentation"
              className="h-10 w-10 sm:h-12 sm:w-12 rounded-full object-contain ring-2 ring-white/30 mx-auto"
              loading="eager"
              width={48}
              height={48}
            />
            <h1 className="mt-2 text-base sm:text-lg font-bold text-white tracking-wide uppercase">
              Sénat
            </h1>
            <p className="mt-0.5 text-xs sm:text-sm text-white/90 font-medium tracking-widest uppercase">
              Prise en charge médicale
            </p>
          </div>

          <div className="px-4 sm:px-6 py-3 sm:py-4 overflow-y-auto min-h-0">
            <form onSubmit={handleSubmit} className="space-y-3">
              {error && (
                <div
                  className="flex gap-2 p-3 rounded-lg border border-red-200 bg-red-50 text-red-800 text-xs sm:text-sm"
                  role="alert"
                >
                  <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-red-600" />
                  <span>{error}</span>
                </div>
              )}

              <div>
                <label htmlFor="login-username" className="block text-xs sm:text-sm font-semibold text-slate-700 mb-1">
                  Nom d'utilisateur
                </label>
                <div className="relative">
                  <User className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4 sm:w-5 sm:h-5 pointer-events-none" />
                  <input
                    id="login-username"
                    type="text"
                    value={username}
                    onChange={(e) => {
                      setUsername(e.target.value);
                      setTotpRequired(false);
                      setTotpCode('');
                    }}
                    className="w-full pl-8 sm:pl-10 pr-3 py-2 sm:py-2.5 text-sm sm:text-base border border-slate-300 rounded-lg focus:ring-2 focus:ring-[#800020]/30 focus:ring-offset-0 focus:border-[#800020]"
                    placeholder="Nom d'utilisateur"
                    required
                    autoComplete="username"
                    disabled={isLoading}
                  />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label htmlFor="login-password" className="block text-xs sm:text-sm font-semibold text-slate-700">
                    Mot de passe
                  </label>
                  <Link
                    to="/mot-de-passe-oublie"
                    className="text-xs font-medium hover:underline"
                    style={{ color: SENAT_BURGUNDY }}
                  >
                    Mot de passe oublié ?
                  </Link>
                </div>
                <div className="relative">
                  <Lock className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4 sm:w-5 sm:h-5 pointer-events-none" />
                  <input
                    id="login-password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      setTotpRequired(false);
                      setTotpCode('');
                    }}
                    className="w-full pl-8 sm:pl-10 pr-10 py-2 sm:py-2.5 text-sm sm:text-base border border-slate-300 rounded-lg focus:ring-2 focus:ring-[#800020]/30 focus:ring-offset-0 focus:border-[#800020]"
                    placeholder="Mot de passe"
                    required
                    autoComplete="current-password"
                    disabled={isLoading}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded-md text-slate-400 hover:text-slate-600 hover:bg-slate-100"
                    title={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
                    aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4 sm:w-5 sm:h-5" /> : <Eye className="w-4 h-4 sm:w-5 sm:h-5" />}
                  </button>
                </div>
              </div>

              {totpRequired && (
                <div className="space-y-1">
                  <label htmlFor="login-totp" className="block text-xs sm:text-sm font-semibold text-slate-700">
                    Code Authenticator
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400 w-4 h-4 pointer-events-none" />
                    <input
                      id="login-totp"
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      maxLength={6}
                      value={totpCode}
                      onChange={(e) =>
                        setTotpCode(e.target.value.replaceAll(/\D/g, '').slice(0, 6))
                      }
                      className="w-full pl-8 pr-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-[#800020]/30 focus:ring-offset-0 focus:border-[#800020]"
                      placeholder="123456"
                      required
                      autoComplete="one-time-code"
                      disabled={isLoading}
                    />
                  </div>
                  <p className="text-xs text-slate-500">Code à 6 chiffres (Google Authenticator)</p>
                </div>
              )}

              <button
                type="submit"
                disabled={isLoading}
                className="w-full py-2.5 sm:py-3 min-h-[44px] rounded-lg font-semibold text-sm sm:text-base text-white transition-colors disabled:opacity-60 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2 focus:outline-none focus:ring-2 focus:ring-offset-2 bg-[#800020] hover:bg-[#650018] active:bg-[#4d0012] focus:ring-[#800020]"
              >
                {isLoading ? (
                  <>
                    <Loader2 className="w-5 h-5 shrink-0 animate-spin" aria-hidden />
                    <span>Connexion en cours…</span>
                  </>
                ) : (
                  'Se connecter'
                )}
              </button>
            </form>

            <footer className="mt-3 pt-3 border-t border-slate-200 text-center text-xs text-slate-500">
              © {new Date().getFullYear()} Sénat – Plateforme Prise en charge médicale
              <br />
              Développé par Otshudiakoy
            </footer>
          </div>
        </div>
      </main>
    </div>
  );
}
