// src/components/AgentDetails.tsx
import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { useAppDialog } from '../contexts/AppDialogContext';
import { agentService, Agent } from '../services/agentService';
import { conjointService } from '../services/conjointService';
import { enfantService, Enfant } from '../services/enfantService';
import { pecService } from '../services/pecService';
import { agentDocumentService, AgentDocument } from '../services/agentDocumentService';

import {
  ArrowLeft,
  CreditCard as Edit,
  Trash2,
  Plus,
  User,
  Users,
  Stethoscope,
  HeartHandshake,
} from 'lucide-react';

import { ConjointForm } from '../components/ConjointForm';
import { EnfantForm } from '../components/EnfantForm';
import { AgentDocumentForm } from '../components/AgentDocumentForm';
import { DocumentViewerModal } from '../components/DocumentViewerModal';
import { PecFormForConjoint } from '../components/PecFormForConjoint';
import { PecFormForEnfant } from '../components/PecFormForEnfant';
import { QuestionDialog } from '../components/QuestionDialog';

const fmtDate = (d?: string | Date) => {
  if (!d) return '-';
  try {
    const date = typeof d === 'string' ? new Date(d) : d;
    if (isNaN(date.getTime())) return '-';
    return date.toLocaleDateString();
  } catch {
    return '-';
  }
};

const yearsBetween = (d?: string | Date): number | null => {
  if (!d) return null;
  const date = typeof d === 'string' ? new Date(d) : d;
  if (isNaN(date.getTime())) return null;
  const now = new Date();
  let y = now.getFullYear() - date.getFullYear();
  const mDiff = now.getMonth() - date.getMonth();
  const dDiff = now.getDate() - date.getDate();
  if (mDiff < 0 || (mDiff === 0 && dDiff < 0)) y--;
  return y;
};

export const AgentDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { hasPermission } = useAuth();
  const { showConfirm, showError } = useAppDialog();

  const [agent, setAgent] = useState<Agent | null>(null);
  const [enfants, setEnfants] = useState<Enfant[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [showConjointForm, setShowConjointForm] = useState(false);
  const [showEnfantForm, setShowEnfantForm] = useState(false);

const [showDocsForm, setShowDocsForm] = useState(false);
const [docs, setDocs] = useState<AgentDocument[]>([]);
const [docsLoading, setDocsLoading] = useState(false);
const [docsErr, setDocsErr] = useState<string>('');
const [viewerOpen, setViewerOpen] = useState(false);
const [viewerTitle, setViewerTitle] = useState<string>('');
const [viewerBlobUrl, setViewerBlobUrl] = useState<string>('');
const [viewerContentType, setViewerContentType] = useState<string>('');
const [viewerDocId, setViewerDocId] = useState<number | null>(null);

  const [editingConjoint, setEditingConjoint] = useState<Agent['conjoint']>();
  const [editingEnfant, setEditingEnfant] = useState<Enfant | undefined>();

  // Modals PEC
  const [showPecForConjointId, setShowPecForConjointId] = useState<number | null>(null);
  const [showPecForEnfantId, setShowPecForEnfantId] = useState<number | null>(null);

  // Dialogue de confirmation (suppression)
  const [confirmDialog, setConfirmDialog] = useState<{
    title: string;
    message: string;
    variant?: 'danger' | 'default';
    onConfirm: () => void | Promise<void>;
  } | null>(null);

  // Photos
  const agentPhotoUrl = useMemo(() => agentService.photoUrl(agent?.photo, true), [agent?.photo]);
  const conjointPhotoUrl = useMemo(
    () => (agent?.conjoint?.photo ? conjointService.photoUrl(agent.conjoint.photo, true) : undefined),
    [agent?.conjoint?.photo]
  );

  const loadData = async () => {
    if (!id) return;
    setIsLoading(true);
    try {
      const agentData = await agentService.getDetails(Number(id)); // inclut conjoint
      setAgent(agentData);
      const enfantsData = await enfantService.listByAgent(Number(id));
      setEnfants(enfantsData);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);



// ---------------- Documents administratifs ----------------
const loadDocs = async (agentId: number) => {
  try {
    setDocsErr('');
    setDocsLoading(true);
    const list = await agentDocumentService.list(agentId);
    setDocs(list || []);
    setDocsLoading(false);
  } catch (e: any) {
    setDocsLoading(false);
    setDocsErr(e?.response?.data?.message || e?.message || 'Erreur lors du chargement des documents.');
  }
};

useEffect(() => {
  if (!id) return;
  const agentId = Number(id);
  if (!agentId) return;
  loadDocs(agentId);
  // eslint-disable-next-line react-hooks/exhaustive-deps
}, [id]);
  // CRUD Conjoint
  const handleConjointSubmit: React.ComponentProps<typeof ConjointForm>['onSubmit'] = async (conjoint, photoFile) => {
    if (!agent) return;
    if (editingConjoint?.id) {
      await conjointService.update(editingConjoint.id, conjoint, photoFile);
    } else {
      await conjointService.createForAgent(agent.id!, conjoint, photoFile);
    }
    await loadData();
    setShowConjointForm(false);
    setEditingConjoint(undefined);
  };

  const openDeleteConjointDialog = () => {
    if (!agent?.conjoint?.id) return;
    setConfirmDialog({
      title: 'Confirmer la suppression',
      message: 'Supprimer ce conjoint ?',
      variant: 'danger',
      onConfirm: async () => {
        await conjointService.delete(agent.conjoint!.id!);
        await loadData();
      },
    });
  };

  // CRUD Enfant
  const handleEnfantSubmit: React.ComponentProps<typeof EnfantForm>['onSubmit'] = async (enfant, photoFile) => {
    if (!agent) return;
    if (editingEnfant?.id) {
      await enfantService.update(editingEnfant.id, enfant, photoFile);
    } else {
      await enfantService.createForAgent(agent.id!, enfant, photoFile);
    }
    await loadData();
    setShowEnfantForm(false);
    setEditingEnfant(undefined);
  };

  const openDeleteEnfantDialog = (enfantId: number) => {
    setConfirmDialog({
      title: 'Confirmer la suppression',
      message: 'Supprimer cet enfant ?',
      variant: 'danger',
      onConfirm: async () => {
        await enfantService.delete(enfantId);
        await loadData();
      },
    });
  };

  if (isLoading) return <div className="text-center py-8">Chargement...</div>;
  if (!agent) return <div className="text-center py-8">Agent non trouvé</div>;

  
const closeViewer = () => {
  if (viewerBlobUrl) URL.revokeObjectURL(viewerBlobUrl);
  setViewerBlobUrl('');
  setViewerContentType('');
  setViewerTitle('');
  setViewerDocId(null);
  setViewerOpen(false);
};

const handleViewDoc = async (doc: AgentDocument) => {
  try {
    setDocsErr('');
    const { blob, contentType } = await agentDocumentService.fetchViewBlob(doc.id);
    const url = URL.createObjectURL(blob);
    setViewerBlobUrl(url);
    setViewerContentType(contentType || doc.contentType || '');
    setViewerTitle(doc.title || doc.originalName);
    setViewerDocId(doc.id);
    setViewerOpen(true);
  } catch (e: any) {
    setDocsErr(e?.response?.data?.message || e?.message || 'Impossible d’ouvrir le document.');
  }
};

const handleDownloadDoc = async (doc: AgentDocument) => {
  try {
    const { blob, fileName } = await agentDocumentService.fetchDownloadBlob(doc.id);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName || doc.originalName || 'document';
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  } catch (e: any) {
    setDocsErr(e?.response?.data?.message || e?.message || 'Impossible de télécharger le document.');
  }
};

const openDeleteDocDialog = (doc: AgentDocument) => {
  setConfirmDialog({
    title: 'Confirmer la suppression',
    message: 'Supprimer ce document ?',
    variant: 'danger',
    onConfirm: async () => {
      try {
        await agentDocumentService.remove(doc.id);
        if (id) await agentDocumentService.list(Number(id)).then(setDocs);
      } catch (e: unknown) {
        const err = e as { response?: { data?: { message?: string }; message?: string }; message?: string };
        setDocsErr(err?.response?.data?.message || err?.message || 'Suppression impossible.');
      }
    },
  });
};

const handleUploadDoc = async (formData: FormData) => {
  if (!id) return;
  await agentDocumentService.upload(Number(id), formData);
  const list = await agentDocumentService.list(Number(id));
  setDocs(list || []);
};

return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => navigate('/')}
          className="flex items-center space-x-2 text-slate-600 hover:text-slate-800"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Retour</span>
        </button>
      </div>

      {/* Identité Agent */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
        <div className="flex items-start space-x-6">
          {agent.photo ? (
            <img
              key={agent.photo}
              src={agentPhotoUrl}
              alt={`${agent.nom} ${agent.prenom}`}
              className="w-32 h-32 rounded-lg object-cover border-2 border-slate-200"
              onError={(e) => {
                (e.currentTarget as HTMLImageElement).style.display = 'none';
              }}
            />
          ) : (
            <div className="w-32 h-32 rounded-lg bg-slate-100 flex items-center justify-center">
              <User className="w-16 h-16 text-slate-400" />
            </div>
          )}

          <div className="flex-1">
            <h1 className="text-3xl font-bold text-slate-800 mb-2">
              {agent.nom} {agent.postnom} {agent.prenom}
            </h1>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <span className="text-slate-500">Genre:</span>{' '}
                <span className="font-medium">
                  {agent.genre === 'M' ? 'Masculin' : agent.genre === 'F' ? 'Féminin' : '-'}
                </span>
              </div>
              <div>
                <span className="text-slate-500">Date de naissance:</span>{' '}
                <span className="font-medium">{fmtDate(agent.datenaiss)}</span>
              </div>
              <div>
                <span className="text-slate-500">Lieu de naissance:</span>{' '}
                <span className="font-medium">{agent.lnaiss || '-'}</span>
              </div>
              {agent.telephone && (
                <div>
                  <span className="text-slate-500">Téléphone:</span>{' '}
                  <span className="font-medium">{agent.telephone}</span>
                </div>
              )}
              {agent.email && (
                <div>
                  <span className="text-slate-500">Email:</span>{' '}
                  <span className="font-medium">{agent.email}</span>
                </div>
              )}
              {agent.direction && (
                <div>
                  <span className="text-slate-500">Direction:</span>{' '}
                  <span className="font-medium">{agent.direction}</span>
                </div>
              )}
            </div>

            {agent.adresse && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm mt-4">
                <div className="md:col-span-3">
                  <span className="text-slate-500">Adresse:</span>{' '}
                  <span className="font-medium">{agent.adresse}</span>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Conjoint */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-slate-800 flex items-center space-x-2">
            <HeartHandshake className="w-5 h-5" />
            <span>Conjoint</span>
          </h2>
          {!agent.conjoint && hasPermission('CONJOINT_ADD') && (
            <button
              onClick={() => setShowConjointForm(true)}
              className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              <Plus className="w-4 h-4" />
              <span>Ajouter</span>
            </button>
          )}
        </div>

        {agent.conjoint ? (
          <div className="flex items-start justify-between p-4 bg-slate-50 rounded-lg">
            <div className="flex items-start space-x-4">
              {agent.conjoint.photo ? (
                <img
                  key={agent.conjoint.photo}
                  src={conjointPhotoUrl}
                  alt="Conjoint"
                  className="w-20 h-20 rounded-lg object-cover"
                  onError={(e) => {
                    (e.currentTarget as HTMLImageElement).style.display = 'none';
                  }}
                />
              ) : (
                <div className="w-20 h-20 rounded-lg bg-slate-200 flex items-center justify-center">
                  <User className="w-10 h-10 text-slate-400" />
                </div>
              )}
              <div>
                <h3 className="font-medium text-slate-800">
                  {agent.conjoint.nom} {agent.conjoint.postnom} {agent.conjoint.prenom}
                </h3>
                <p className="text-sm text-slate-600">
                  {agent.conjoint.genre === 'M'
                    ? 'Masculin'
                    : agent.conjoint.genre === 'F'
                    ? 'Féminin'
                    : '-'}
                </p>
                {agent.conjoint.telephone && (
                  <p className="text-sm text-slate-600">{agent.conjoint.telephone}</p>
                )}
              </div>
            </div>
            <div className="flex gap-2">
              {hasPermission('CONJOINT_PEC') && (
                <button
                  onClick={() => setShowPecForConjointId(agent.conjoint!.id!)}
                  className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-emerald-600 text-white hover:bg-emerald-700"
                  title="Nouvelle prise en charge (conjoint)"
                >
                  <Stethoscope className="w-4 h-4" />
                  PEC
                </button>
              )}
              {hasPermission('CONJOINT_EDIT') && (
                <button
                  onClick={() => {
                    setEditingConjoint(agent.conjoint!);
                    setShowConjointForm(true);
                  }}
                  className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                  title="Modifier"
                >
                  <Edit className="w-4 h-4" />
                </button>
              )}
              {hasPermission('CONJOINT_DELETE') && (
                <button
                  onClick={openDeleteConjointDialog}
                  className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                  title="Supprimer"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              )}
            </div>
          </div>
        ) : (
          <p className="text-slate-500 text-center py-4">Aucun conjoint enregistré</p>
        )}
      </div>

      {/* Enfants */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-slate-800 flex items-center space-x-2">
            <Users className="w-5 h-5" />
            <span>Enfants ({enfants.length})</span>
          </h2>
          {hasPermission('ENFANT_ADD') && (
            <button
              onClick={() => setShowEnfantForm(true)}
              className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              <Plus className="w-4 h-4" />
              <span>Ajouter</span>
            </button>
          )}
        </div>

        {enfants.length > 0 ? (
          <div className="space-y-3">
            {enfants.map((enfant) => {
              const enfantPhotoUrl = enfantService.photoUrl(enfant.photo, true);
              const age = yearsBetween(enfant.datenaiss || undefined);
              const allowed = age === null || age <= 25; // UI hint

              return (
                <div
                  key={enfant.id}
                  className="flex items-start justify-between p-4 bg-slate-50 rounded-lg"
                >
                  <div className="flex items-start space-x-4">
                    {enfant.photo ? (
                      <img
                        key={enfant.photo}
                        src={enfantPhotoUrl}
                        alt="Enfant"
                        className="w-16 h-16 rounded-lg object-cover"
                        onError={(e) => {
                          (e.currentTarget as HTMLImageElement).style.display = 'none';
                        }}
                      />
                    ) : (
                      <div className="w-16 h-16 rounded-lg bg-slate-200 flex items-center justify-center">
                        <User className="w-8 h-8 text-slate-400" />
                      </div>
                    )}
                    <div>
                      <h3 className="font-medium text-slate-800">
                        {enfant.nomEnfant} {enfant.postnomEnfant} {enfant.prenomEnfant}
                      </h3>
                      <p className="text-sm text-slate-600">
                        {enfant.genre === 'M'
                          ? 'Masculin'
                          : enfant.genre === 'F'
                          ? 'Féminin'
                          : '-'}{' '}
                        • {enfant.categorie}
                        {age !== null && ` • ${age} ans`}
                      </p>
                      <p className="text-sm text-slate-600">{fmtDate(enfant.datenaiss)}</p>
                    </div>
                  </div>

                  <div className="flex gap-2">
                    {hasPermission('ENFANT_PEC') && (
                      <button
                        onClick={() => setShowPecForEnfantId(enfant.id!)}
                        className={`inline-flex items-center gap-2 px-3 py-2 rounded-lg ${
                          allowed
                            ? 'bg-emerald-600 text-white hover:bg-emerald-700'
                            : 'bg-slate-200 text-slate-500 cursor-not-allowed'
                        }`}
                        disabled={!allowed}
                        title={
                          allowed
                            ? 'Nouvelle prise en charge (enfant)'
                            : "Âge > 25 ans (règle d'éligibilité)"
                        }
                      >
                        <Stethoscope className="w-4 h-4" />
                        PEC
                      </button>
                    )}
                    {hasPermission('ENFANT_EDIT') && (
                      <button
                        onClick={() => {
                          setEditingEnfant(enfant);
                          setShowEnfantForm(true);
                        }}
                        className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                        title="Modifier"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                    )}
                    {hasPermission('ENFANT_DELETE') && (
                      <button
                        onClick={() => openDeleteEnfantDialog(enfant.id!)}
                        className="p-2 text-red-600 hover:bg-red-50 rounded-lg"
                        title="Supprimer"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <p className="text-slate-500 text-center py-4">Aucun enfant enregistré</p>
        )}
      </div>

      {/* Modals CRUD */}
      {showConjointForm && (
        <ConjointForm
          conjoint={editingConjoint}
          onSubmit={handleConjointSubmit}
          onCancel={() => {
            setShowConjointForm(false);
            setEditingConjoint(undefined);
          }}
        />
      )}

      {showEnfantForm && (
        <EnfantForm
          enfant={editingEnfant}
          onSubmit={handleEnfantSubmit}
          onCancel={() => {
            setShowEnfantForm(false);
            setEditingEnfant(undefined);
          }}
        />
      )}

      {/* Modal Nouvelle PEC (Conjoint) */}
      {showPecForConjointId && (
        <PecFormForConjoint
          conjointId={showPecForConjointId}
          defaultBeneficiaireLabel="Conjoint(e) d’agent"
          defaultQualite="Conjoint(e)"
          onCreated={async (pecId: number) => {
            setShowPecForConjointId(null);
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
          onCancel={() => setShowPecForConjointId(null)}
        />
      )}

      {/* Modal Nouvelle PEC (Enfant) */}
      {showPecForEnfantId && (
        <PecFormForEnfant
          enfantId={showPecForEnfantId}
          defaultBeneficiaireLabel="Enfant d’agent"
          defaultQualite="Enfant"
          onCreated={async (pecId: number) => {
            setShowPecForEnfantId(null);
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
          onCancel={() => setShowPecForEnfantId(null)}
        />
      )}

      {/* Dialogue de confirmation (suppression) */}
      {confirmDialog && (
        <QuestionDialog
          open
          title={confirmDialog.title}
          message={confirmDialog.message}
          variant={confirmDialog.variant}
          confirmLabel={confirmDialog.variant === 'danger' ? 'Supprimer' : undefined}
          onConfirm={confirmDialog.onConfirm}
          onCancel={() => setConfirmDialog(null)}
        />
      )}
    </div>
  );
};

export default AgentDetails;
