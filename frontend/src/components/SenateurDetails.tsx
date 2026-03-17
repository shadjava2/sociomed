// src/components/SenateurDetails.tsx
import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAppDialog } from '../contexts/AppDialogContext';
import { usePdfViewer } from '../contexts/PdfViewerContext';
import { senateurService, Senateur } from '../services/senateurService';
import { conjointService, Conjoint } from '../services/conjointService';
import { enfantService, Enfant } from '../services/enfantService';
import { API_BASE_URL } from '../config/api';
import {
  ArrowLeft,
  CreditCard as Edit,
  Trash2,
  Plus,
  User,
  Users,
  Award,
  Stethoscope,
} from 'lucide-react';
import { ConjointForm } from './ConjointForm';
import { EnfantFormSenateur } from './EnfantFormSenateur';
import { PecFormForConjoint } from './PecFormForConjoint';
import { PecFormForEnfant } from './PecFormForEnfant';
import { pecService } from '../services/pecService';
import { QuestionDialog } from './QuestionDialog';

// Supprime /uploads/photos/ ou /uploads/photos au début + éventuels / en trop
const stripUploadsPrefix = (p?: string) =>
  p ? p.replace(/^\/?uploads\/photos\//, '').replace(/^\/+/, '') : undefined;

const computeAgeFromString = (dateStr?: string): number | null => {
  if (!dateStr) return null;
  // Accepte YYYY-MM-DD, DD/MM/YYYY, etc. (on essaie simple)
  const parts =
    dateStr.includes('-')
      ? dateStr.split('-') // yyyy-mm-dd
      : dateStr.includes('/')
      ? dateStr.split('/') // dd/mm/yyyy
      : null;
  if (!parts || parts.length !== 3) return null;

  let y = 0,
    m = 0,
    d = 0;

  if (dateStr.includes('-')) {
    // yyyy-mm-dd
    y = parseInt(parts[0], 10);
    m = parseInt(parts[1], 10);
    d = parseInt(parts[2], 10);
  } else {
    // dd/mm/yyyy
    d = parseInt(parts[0], 10);
    m = parseInt(parts[1], 10);
    y = parseInt(parts[2], 10);
  }
  if (!y || !m || !d) return null;

  const dob = new Date(y, m - 1, d);
  if (isNaN(dob.getTime())) return null;

  const now = new Date();
  let age = now.getFullYear() - dob.getFullYear();
  const beforeBirthdayThisYear =
    now.getMonth() < dob.getMonth() ||
    (now.getMonth() === dob.getMonth() && now.getDate() < dob.getDate());
  if (beforeBirthdayThisYear) age--;
  return age;
};

export const SenateurDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { showConfirm, showError } = useAppDialog();
  const { openPdf } = usePdfViewer();
  const [senateur, setSenateur] = useState<Senateur | null>(null);
  const [enfants, setEnfants] = useState<Enfant[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showConjointForm, setShowConjointForm] = useState(false);
  const [showEnfantForm, setShowEnfantForm] = useState(false);
  const [editingConjoint, setEditingConjoint] = useState<Conjoint | undefined>();
  const [editingEnfant, setEditingEnfant] = useState<Enfant | undefined>();

  // Modals PEC (conjoint + enfant)
  const [showPecForConjointId, setShowPecForConjointId] = useState<number | null>(null);
  const [showPecForEnfantId, setShowPecForEnfantId] = useState<number | null>(null);

  // Popup confirmation suppression (conjoint ou enfant)
  const [deleteConfirm, setDeleteConfirm] = useState<null | 'conjoint' | { type: 'enfant'; id: number }>(null);

  const loadData = async () => {
    if (!id) return;
    setIsLoading(true);
    try {
      const senateurData = await senateurService.getById(Number(id));
      setSenateur(senateurData);
      const enfantsData = await enfantService.listBySenateur(Number(id));
      setEnfants(enfantsData);
    } catch (error) {
      console.error('Erreur chargement:', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const handleConjointSubmit = async (conjoint: Conjoint, photoFile?: File) => {
    if (!senateur) return;
    if (editingConjoint?.id) {
      await conjointService.update(editingConjoint.id, conjoint, photoFile);
    } else {
      await conjointService.createForSenateur(senateur.id!, conjoint, photoFile);
    }
    await loadData();
    setShowConjointForm(false);
    setEditingConjoint(undefined);
  };

  const handleEnfantSubmit = async (enfant: Enfant, photoFile?: File) => {
    if (!senateur) return;
    if (editingEnfant?.id) {
      await enfantService.update(editingEnfant.id, enfant, photoFile);
    } else {
      await enfantService.createForSenateur(senateur.id!, enfant, photoFile);
    }
    await loadData();
    setShowEnfantForm(false);
    setEditingEnfant(undefined);
  };

  const openDeleteConjointDialog = () => {
    if (senateur?.conjoint?.id) setDeleteConfirm('conjoint');
  };

  const performDeleteConjoint = async () => {
    if (!senateur?.conjoint?.id) return;
    await conjointService.delete(senateur.conjoint.id);
    await loadData();
    setDeleteConfirm(null);
  };

  const openDeleteEnfantDialog = (enfantId: number) => {
    setDeleteConfirm({ type: 'enfant', id: enfantId });
  };

  const performDeleteEnfant = async (enfantId: number) => {
    await enfantService.delete(enfantId);
    await loadData();
    setDeleteConfirm(null);
  };

  // URL photo sénateur (avec léger cache-bust facultatif)
  const senateurPhotoUrl = useMemo(() => {
    if (!senateur?.photo) return undefined;
    const base = senateurService.photoUrl(senateur.photo);
    return base ? `${base}?t=${Date.now()}` : undefined;
  }, [senateur?.photo]);

  // URL photo conjoint : on normalise pour ne garder que le filename
  const conjointPhotoUrl = useMemo(() => {
    const raw = senateur?.conjoint?.photo;
    if (!raw) return undefined;
    const filename = stripUploadsPrefix(raw);
    return filename ? conjointService.photoUrl(filename, true) : undefined;
  }, [senateur?.conjoint?.photo]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg text-slate-600">Chargement...</div>
      </div>
    );
  }

  if (!senateur) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-slate-800 mb-4">Sénateur non trouvé</h2>
          <button
            onClick={() => navigate('/senateurs')}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            Retour à la liste
          </button>
        </div>
      </div>
    );
  }

  const senateurFullname =
    `${senateur.nom ?? ''} ${senateur.postnom ?? ''} ${senateur.prenom ?? ''}`.trim();

  return (
    <div className="space-y-6">
      {/* En-tête page — Retour aligné à gauche, cohérent avec les sections */}
      <header className="flex items-center justify-between">
        <button
          onClick={() => navigate('/senateurs')}
          className="inline-flex items-center gap-2 px-4 py-2 text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors font-medium"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Retour</span>
        </button>
      </header>

      {/* Carte sénateur */}
      <div className="bg-white rounded-xl shadow-lg border border-slate-200 overflow-hidden">
        <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-4">
          <div className="flex items-center space-x-2 text-white">
            <Award className="w-6 h-6" />
            <h1 className="text-2xl font-bold">Profil du Sénateur</h1>
          </div>
        </div>

        <div className="p-6">
          <div className="flex flex-col md:flex-row items-start md:items-center space-y-6 md:space-y-0 md:space-x-8">
            {senateur.photo ? (
              <img
                src={senateurPhotoUrl}
                alt={`${senateur.nom} ${senateur.prenom}`}
                className="w-40 h-40 rounded-xl object-cover border-4 border-slate-200 shadow-lg"
                loading="lazy"
                onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
              />
            ) : (
              <div className="w-40 h-40 rounded-xl bg-gradient-to-br from-slate-100 to-slate-200 flex items-center justify-center border-4 border-slate-200 shadow-lg">
                <User className="w-20 h-20 text-slate-400" />
              </div>
            )}

            <div className="flex-1">
              <h2 className="text-3xl font-bold text-slate-800 mb-3">
                {senateur.nom} {senateur.postnom} {senateur.prenom}
              </h2>

              <div className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium mb-4 bg-blue-100 text-blue-800">
                <Award className="w-4 h-4 mr-2" />
                {senateur.statut === 'EN_ACTIVITE' ? 'En activité' : 'Honoraire'}
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="flex items-start space-x-3">
                  <div className="text-slate-500 font-medium min-w-[120px]">Genre:</div>
                  <div className="text-slate-800 font-medium">
                    {senateur.genre === 'M' ? 'Masculin' : 'Féminin'}
                  </div>
                </div>

                <div className="flex items-start space-x-3">
                  <div className="text-slate-500 font-medium min-w-[120px]">Date naissance:</div>
                  <div className="text-slate-800 font-medium">
                    {senateur.datenaiss ? String(senateur.datenaiss) : ''}
                  </div>
                </div>

                {senateur.legislature && (
                  <div className="flex items-start space-x-3">
                    <div className="text-slate-500 font-medium min-w-[120px]">Législature:</div>
                    <div className="text-slate-800 font-medium">{senateur.legislature}</div>
                  </div>
                )}

                {senateur.telephone && (
                  <div className="flex items-start space-x-3">
                    <div className="text-slate-500 font-medium min-w-[120px]">Téléphone:</div>
                    <div className="text-slate-800 font-medium">{senateur.telephone}</div>
                  </div>
                )}

                {senateur.email && (
                  <div className="flex items-start space-x-3">
                    <div className="text-slate-500 font-medium min-w-[120px]">Email:</div>
                    <div className="text-slate-800 font-medium">{senateur.email}</div>
                  </div>
                )}

                {senateur.adresse && (
                  <div className="flex items-start space-x-3">
                    <div className="text-slate-500 font-medium min-w-[120px]">Adresse:</div>
                    <div className="text-slate-800 font-medium">{senateur.adresse}</div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Conjoint */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
            <User className="w-5 h-5" />
            <span>Conjoint</span>
          </h2>
          {!senateur.conjoint && (
            <button
              onClick={() => setShowConjointForm(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
            >
              <Plus className="w-4 h-4" />
              <span>Ajouter</span>
            </button>
          )}
        </div>

        {senateur.conjoint ? (
          <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 p-4 bg-slate-50 rounded-lg">
            <div className="flex items-start space-x-4 min-w-0">
              {senateur.conjoint.photo ? (
                <img
                  src={conjointPhotoUrl}
                  alt="Conjoint"
                  className="w-20 h-20 rounded-lg object-cover border-2 border-slate-200"
                  loading="lazy"
                  onError={(e) => {
                    // Fallback 1x vers /api/agents/photos/**
                    const el = e.currentTarget as HTMLImageElement;
                    if (el.dataset.triedFallback !== '1') {
                      el.dataset.triedFallback = '1';
                      const filename = stripUploadsPrefix(senateur.conjoint?.photo);
                      el.src = filename ? `${API_BASE_URL}/api/agents/photos/${filename}` : '';
                    } else {
                      el.style.display = 'none';
                    }
                  }}
                />
              ) : (
                <div className="w-20 h-20 rounded-lg bg-slate-200 flex items-center justify-center">
                  <User className="w-10 h-10 text-slate-400" />
                </div>
              )}
              <div>
                <h3 className="font-medium text-slate-800 text-lg">
                  {senateur.conjoint.nom} {senateur.conjoint.postnom} {senateur.conjoint.prenom}
                </h3>
                <p className="text-sm text-slate-600">
                  {senateur.conjoint.genre === 'M' ? 'Masculin' : 'Féminin'}
                </p>
                {senateur.conjoint.telephone && (
                  <p className="text-sm text-slate-600">{senateur.conjoint.telephone}</p>
                )}
              </div>
            </div>
            <div className="flex flex-wrap items-center justify-end gap-2 flex-shrink-0">
              {/* Bouton PEC pour le conjoint */}
              <button
                onClick={() => setShowPecForConjointId(senateur.conjoint!.id!)}
                className="inline-flex items-center gap-1 px-3 py-2 rounded-lg hover:bg-emerald-50 text-emerald-700 border border-emerald-200 transition-colors"
                title="Nouvelle prise en charge (Conjoint)"
              >
                <Stethoscope className="w-5 h-5" />
                <span className="text-sm font-medium">PEC</span>
              </button>
              <button
                onClick={() => {
                  setEditingConjoint(senateur.conjoint!);
                  setShowConjointForm(true);
                }}
                className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                title="Modifier"
              >
                <Edit className="w-4 h-4" />
              </button>
              <button
                onClick={openDeleteConjointDialog}
                className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                title="Supprimer"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
        ) : (
          <p className="text-slate-500 text-center py-4">Aucun conjoint enregistré</p>
        )}
      </div>

      {/* Enfants — même disposition que Conjoint : titre à gauche, Ajouter à droite */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <h2 className="text-xl font-bold text-slate-800 flex items-center gap-2">
            <Users className="w-5 h-5" />
            <span>Enfants ({enfants.length})</span>
          </h2>
          <button
            onClick={() => setShowEnfantForm(true)}
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
          >
            <Plus className="w-4 h-4" />
            <span>Ajouter</span>
          </button>
        </div>

        {enfants.length > 0 ? (
          <div className="space-y-3">
            {enfants.map((enfant) => {
              const enfantPhotoUrl = enfant.photo
                ? enfantService.photoUrl(enfant.photo, true)
                : undefined;
              const age = computeAgeFromString(
                enfant.datenaiss ? String(enfant.datenaiss) : undefined
              );
              const enfantFullname =
                `${enfant.nomEnfant ?? ''} ${enfant.postnomEnfant ?? ''} ${enfant.prenomEnfant ?? ''}`.trim();
              return (
                <div
                  key={enfant.id}
                  className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4 p-4 bg-slate-50 rounded-lg hover:bg-slate-100 transition-colors"
                >
                  <div className="flex items-start space-x-4 min-w-0">
                    {enfantPhotoUrl ? (
                      <img
                        src={enfantPhotoUrl}
                        alt="Enfant"
                        className="w-16 h-16 rounded-lg object-cover border-2 border-slate-200"
                        loading="lazy"
                        onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
                      />
                    ) : (
                      <div className="w-16 h-16 rounded-lg bg-slate-200 flex items-center justify-center">
                        <User className="w-8 h-8 text-slate-400" />
                      </div>
                    )}
                    <div>
                      <h3 className="font-medium text-slate-800">
                        {enfantFullname}
                      </h3>
                      <div className="flex items-center gap-3 text-sm text-slate-600 mt-1">
                        <span>{enfant.genre === 'M' ? 'Masculin' : 'Féminin'}</span>
                        <span>•</span>
                        <span className="px-2 py-0.5 bg-slate-200 rounded-full text-xs">
                          {enfant.categorie}
                        </span>
                        {age !== null && (
                          <>
                            <span>•</span>
                            <span className="px-2 py-0.5 bg-slate-200 rounded-full text-xs">
                              {age} ans
                            </span>
                          </>
                        )}
                      </div>
                      <p className="text-sm text-slate-600 mt-1">
                        {enfant.datenaiss ? String(enfant.datenaiss) : ''}
                      </p>
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center justify-end gap-2 flex-shrink-0">
                    <button
                      onClick={() => setShowPecForEnfantId(enfant.id!)}
                      className="inline-flex items-center gap-1 px-3 py-2 rounded-lg hover:bg-emerald-50 text-emerald-700 border border-emerald-200 transition-colors"
                      title="Nouvelle prise en charge (Enfant)"
                    >
                      <Stethoscope className="w-5 h-5" />
                      <span className="text-sm font-medium">PEC</span>
                    </button>
                    <button
                      onClick={() => {
                        setEditingEnfant(enfant);
                        setShowEnfantForm(true);
                      }}
                      className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                      title="Modifier"
                    >
                      <Edit className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => openDeleteEnfantDialog(enfant.id!)}
                      className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                      title="Supprimer"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
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
        <EnfantFormSenateur
          enfant={editingEnfant}
          onSubmit={handleEnfantSubmit}
          onCancel={() => {
            setShowEnfantForm(false);
            setEditingEnfant(undefined);
          }}
        />
      )}

      {/* Modals PEC */}
      {showPecForConjointId && (
        <PecFormForConjoint
          conjointId={showPecForConjointId}
          defaultBeneficiaireLabel={
            `${senateur.conjoint?.nom ?? ''} ${senateur.conjoint?.postnom ?? ''} ${senateur.conjoint?.prenom ?? ''}`.trim() || 'Conjoint'
          }
          defaultQualite={
            `Conjoint(e) de ${senateurFullname}`.trim()
          }
          onCreated={async (pecId) => {
            setShowPecForConjointId(null);
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
          onCancel={() => setShowPecForConjointId(null)}
        />
      )}

      {showPecForEnfantId && (
        <PecFormForEnfant
          enfantId={showPecForEnfantId}
          defaultBeneficiaireLabel={
            (() => {
              const e = enfants.find(x => x.id === showPecForEnfantId);
              return `${e?.nomEnfant ?? ''} ${e?.postnomEnfant ?? ''} ${e?.prenomEnfant ?? ''}`.trim() || 'Enfant';
            })()
          }
          defaultQualite={`Enfant de ${senateurFullname}`}
          onCreated={async (pecId) => {
            setShowPecForEnfantId(null);
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
          onCancel={() => setShowPecForEnfantId(null)}
        />
      )}

      {deleteConfirm === 'conjoint' && (
        <QuestionDialog
          open
          title="Confirmer la suppression"
          message="Voulez-vous vraiment supprimer ce conjoint ? Cette action est irréversible."
          variant="danger"
          confirmLabel="Supprimer"
          onConfirm={performDeleteConjoint}
          onCancel={() => setDeleteConfirm(null)}
        />
      )}
      {deleteConfirm?.type === 'enfant' && (
        <QuestionDialog
          open
          title="Confirmer la suppression"
          message="Voulez-vous vraiment supprimer cet enfant ? Cette action est irréversible."
          variant="danger"
          confirmLabel="Supprimer"
          onConfirm={() => performDeleteEnfant(deleteConfirm.id)}
          onCancel={() => setDeleteConfirm(null)}
        />
      )}
    </div>
  );
};

export default SenateurDetails;
