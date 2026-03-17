// src/pages/AgentsList.tsx
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useAppDialog } from '../contexts/AppDialogContext';
import { usePdfViewer } from '../contexts/PdfViewerContext';
import { agentService, Agent, PageResponse } from '../services/agentService';
import { Plus, Edit, Trash2, Eye, Loader2, User, Stethoscope, RefreshCw } from 'lucide-react';
import { PecFormForAgent } from '../components/PecFormForAgent';
import { pecService } from '../services/pecService';
import { AgentForm } from '../components/AgentForm';
import { QuestionDialog } from '../components/QuestionDialog';
import { Skeleton, SkeletonTableRow } from '../components/ui/Skeleton';

const COLS = 8; // Photo, Nom, Postnom, Prénom, Genre, Catégorie, Direction, Actions

/** Skeleton de la page Gestion des Agents */
const AgentsListSkeleton: React.FC = () => (
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
      <div className="flex items-center gap-3 mt-3">
        <Skeleton className="h-4 w-16" />
        <Skeleton className="h-8 w-24 rounded-lg" />
        <Skeleton className="h-8 w-20 rounded-lg" />
        <Skeleton className="h-8 w-20 ml-auto rounded-lg" />
      </div>
    </div>
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
      <table className="min-w-full">
        <thead className="bg-slate-50">
          <tr>
            {['Photo', 'Nom', 'Postnom', 'Prénom', 'Genre', 'Catégorie', 'Direction', 'Actions'].map((label) => (
              <th key={label} className="text-left px-4 py-3 text-sm font-medium text-slate-500">
                {label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {Array.from({ length: 10 }).map((_, i) => (
            <SkeletonTableRow key={i} cols={COLS} />
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

export const AgentsList: React.FC = () => {
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const { showConfirm, showError } = useAppDialog();
  const { openPdf } = usePdfViewer();

  // UI state (modal + loader)
  const [showForm, setShowForm] = useState(false);
  const [editingAgent, setEditingAgent] = useState<Agent | undefined>();
  const [listLoading, setListLoading] = useState<boolean>(true);
  const [editLoadingId, setEditLoadingId] = useState<number | null>(null);
  const [modalBootLoading, setModalBootLoading] = useState<boolean>(false);

  // ➕ Modal PEC (par agent)
  const [showPecForAgentId, setShowPecForAgentId] = useState<number | null>(null);

  // Popup confirmation suppression agent
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  // Data state
  const [data, setData] = useState<PageResponse<Agent> | null>(null);
  const [err, setErr] = useState<string>('');

  // Query/pagination state
  const [q, setQ] = useState<string>('');
  const [genre, setGenre] = useState<'M' | 'F' | ''>('');
  const [categorie, setCategorie] = useState<string>('');
  const [direction, setDirection] = useState<string>('');
  const [etat, setEtat] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [size, setSize] = useState<number>(10);
  const [sortBy, setSortBy] = useState<string>('nom');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc');

  const loadList = async () => {
    try {
      setListLoading(true);
      setErr('');
      const resp = await agentService.listPaged({
        q: q || undefined,
        genre: (genre || undefined) as 'M' | 'F' | undefined,
        categorie: categorie || undefined,
        direction: direction || undefined,
        etat: etat || undefined,
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
      const full = await agentService.getDetails(id);
      setEditingAgent(full);
      // petit overlay
      setModalBootLoading(true);
      setShowForm(true);
      setTimeout(() => setModalBootLoading(false), 150);
    } catch (e: any) {
      setErr(e?.response?.data?.error || e?.message || "Impossible de charger l'agent.");
    } finally {
      setEditLoadingId(null);
    }
  };

  const handleCreateClick = () => {
    setEditingAgent(undefined);
    setModalBootLoading(false);
    setShowForm(true);
  };

  const handleSubmit = async (agent: Agent, photoFile?: File) => {
    try {
      if (editingAgent?.id) {
        await agentService.update(editingAgent.id, agent, photoFile);
      } else {
        await agentService.create(agent, photoFile);
      }
      setShowForm(false);
      setEditingAgent(undefined);
      await loadList();
    } catch (e: any) {
      setErr(e?.response?.data?.error || e?.message || "Échec de l'enregistrement");
    }
  };

  const performDeleteAgent = async (id: number) => {
    try {
      await agentService.delete(id);
      await loadList();
    } catch (e: unknown) {
      const ex = e as { response?: { data?: { error?: string }; message?: string }; message?: string };
      setErr(ex?.response?.data?.error || ex?.message || 'Suppression impossible');
    }
  };

  if (listLoading && !data) {
    return <AgentsListSkeleton />;
  }

  return (
    <div className="space-y-6">
      {/* Header — responsive : stack sur mobile */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h1 className="text-2xl sm:text-3xl font-bold text-slate-800">Gestion des Agents</h1>
        {hasPermission('AGENT_CREATE') && (
          <button
            onClick={handleCreateClick}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors min-h-[44px] sm:min-h-0"
          >
            <Plus className="w-5 h-5" />
            <span>Nouvel agent</span>
          </button>
        )}
      </div>

      {/* Filtres — grille responsive, stack sur mobile */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3">
          <input
            placeholder="Recherche (nom, postnom, prenom)"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            className="px-3 py-2 border rounded-lg"
          />
          <select
            value={genre}
            onChange={(e) => setGenre(e.target.value as 'M' | 'F' | '')}
            className="px-3 py-2 border rounded-lg"
          >
            <option value="">Genre (tous)</option>
            <option value="M">Masculin</option>
            <option value="F">Féminin</option>
          </select>
          <select
            value={categorie}
            onChange={(e) => setCategorie(e.target.value)}
            className="px-3 py-2 border rounded-lg"
          >
            <option value="">Catégorie (toutes)</option>
            <option value="Personnel d'appoint">Personnel d'appoint</option>
            <option value="Agent Administratif">Agent Administratif</option>
            <option value="Cadre Administratif">Cadre Administratif</option>
          </select>
          <input
            placeholder="Direction"
            value={direction}
            onChange={(e) => setDirection(e.target.value)}
            className="px-3 py-2 border rounded-lg"
          />
          <input
            placeholder="État (ex. ACTIF)"
            value={etat}
            onChange={(e) => setEtat(e.target.value)}
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
            <option value="categorie">Catégorie</option>
            <option value="direction">Direction</option>
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

      {/* Table — scroll horizontal + colonne Actions fixe sur petit écran */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden relative">
        {listLoading ? (
          <div className="p-6 text-slate-500 flex items-center gap-2">
            <Loader2 className="w-4 h-4 animate-spin" />
            Chargement…
          </div>
        ) : err ? (
          <div className="p-6 text-red-600">Erreur : {err}</div>
        ) : !data || data.empty ? (
          <div className="p-6 text-slate-500">Aucun agent trouvé.</div>
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
                    <th className="text-left px-4 py-3 whitespace-nowrap">Catégorie</th>
                    <th className="text-left px-4 py-3 whitespace-nowrap">Direction</th>
                    <th className="text-right px-4 py-3 whitespace-nowrap sticky right-0 bg-slate-50 shadow-[-4px_0_6px_rgba(0,0,0,0.04)] z-10">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((a) => {
                    const isThisRowEditing = editLoadingId === a.id;
                    const thumb = agentService.photoUrl(a.photo);
                    return (
                      <tr key={a.id} className="border-t bg-white">
                        <td className="px-4 py-3 whitespace-nowrap">
                          {thumb ? (
                            <img
                              src={thumb}
                              alt={`${a.nom} ${a.prenom}`}
                              className="w-10 h-10 rounded object-cover border"
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
                        <td className="px-4 py-3 whitespace-nowrap">{a.nom}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{a.postnom}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{a.prenom}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{a.genre}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{a.categorie || '-'}</td>
                        <td className="px-4 py-3 whitespace-nowrap">{a.direction || '-'}</td>
                        <td className="px-4 py-3 whitespace-nowrap sticky right-0 bg-white shadow-[-4px_0_6px_rgba(0,0,0,0.04)] z-10">
                          <div className="flex items-center justify-end gap-1 sm:gap-2 flex-shrink-0">
                          {hasPermission('AGENT_VIEW') && (
                            <button
                              onClick={() => navigate(`/agents/${a.id}`)}
                              className="p-2 rounded-lg hover:bg-slate-100"
                              title="Voir"
                              disabled={!!editLoadingId}
                            >
                              <Eye className="w-5 h-5" />
                            </button>
                          )}
                          {hasPermission('AGENT_PEC_CREATE') && (
                            <button
                              onClick={() => setShowPecForAgentId(a.id!)}
                              className="p-2 rounded-lg hover:bg-emerald-50 text-emerald-700"
                              title="Nouvelle prise en charge"
                              disabled={!!editLoadingId}
                            >
                              <Stethoscope className="w-5 h-5" />
                            </button>
                          )}
                          {hasPermission('AGENT_EDIT') && (
                            <button
                              onClick={() => handleEdit(a.id!)}
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
                          {hasPermission('AGENT_DELETE') && (
                            <button
                              onClick={() => setDeleteConfirmId(a.id!)}
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

      {/* Modal Nouvelle PEC (Agent) */}
      {showPecForAgentId && (
        <PecFormForAgent
          agentId={showPecForAgentId}
          onCreated={async (pecId) => {
            setShowPecForAgentId(null);
            const ok = await showConfirm({
              title: 'PEC enregistrée',
              message: 'Voulez-vous imprimer maintenant ?',
              confirmLabel: 'Imprimer',
              cancelLabel: 'Plus tard',
            });
            if (ok) {
              try {
                const { blob, filename } = await pecService.print(pecId);
                openPdf(blob, filename, `Prise en charge #${pecId}`);
              } catch {
                showError("Erreur lors de l'impression du PDF.");
              }
            }
          }}
          onCancel={() => setShowPecForAgentId(null)}
        />
      )}

      {/* Modal Formulaire Agent */}
      {showForm && (
        <AgentForm
          agent={editingAgent}
          onSubmit={handleSubmit}
          onCancel={() => {
            setShowForm(false);
            setEditingAgent(undefined);
          }}
        />
      )}

      {/* Popup confirmation suppression — design cohérent avec l'app */}
      {deleteConfirmId !== null && (
        <QuestionDialog
          open
          title="Confirmer la suppression"
          message="Voulez-vous vraiment supprimer cet agent ? Cette action est irréversible."
          variant="danger"
          confirmLabel="Supprimer"
          onConfirm={async () => {
            await performDeleteAgent(deleteConfirmId);
            setDeleteConfirmId(null);
          }}
          onCancel={() => setDeleteConfirmId(null)}
        />
      )}
    </div>
  );
};

export default AgentsList;
