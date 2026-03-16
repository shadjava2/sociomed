// src/pages/SenateursList.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useAppDialog } from '../contexts/AppDialogContext';
import { senateurService, Senateur, PageResponse } from '../services/senateurService';
import { Plus, Edit, Trash2, Eye, Loader2, User, Stethoscope, RefreshCw } from 'lucide-react';
import { PecFormForSenateur } from '../components/PecFormForSenateur';
import { pecService } from '../services/pecService';
import { SenateurForm } from '../components/SenateurForm';
import { QuestionDialog } from '../components/QuestionDialog';

type Statut = 'EN_ACTIVITE' | 'HONORAIRE' | '';
type Genre = 'M' | 'F' | '';

export const SenateursList: React.FC = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const { showConfirm, showError } = useAppDialog();

  // UI state (modal + loader)
  const [showForm, setShowForm] = useState(false);
  const [editingSenateur, setEditingSenateur] = useState<Senateur | undefined>();
  const [listLoading, setListLoading] = useState<boolean>(true);
  const [editLoadingId, setEditLoadingId] = useState<number | null>(null);
  const [modalBootLoading, setModalBootLoading] = useState<boolean>(false);

  // ➕ Modal PEC (par sénateur)
  const [showPecForSenateurId, setShowPecForSenateurId] = useState<number | null>(null);

  // Popup confirmation suppression sénateur
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  // Data state
  const [data, setData] = useState<PageResponse<Senateur> | null>(null);
  const [err, setErr] = useState<string>('');

  // Query/pagination state
  const [q, setQ] = useState<string>('');
  const [genre, setGenre] = useState<Genre>('');
  const [statut, setStatut] = useState<Statut>('');
  const [legislature, setLegislature] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [size, setSize] = useState<number>(10);
  const [sortBy, setSortBy] = useState<string>('nom');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');

  const loadList = async () => {
    try {
      setListLoading(true);
      setErr('');
      const resp = await senateurService.listPaged({
        q: q || undefined,
        genre: (genre || undefined) as 'M' | 'F' | undefined,
        statut: (statut || undefined) as 'EN_ACTIVITE' | 'HONORAIRE' | undefined,
        legislature: legislature || undefined,
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
      setListLoading(false);
    }
  };

  useEffect(() => {
    loadList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, size, sortBy, sortDir]);

  const applyFilters = () => {
    setPage(0);
    loadList();
  };

  const handleEdit = async (id: number) => {
    try {
      setEditLoadingId(id);
      setErr('');
      // Charger le DÉTAIL (pour préremplir)
      const full = await senateurService.getById(id);
      setEditingSenateur(full);
      // petit overlay
      setModalBootLoading(true);
      setShowForm(true);
      setTimeout(() => setModalBootLoading(false), 150);
    } catch (e: any) {
      setErr(e?.response?.data?.error || e?.message || "Impossible de charger le sénateur.");
    } finally {
      setEditLoadingId(null);
    }
  };

  const handleCreateClick = () => {
    setEditingSenateur(undefined);
    setModalBootLoading(false);
    setShowForm(true);
  };

  const handleSubmit = async (senateur: Senateur, photoFile?: File) => {
    try {
      if (editingSenateur?.id) {
        await senateurService.update(editingSenateur.id, senateur, photoFile);
      } else {
        await senateurService.create(senateur, photoFile);
      }
      setShowForm(false);
      setEditingSenateur(undefined);
      await loadList();
    } catch (e: any) {
      setErr(e?.response?.data?.error || e?.message || "Échec de l'enregistrement");
    }
  };

  const performDeleteSenateur = async (id: number) => {
    try {
      await senateurService.delete(id);
      await loadList();
    } catch (e: any) {
      setErr(e?.response?.data?.error || e?.message || 'Suppression impossible');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h1 className="text-2xl sm:text-3xl font-bold text-slate-800">Gestion des Sénateurs</h1>
        {hasPermission('SENATEUR_CREATE') && (
          <button
            onClick={handleCreateClick}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors min-h-[44px] sm:min-h-0"
          >
            <Plus className="w-5 h-5" />
            <span>Nouveau sénateur</span>
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          <input
            placeholder="Recherche (nom, postnom, prénom, email, tel)"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            className="px-3 py-2 border rounded-lg"
          />
          <select
            value={genre}
            onChange={(e) => setGenre(e.target.value as Genre)}
            className="px-3 py-2 border rounded-lg"
          >
            <option value="">Genre (tous)</option>
            <option value="M">Masculin</option>
            <option value="F">Féminin</option>
          </select>
          <select
            value={statut}
            onChange={(e) => setStatut(e.target.value as Statut)}
            className="px-3 py-2 border rounded-lg"
          >
            <option value="">Statut (tous)</option>
            <option value="EN_ACTIVITE">En activité</option>
            <option value="HONORAIRE">Honoraire</option>
          </select>
          <input
            placeholder="Législature"
            value={legislature}
            onChange={(e) => setLegislature(e.target.value)}
            className="px-3 py-2 border rounded-lg"
          />
          <button
            type="button"
            onClick={applyFilters}
            disabled={listLoading}
            className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-slate-800 text-white rounded-lg hover:bg-slate-900 disabled:opacity-70 disabled:cursor-not-allowed"
          >
            {listLoading ? <Loader2 className="w-4 h-4 animate-spin" aria-hidden /> : null}
            Rechercher
          </button>
          <button
            type="button"
            onClick={() => loadList()}
            disabled={listLoading}
            className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-slate-100 text-slate-700 rounded-lg hover:bg-slate-200 border border-slate-300 disabled:opacity-70 disabled:cursor-not-allowed"
            title="Actualiser la liste"
          >
            {listLoading ? <Loader2 className="w-4 h-4 animate-spin" aria-hidden /> : <RefreshCw className="w-4 h-4" />}
            Actualiser
          </button>
        </div>

        <div className="flex flex-wrap items-center gap-2 sm:gap-3 mt-3">
          <label className="text-sm text-slate-600 w-full sm:w-auto">Trier par</label>
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value)} className="px-2 py-1.5 border rounded-lg min-h-[44px] sm:min-h-0">
            <option value="nom">Nom</option>
            <option value="postnom">Postnom</option>
            <option value="prenom">Prénom</option>
            <option value="statut">Statut</option>
            <option value="legislature">Législature</option>
          </select>
          <select
            value={sortDir}
            onChange={(e) => setSortDir(e.target.value as 'asc' | 'desc')}
            className="px-2 py-1.5 border rounded-lg min-h-[44px] sm:min-h-0"
          >
            <option value="asc">Asc</option>
            <option value="desc">Desc</option>
          </select>
          <select
            value={size}
            onChange={(e) => {
              setSize(Number(e.target.value));
              setPage(0);
            }}
            className="sm:ml-auto px-2 py-1.5 border rounded-lg min-h-[44px] sm:min-h-0"
          >
            {[10, 20, 50].map((s) => (
              <option key={s} value={s}>
                {s}/page
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Table — scroll horizontal + colonne Actions fixe à droite pour petit écran */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden relative">
        {listLoading ? (
          <div className="p-6 text-slate-500 flex items-center gap-2">
            <Loader2 className="w-4 h-4 animate-spin" />
            Chargement…
          </div>
        ) : err ? (
          <div className="p-6 text-red-600">Erreur : {err}</div>
        ) : !data || data.empty ? (
          <div className="p-6 text-slate-500">Aucun sénateur trouvé.</div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full w-max">
                <thead className="bg-slate-50">
                  <tr>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Photo</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Nom</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Postnom</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Prénom</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Genre</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Statut</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Législature</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Téléphone</th>
                    <th className="text-right px-4 py-3 whitespace-nowrap sticky right-0 bg-slate-50 shadow-[-4px_0_6px_rgba(0,0,0,0.04)] z-10">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((s) => {
                    const isThisRowEditing = editLoadingId === s.id;
                    const thumb = senateurService.photoUrl(s.photo);
                    return (
                      <tr key={s.id} className="border-t bg-white">
                        <td className="px-4 py-3 whitespace-nowrap">
                          {thumb ? (
                            <img
                              src={thumb}
                              alt={`${s.nom} ${s.prenom}`}
                              className="w-10 h-10 rounded object-cover border"
                              loading="lazy"
                              onError={(e) => {
                                (e.currentTarget as HTMLImageElement).style.display = 'none';
                              }}
                            />
                          ) : (
                            <div className="w-10 h-10 rounded bg-slate-100 flex items-center justify-center">
                              <User className="w-5 h-5 text-slate-400" />
                            </div>
                          )}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap">{s.nom}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{s.postnom}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{s.prenom}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{s.genre}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{s.statut}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{s.legislature || '-'}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{s.telephone || '-'}</td>
                        <td className="px-4 py-3 whitespace-nowrap sticky right-0 bg-white shadow-[-4px_0_6px_rgba(0,0,0,0.04)] z-10">
                          <div className="flex items-center justify-end gap-1 sm:gap-2 flex-shrink-0">
                          {hasPermission('SENATEUR_PEC') && (
                            <button
                              onClick={() => setShowPecForSenateurId(s.id!)}
                              className="p-2 rounded-lg hover:bg-emerald-50 text-emerald-700"
                              title="Nouvelle prise en charge"
                              disabled={!!editLoadingId}
                            >
                              <Stethoscope className="w-5 h-5" />
                            </button>
                          )}
                          {hasPermission('SENATEUR_VIEW') && (
                            <button
                              onClick={() => navigate(`/senateurs/${s.id}`)}
                              className="p-2 rounded-lg hover:bg-slate-100"
                              title="Voir"
                              disabled={!!editLoadingId}
                            >
                              <Eye className="w-5 h-5" />
                            </button>
                          )}
                          {hasPermission('SENATEUR_EDIT') && (
                            <button
                              onClick={() => handleEdit(s.id!)}
                              className="p-2 rounded-lg hover:bg-slate-100 disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center gap-2"
                              title="Éditer"
                              disabled={!!editLoadingId}
                            >
                              {isThisRowEditing ? (
                                <>
                                  <Loader2 className="w-4 h-4 animate-spin" />
                                  <span className="text-sm">Ouverture…</span>
                                </>
                              ) : (
                                <Edit className="w-5 h-5" />
                              )}
                            </button>
                          )}
                          {hasPermission('SENATEUR_DELETE') && (
                            <button
                              onClick={() => setDeleteConfirmId(s.id!)}
                              className="p-2 rounded-lg hover:bg-red-50 text-red-600 disabled:opacity-50"
                              title="Supprimer"
                              disabled={!!editLoadingId}
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

            {/* Pagination */}
            <div className="flex items-center justify-between px-4 py-3 border-t bg-slate-50">
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

        {/* Petit overlay lors de l'ouverture du modal (esthétique) */}
        {modalBootLoading && (
          <div className="absolute inset-0 bg-white/40 backdrop-blur-[1px] flex items-center justify-center">
            <div className="flex items-center gap-2 text-slate-700 bg-white/80 px-3 py-2 rounded-lg border">
              <Loader2 className="w-4 h-4 animate-spin" />
              Préparation du formulaire…
            </div>
          </div>
        )}
      </div>

      {/* Modal Nouvelle PEC (Sénateur) */}
      {showPecForSenateurId && (
        <PecFormForSenateur
          senateurId={showPecForSenateurId}
          onCreated={async (pecId) => {
            setShowPecForSenateurId(null);
            const ok = await showConfirm({
              title: 'PEC enregistrée',
              message: 'Voulez-vous imprimer maintenant ?',
              confirmLabel: 'Imprimer',
              cancelLabel: 'Plus tard',
            });
            if (ok) {
              try {
                await pecService.print(pecId);
              } catch {
                showError("Erreur lors de l'impression du PDF.");
              }
            }
          }}
          onCancel={() => setShowPecForSenateurId(null)}
        />
      )}

      {/* Modal Formulaire Sénateur */}
      {showForm && (
        <SenateurForm
          senateur={editingSenateur}
          onSubmit={handleSubmit}
          onCancel={() => {
            setShowForm(false);
            setEditingSenateur(undefined);
          }}
        />
      )}

      {/* Popup confirmation suppression sénateur */}
      {deleteConfirmId !== null && (
        <QuestionDialog
          open
          title="Confirmer la suppression"
          message="Voulez-vous vraiment supprimer ce sénateur ? Cette action est irréversible."
          variant="danger"
          confirmLabel="Supprimer"
          onConfirm={async () => {
            await performDeleteSenateur(deleteConfirmId);
            setDeleteConfirmId(null);
          }}
          onCancel={() => setDeleteConfirmId(null)}
        />
      )}
    </div>
  );
};

export default SenateursList;
