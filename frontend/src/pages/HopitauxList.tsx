// src/pages/HopitauxList.tsx
import React, { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { hopitalService, type HopitalDetail, type HopitalSummary, type CategorieHopital } from '../services/hopitalService';
import type { PageResponse } from '../services/agentService';
import { HopitalForm } from '../components/HopitalForm';
import { Plus, Edit, Trash2, CheckCircle, XCircle, Loader2, RefreshCw } from 'lucide-react';
import { Skeleton, SkeletonTableRow } from '../components/ui/Skeleton';
import { QuestionDialog } from '../components/QuestionDialog';

/** Skeleton de la page Hôpitaux conventionnés */
const HopitauxListSkeleton: React.FC = () => (
  <div className="space-y-6" aria-hidden>
    <div className="flex items-center justify-between">
      <Skeleton className="h-9 w-64" />
      <Skeleton className="h-10 w-36 rounded-lg" />
    </div>
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
      <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
        <Skeleton className="h-10 w-full rounded-lg" />
        <Skeleton className="h-10 w-full rounded-lg" />
        <Skeleton className="h-10 w-full rounded-lg" />
        <Skeleton className="h-10 w-full rounded-lg" />
        <Skeleton className="h-10 w-full rounded-lg" />
      </div>
      <div className="mt-3 flex items-center gap-2">
        <Skeleton className="h-10 w-28 rounded-lg" />
        <Skeleton className="h-8 w-20 rounded-lg" />
      </div>
    </div>
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
      <table className="min-w-full">
        <thead className="bg-slate-50">
          <tr>
            {['Code', 'Nom', 'Catégorie', 'Ville', 'Actif', 'Actions'].map((label) => (
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
      <div className="flex items-center justify-between px-4 py-3 border-t bg-slate-50">
        <Skeleton className="h-4 w-48" />
        <div className="flex gap-2">
          <Skeleton className="h-8 w-20 rounded-lg" />
          <Skeleton className="h-8 w-20 rounded-lg" />
        </div>
      </div>
    </div>
  </div>
);

export const HopitauxList: React.FC = () => {
  const { hasPermission } = useAuth();
  const [data, setData] = useState<PageResponse<HopitalSummary> | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState('');

  // filtres / tri
  const [q, setQ] = useState('');
  const [actif, setActif] = useState<'all' | 'yes' | 'no'>('all');
  const [categorie, setCategorie] = useState<CategorieHopital | ''>('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sortBy, setSortBy] = useState('nom');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');

  // modal
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<HopitalDetail | undefined>();
  const [rowBusy, setRowBusy] = useState<number | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setErr('');
      const resp = await hopitalService.listPaged({
        q: q || undefined,
        categorie: (categorie as CategorieHopital) || undefined,
        actif: actif === 'all' ? undefined : actif === 'yes',
        page,
        size,
        sortBy,
        sortDir,
      });
      setData(resp);
    } catch (e: any) {
      setErr(e?.response?.data?.error || e?.message || 'Erreur de chargement');
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size, sortBy, sortDir]);

  const applyFilters = () => {
    setPage(0);
    load();
  };

  const openCreate = () => {
    setEditing(undefined);
    setShowForm(true);
  };

  const openEdit = async (id: number) => {
    try {
      setRowBusy(id);
      const d = await hopitalService.getById(id);
      setEditing(d);
      setShowForm(true);
    } catch (e: any) {
      setErr(e?.response?.data?.error || e?.message || 'Chargement impossible');
    } finally {
      setRowBusy(null);
    }
  };

  const onSubmit = async (payload: any) => {
    if (editing?.id) {
      await hopitalService.update(editing.id, payload);
    } else {
      await hopitalService.create(payload);
    }
    setShowForm(false);
    setEditing(undefined);
    await load();
  };

  const performDelete = async (id: number) => {
    await hopitalService.delete(id);
    await load();
  };

  const toggleActif = async (h: HopitalSummary) => {
    await hopitalService.setActif(h.id, !h.actif);
    await load();
  };

  if (loading && !data) {
    return <HopitauxListSkeleton />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h1 className="text-2xl sm:text-3xl font-bold text-slate-800">Hôpitaux conventionnés</h1>
        {hasPermission('HOPITAUX_CREATE') && (
          <button onClick={openCreate} className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 min-h-[44px] sm:min-h-0">
            <Plus className="w-5 h-5" />
            <span>Nouvel hôpital</span>
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          <input
            placeholder="Recherche (nom, ville...)"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            className="px-3 py-2 border rounded-lg"
          />
        <select
            value={categorie}
            onChange={(e) => setCategorie(e.target.value as CategorieHopital | '')}
            className="px-3 py-2 border rounded-lg"
          >
            <option value="">Catégorie (toutes)</option>
            <option value="PUBLIC">PUBLIC</option>
            <option value="PRIVE">PRIVE</option>
            <option value="CONFESSIONNEL">CONFESSIONNEL</option>
            <option value="AUTRE">AUTRE</option>
          </select>
          <select
            value={actif}
            onChange={(e) => setActif(e.target.value as any)}
            className="px-3 py-2 border rounded-lg"
          >
            <option value="all">Actif (tous)</option>
            <option value="yes">Actifs</option>
            <option value="no">Inactifs</option>
          </select>
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value)} className="px-3 py-2 border rounded-lg">
            <option value="nom">Nom</option>
            <option value="ville">Ville</option>
            <option value="categorie">Catégorie</option>
          </select>
          <select value={sortDir} onChange={(e) => setSortDir(e.target.value as 'asc' | 'desc')} className="px-3 py-2 border rounded-lg">
            <option value="asc">Asc</option>
            <option value="desc">Desc</option>
          </select>
        </div>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={applyFilters}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-slate-800 text-white rounded-lg hover:bg-slate-900 min-h-[44px] sm:min-h-0 disabled:opacity-70 disabled:cursor-not-allowed"
          >
            {loading ? <Loader2 className="w-4 h-4 animate-spin" aria-hidden /> : null}
            Rechercher
          </button>
          <button
            type="button"
            onClick={() => load()}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-slate-100 text-slate-700 rounded-lg hover:bg-slate-200 border border-slate-300 min-h-[44px] sm:min-h-0 disabled:opacity-70 disabled:cursor-not-allowed"
            title="Actualiser la liste"
          >
            {loading ? <Loader2 className="w-4 h-4 animate-spin" aria-hidden /> : <RefreshCw className="w-4 h-4" />}
            Actualiser
          </button>
          <select
            value={size}
            onChange={(e) => {
              setSize(Number(e.target.value));
              setPage(0);
            }}
            className="px-2 py-1.5 border rounded-lg min-h-[44px] sm:min-h-0"
          >
            {[10, 20, 50].map((s) => <option key={s} value={s}>{s}/page</option>)}
          </select>
        </div>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        {loading ? (
          <div className="p-6 text-slate-500 flex items-center gap-2">
            <Loader2 className="w-4 h-4 animate-spin" />
            Chargement…
          </div>
        ) : err ? (
          <div className="p-6 text-red-600">Erreur : {err}</div>
        ) : !data || data.empty ? (
          <div className="p-6 text-slate-500">Aucun hôpital trouvé.</div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full w-max">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Code</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Nom</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Catégorie</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Ville</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Actif</th>
                    <th className="text-right px-4 py-3 whitespace-nowrap sticky right-0 bg-slate-50 shadow-[-4px_0_6px_rgba(0,0,0,0.04)] z-10">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((h) => {
                    const busy = rowBusy === h.id;
                    return (
                      <tr key={h.id} className="border-t bg-white">
                        <td className="px-4 py-3 whitespace-nowrap">{h.code}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{h.nom}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{h.categorie}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{h.ville || '-'}</td>
                        <td className="px-4 py-3 whitespace-nowrap">
                          {h.actif ? (
                            <span className="inline-flex items-center gap-1 text-green-600">
                              <CheckCircle className="w-4 h-4" /> Actif
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-slate-500">
                              <XCircle className="w-4 h-4" /> Inactif
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap sticky right-0 bg-white shadow-[-4px_0_6px_rgba(0,0,0,0.04)] z-10">
                          <div className="flex items-center justify-end gap-1 sm:gap-2 flex-shrink-0">
                          {hasPermission('HOPITAUX_EDIT') && (
                            <button
                              onClick={() => openEdit(h.id)}
                              className="p-2 rounded-lg hover:bg-slate-100 disabled:opacity-50"
                              disabled={busy}
                              title="Modifier"
                            >
                              {busy ? <Loader2 className="w-4 h-4 animate-spin" /> : <Edit className="w-5 h-5" />}
                            </button>
                          )}
                          {hasPermission('HOPITAUX_EDIT') && (
                            <button
                              onClick={() => toggleActif(h)}
                              className="p-2 rounded-lg hover:bg-slate-100"
                              title={h.actif ? 'Désactiver' : 'Activer'}
                            >
                              {h.actif ? <XCircle className="w-5 h-5 text-slate-600" /> : <CheckCircle className="w-5 h-5 text-green-600" />}
                            </button>
                          )}
                          {hasPermission('HOPITAUX_DELETE') && (
                            <button
                              onClick={() => setDeleteConfirmId(h.id)}
                              className="p-2 rounded-lg hover:bg-red-50 text-red-600"
                              title="Supprimer"
                            >
                              <Trash2 className="w-5 h-5" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            </div>

            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 px-4 py-3 border-t bg-slate-50">
              <div className="text-sm text-slate-600">
                Page {data.currentPage + 1} / {data.totalPages} — {data.totalElements} éléments
              </div>
              <div className="flex gap-2">
                <button
                  disabled={data.first}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  className="px-3 py-1 border rounded-lg disabled:opacity-50"
                >
                  Précédent
                </button>
                <button
                  disabled={data.last}
                  onClick={() => setPage((p) => p + 1)}
                  className="px-3 py-1 border rounded-lg disabled:opacity-50"
                >
                  Suivant
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {showForm && (
        <HopitalForm
          hopital={editing}
          onSubmit={onSubmit}
          onCancel={() => { setShowForm(false); setEditing(undefined); }}
        />
      )}

      {deleteConfirmId !== null && (
        <QuestionDialog
          open
          title="Confirmer la suppression"
          message="Voulez-vous vraiment supprimer cet hôpital ? Cette action est irréversible."
          variant="danger"
          confirmLabel="Supprimer"
          onConfirm={async () => {
            await performDelete(deleteConfirmId);
            setDeleteConfirmId(null);
          }}
          onCancel={() => setDeleteConfirmId(null)}
        />
      )}
    </div>
  );
};
