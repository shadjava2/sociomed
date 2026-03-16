// src/pages/DashboardPec.tsx
import React, { useEffect, useState } from 'react';
import { pecService, LabelCount } from '../services/pecService';
import { Building2, RefreshCw, Loader2 } from 'lucide-react';
import { Skeleton } from '../components/ui/Skeleton';

function moisCourantFR(d = new Date()) {
  return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(d);
}

/** Skeleton du tableau de bord : même structure que le contenu réel */
const DashboardPecSkeleton: React.FC = () => (
  <div className="space-y-5" aria-hidden>
    <div className="flex items-center justify-between">
      <div className="space-y-2">
        <Skeleton className="h-6 w-72" />
        <Skeleton className="h-4 w-48" />
      </div>
      <Skeleton className="h-10 w-24 rounded-lg" />
    </div>
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      {[1, 2, 3, 4, 5, 6].map((id) => (
        <div
          key={id}
          className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm flex items-center justify-between"
        >
          <div className="flex items-center gap-3">
            <Skeleton className="h-10 w-10 rounded-lg shrink-0" />
            <div className="space-y-2">
              <Skeleton className="h-3 w-16" />
              <Skeleton className="h-5 w-32" />
            </div>
          </div>
          <Skeleton className="h-8 w-12 rounded" />
        </div>
      ))}
    </div>
  </div>
);

export const DashboardPec: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState<LabelCount[]>([]);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await pecService.statsByHopital();
      setStats(data);
    } catch (e: any) {
      setError(e?.message ?? 'Erreur de chargement');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  if (loading && stats.length === 0) {
    return <DashboardPecSkeleton />;
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold text-slate-800">
            Tableau de bord — Prises en charge
          </h2>
          <p className="text-slate-600 text-sm sm:text-base">Mois en cours : {moisCourantFR()}</p>
        </div>
        <button
          type="button"
          onClick={load}
          disabled={loading}
          className="inline-flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300 min-h-[44px] sm:min-h-0 w-full sm:w-auto disabled:opacity-70 disabled:cursor-not-allowed"
          title="Actualiser les données"
        >
          {loading ? <Loader2 className="w-4 h-4 animate-spin" aria-hidden /> : <RefreshCw className="w-4 h-4" />}
          Actualiser
        </button>
      </div>

      {error && (
        <div className="p-3 rounded-lg border border-red-200 bg-red-50 text-red-700">
          {error}
        </div>
      )}

      {!error && (
        <>
          {stats.length === 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              <div className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm flex items-center justify-center min-h-[120px] col-span-full sm:col-span-2 lg:col-span-3">
                <div className="flex items-center gap-3 text-slate-500">
                  <div className="p-2 rounded-lg bg-slate-100">
                    <Building2 className="w-6 h-6 text-slate-400" />
                  </div>
                  <p className="text-sm sm:text-base">
                    Aucune prise en charge trouvée pour le mois en cours.
                  </p>
                </div>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
              {stats.map((it) => (
                <div
                  key={it.label}
                  className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-lg bg-[#800020]/10">
                      <Building2 className="w-5 h-5 text-[#800020]" />
                    </div>
                    <div>
                      <div className="text-sm text-slate-500">Hôpital</div>
                      <div className="text-base font-semibold text-slate-800">
                        {it.label || '—'}
                      </div>
                    </div>
                  </div>
                  <div className="text-3xl font-bold text-[#800020] tabular-nums">
                    {it.value ?? 0}
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};
