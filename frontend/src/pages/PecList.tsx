import React, { useEffect, useMemo, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useAppDialog } from '../contexts/AppDialogContext';
import { pecService, PecListRow, PageResponse } from '../services/pecService';
import { hopitauxService, HopitalLite } from '../services/hopitauxService';
import {
  Loader2,
  Printer,
  Search,
  RefreshCw,
  Stethoscope,
  Trash2,
} from 'lucide-react';
import { Skeleton, SkeletonTableRow } from '../components/ui/Skeleton';
import { QuestionDialog } from '../components/QuestionDialog';

function formatDate(d?: string | Date | null) {
  if (!d) return '';
  const date = typeof d === 'string' ? new Date(d) : d;
  return new Intl.DateTimeFormat('fr-FR').format(date);
}

/** Skeleton de la page Prises en charge */
const PecListSkeleton: React.FC = () => (
  <div className="space-y-4" aria-hidden>
    <div className="flex items-center justify-between">
      <div className="flex items-center gap-3">
        <Skeleton className="h-6 w-6 rounded" />
        <Skeleton className="h-6 w-40" />
      </div>
      <div className="flex items-center gap-2">
        <Skeleton className="h-10 w-36 rounded-lg" />
        <Skeleton className="h-10 w-10 rounded-lg" />
      </div>
    </div>

    <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
      {[1, 2, 3, 4].map((i) => (
        <div key={i}>
          <Skeleton className="h-4 w-16 mb-1" />
          <Skeleton className="h-10 w-full rounded-lg" />
        </div>
      ))}
    </div>

    <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <thead className="bg-slate-50 text-slate-600">
            <tr>
              {['N°', 'Date', 'Hôpital', 'Bénéficiaire', 'Qualité', 'Actions'].map((label) => (
                <th key={label} className="text-left px-4 py-3 text-sm font-medium text-slate-500">
                  {label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: 10 }).map((_, i) => (
              <SkeletonTableRow key={i} cols={6} />
            ))}
          </tbody>
        </table>
      </div>
      <div className="flex items-center justify-between px-4 py-3 bg-slate-50 border-t border-slate-200">
        <Skeleton className="h-4 w-32" />
        <div className="flex gap-2">
          <Skeleton className="h-8 w-24 rounded-lg" />
          <Skeleton className="h-8 w-24 rounded-lg" />
        </div>
      </div>
    </div>
  </div>
);

export const PecList: React.FC = () => {
  const { hasPermission } = useAuth();
  const { showError } = useAppDialog();
  const [loading, setLoading] = useState(true);
  const [printingListing, setPrintingListing] = useState(false);
  const [printingPecId, setPrintingPecId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [deleteConfirmRow, setDeleteConfirmRow] = useState<PecListRow | null>(null);
  const [hopitaux, setHopitaux] = useState<HopitalLite[]>([]);
  const [hopitalId, setHopitalId] = useState<number | ''>('');
  const [month, setMonth] = useState<string>(''); // YYYY-MM
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [search, setSearch] = useState('');
  const [data, setData] = useState<PageResponse<PecListRow> | null>(null);

  const fetchHopitaux = async () => {
    try {
      const list = await hopitauxService.listLite();
      setHopitaux(list);
    } catch (e) {
      console.error('Erreur chargement hôpitaux', e);
    }
  };

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await pecService.listByHopital({
        hopitalId: hopitalId ? Number(hopitalId) : undefined,
        month: month || undefined,
        page,
        size,
      });
      setData(res);
    } catch (e) {
      console.error('Erreur chargement PEC', e);
      showError("Impossible de charger la liste des PEC.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHopitaux();
  }, []);

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hopitalId, month, page, size]);

  const filtered = useMemo(() => {
    const rows = data?.content ?? [];
    if (!search.trim()) return rows;

    const q = search.toLowerCase();
    return rows.filter(
      (r) =>
        (r.numero ?? '').toLowerCase().includes(q) ||
        (r.beneficiaireNom ?? '').toLowerCase().includes(q) ||
        (r.hopitalNom ?? '').toLowerCase().includes(q) ||
        (r.beneficiaireQualite ?? '').toLowerCase().includes(q)
    );
  }, [data, search]);

  const totalPages = data?.totalPages ?? 0;

  const handlePrintListing = async () => {
    try {
      setPrintingListing(true);
      await pecService.printListing({
        hopitalId: hopitalId ? Number(hopitalId) : undefined,
        month: month || undefined,
        limit: 5000,
      });
    } catch (e) {
      console.error('Erreur impression liste PEC', e);
      showError("Impossible d'imprimer la liste.");
    } finally {
      setPrintingListing(false);
    }
  };

  const handlePrintPec = async (pecId: number) => {
    try {
      setPrintingPecId(pecId);
      await pecService.print(pecId);
    } catch (e) {
      console.error('Erreur impression PEC', e);
      showError("Impossible d'ouvrir le PDF.");
    } finally {
      setPrintingPecId(null);
    }
  };

  const performDeletePec = async (row: PecListRow) => {
    try {
      setDeletingId(row.id);
      await pecService.remove(row.id);
      await fetchData();
      setDeleteConfirmRow(null);
    } catch (e) {
      console.error('Erreur suppression PEC', e);
      showError('Suppression impossible. Veuillez réessayer.');
    } finally {
      setDeletingId(null);
    }
  };

  if (loading && data === null) {
    return <PecListSkeleton />;
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="flex items-center gap-3">
          <Stethoscope className="w-6 h-6 text-[#800020]" />
          <h2 className="text-xl font-semibold text-slate-800">Prises en charge</h2>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={handlePrintListing}
            disabled={printingListing || loading}
            className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-[#800020] text-white hover:bg-[#650018] disabled:opacity-60"
            title="Imprimer la liste filtrée"
          >
            {printingListing ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Impression…</span>
              </>
            ) : (
              <>
                <Printer className="w-4 h-4" />
                <span>Imprimer la liste</span>
              </>
            )}
          </button>

          <button
            type="button"
            onClick={fetchData}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 px-3 py-2 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-300 disabled:opacity-70 disabled:cursor-not-allowed"
            title="Actualiser la liste"
          >
            {loading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" aria-hidden />
                <span>Actualisation…</span>
              </>
            ) : (
              <>
                <RefreshCw className="w-4 h-4" />
                <span>Actualiser</span>
              </>
            )}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
        <div>
          <label className="block text-sm text-slate-600 mb-1">Hôpital</label>
          <select
            value={hopitalId}
            onChange={(e) => {
              setPage(0);
              setHopitalId(e.target.value ? Number(e.target.value) : '');
            }}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 bg-white"
          >
            <option value="">Tous</option>
            {hopitaux.map((h) => (
              <option key={h.id} value={h.id}>
                {h.nom}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm text-slate-600 mb-1">Mois</label>
          <input
            type="month"
            value={month}
            onChange={(e) => {
              setPage(0);
              setMonth(e.target.value);
            }}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 bg-white"
          />
        </div>

        <div>
          <label className="block text-sm text-slate-600 mb-1">Taille page</label>
          <select
            value={size}
            onChange={(e) => {
              setPage(0);
              setSize(Number(e.target.value));
            }}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 bg-white"
          >
            {[10, 20, 50, 100].map((n) => (
              <option key={n} value={n}>
                {n}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-sm text-slate-600 mb-1">Recherche</label>
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Numéro, bénéficiaire, hôpital..."
              className="w-full rounded-lg border border-slate-300 pl-9 pr-3 py-2 bg-white"
            />
          </div>
        </div>
      </div>

      <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full w-max text-sm">
            <thead className="bg-slate-50 text-slate-600">
              <tr>
                <th className="text-left px-4 py-3 whitespace-nowrap">N°</th>
                <th className="text-left px-4 py-3 whitespace-nowrap">Date émission</th>
                <th className="text-left px-4 py-3 whitespace-nowrap">Date expiration</th>
                <th className="text-left px-4 py-3 whitespace-nowrap">Hôpital</th>
                <th className="text-left px-4 py-3 whitespace-nowrap">Bénéficiaire</th>
                <th className="text-left px-4 py-3 whitespace-nowrap">Qualité</th>
                <th className="text-left px-4 py-3 whitespace-nowrap">Statut</th>
                <th className="text-right px-4 py-3 whitespace-nowrap sticky right-0 bg-slate-50 shadow-[-4px_0_6px_rgba(0,0,0,0.04)] z-10">Actions</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} className="px-4 py-10 text-center text-slate-500">
                    <Loader2 className="w-5 h-5 inline-block animate-spin mr-2" />
                    Chargement…
                  </td>
                </tr>
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-4 py-10 text-center text-slate-500">
                    Aucune prise en charge trouvée.
                  </td>
                </tr>
              ) : (
                filtered.map((r) => (
                  <tr key={r.id} className="border-t border-slate-100 bg-white">
                    <td className="px-4 py-2 whitespace-nowrap">{r.numero}</td>
                    <td className="px-4 py-2 whitespace-nowrap">{formatDate(r.dateEmission)}</td>
                    <td className="px-4 py-2 whitespace-nowrap">{formatDate(r.dateExpiration) || '—'}</td>
                    <td className="px-4 py-2 whitespace-nowrap">{r.hopitalNom ?? '—'}</td>
                    <td className="px-4 py-2 whitespace-nowrap">{r.beneficiaireNom ?? '—'}</td>
                    <td className="px-4 py-2 whitespace-nowrap">{r.beneficiaireQualite ?? '—'}</td>
                    <td className="px-4 py-2 whitespace-nowrap">
                      {r.statut === 'Valide' ? (
                        <span className="inline-flex items-center rounded-full bg-green-50 px-2 py-0.5 text-xs font-medium text-green-700">Valide</span>
                      ) : r.statut === 'Expiré' ? (
                        <span className="inline-flex items-center rounded-full bg-red-50 px-2 py-0.5 text-xs font-medium text-red-700">Expiré</span>
                      ) : (
                        <span className="text-slate-500">{r.statut ?? '—'}</span>
                      )}
                    </td>

                    <td className="px-4 py-2 text-right whitespace-nowrap sticky right-0 bg-white shadow-[-4px_0_6px_rgba(0,0,0,0.04)] z-10">
                      <div className="flex justify-end gap-2 flex-shrink-0">
                      <button
                        onClick={() => handlePrintPec(r.id)}
                        className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 disabled:opacity-60"
                        title="Imprimer la note"
                        disabled={deletingId === r.id || printingPecId !== null}
                      >
                        {printingPecId === r.id ? (
                          <>
                            <Loader2 className="w-4 h-4 animate-spin" aria-hidden />
                            PDF…
                          </>
                        ) : (
                          <>
                            <Printer className="w-4 h-4" />
                            PDF
                          </>
                        )}
                      </button>

                      {hasPermission('PEC_DELETE') && (
                        <button
                          onClick={() => setDeleteConfirmRow(r)}
                          className="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-red-50 hover:bg-red-100 text-red-700 border border-red-200 disabled:opacity-60"
                          title="Supprimer"
                          disabled={deletingId === r.id}
                        >
                          {deletingId === r.id ? (
                            <>
                              <Loader2 className="w-4 h-4 animate-spin" />
                              Suppression…
                            </>
                          ) : (
                            <>
                              <Trash2 className="w-4 h-4" />
                              Supprimer
                            </>
                          )}
                        </button>
                      )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 px-4 py-3 bg-slate-50 border-t border-slate-200">
          <div className="text-sm text-slate-600">
            Page <span className="font-semibold">{(data?.pageNumber ?? page) + 1}</span> / {totalPages || 1}
          </div>

          <div className="flex items-center gap-2">
            <button
              disabled={page <= 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              className={`px-3 py-1.5 rounded-lg ${
                page <= 0
                  ? 'bg-slate-100 text-slate-400'
                  : 'bg-white border border-slate-200 hover:bg-slate-100'
              }`}
            >
              Précédent
            </button>

            <button
              disabled={page >= totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              className={`px-3 py-1.5 rounded-lg ${
                page >= totalPages - 1
                  ? 'bg-slate-100 text-slate-400'
                  : 'bg-white border border-slate-200 hover:bg-slate-100'
              }`}
            >
              Suivant
            </button>
          </div>
        </div>
      </div>

      {deleteConfirmRow && (
        <QuestionDialog
          open
          title="Confirmer la suppression"
          message={`Voulez-vous vraiment supprimer la PEC ${deleteConfirmRow.numero} ? Bénéficiaire : ${deleteConfirmRow.beneficiaireNom ?? '—'}. Hôpital : ${deleteConfirmRow.hopitalNom ?? '—'}.`}
          variant="danger"
          confirmLabel="Supprimer"
          loading={deletingId === deleteConfirmRow.id}
          onConfirm={() => performDeletePec(deleteConfirmRow)}
          onCancel={() => setDeleteConfirmRow(null)}
        />
      )}
    </div>
  );
};