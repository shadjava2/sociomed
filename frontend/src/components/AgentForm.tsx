// src/components/AgentForm.tsx
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Agent, agentService } from '../services/agentService';
import { X, Upload, User, Camera, Loader2 } from 'lucide-react';
import { WebcamCaptureModal } from './WebcamCaptureModal';

type Props = {
  agent?: Agent; // si présent = édition
  onSubmit: (agent: Agent, photoFile?: File) => Promise<void> | void;
  onCancel: () => void;
};

const toIsoDate = (value?: string | Date) => {
  if (!value) return '';
  if (typeof value === 'string') {
    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value; // déjà ISO
    const m = value.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
    if (m) return `${m[3]}-${m[2]}-${m[1]}`; // dd/MM/yyyy -> yyyy-MM-dd
    const d = new Date(value);
    if (!isNaN(d.getTime())) return d.toISOString().slice(0, 10);
    return '';
  }
  const d = value;
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
};

const getAgeFromDate = (dateStr?: string | null): number | null => {
  if (!dateStr || !String(dateStr).trim()) return null;
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return null;
  const today = new Date();
  let age = today.getFullYear() - d.getFullYear();
  const m = today.getMonth() - d.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < d.getDate())) age--;
  return age >= 0 ? age : null;
};

export const AgentForm: React.FC<Props> = ({ agent, onSubmit, onCancel }) => {
  const isEdit = Boolean(agent?.id);

  // --------- State du formulaire (initialisé + synchronisé avec props.agent) ---------
  const [form, setForm] = useState<Agent>({
    nom: agent?.nom || '',
    postnom: agent?.postnom || '',
    prenom: agent?.prenom || '',
    genre: agent?.genre || undefined,
    datenaiss: toIsoDate(agent?.datenaiss),
    lnaiss: agent?.lnaiss || '',
    etatc: agent?.etatc || '',
    village: agent?.village || '',
    groupement: agent?.groupement || '',
    secteur: agent?.secteur || '',
    territoire: agent?.territoire || '',
    district: agent?.district || '',
    province: agent?.province || '',
    nationalite: agent?.nationalite || '',
    telephone: agent?.telephone || '',
    email: agent?.email || '',
    adresse: agent?.adresse || '',
    direction: agent?.direction || '',
    etat: agent?.etat || '',
    stat: agent?.stat || 'ACTIF',
    photo: agent?.photo || undefined,
    categorie: agent?.categorie || undefined,
  });

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    // quand l'agent change (ou ouverture pour un autre), on réhydrate le form
    setForm({
      nom: agent?.nom || '',
      postnom: agent?.postnom || '',
      prenom: agent?.prenom || '',
      genre: agent?.genre || undefined,
      datenaiss: toIsoDate(agent?.datenaiss),
      lnaiss: agent?.lnaiss || '',
      etatc: agent?.etatc || '',
      village: agent?.village || '',
      groupement: agent?.groupement || '',
      secteur: agent?.secteur || '',
      territoire: agent?.territoire || '',
      district: agent?.district || '',
      province: agent?.province || '',
      nationalite: agent?.nationalite || '',
      telephone: agent?.telephone || '',
      email: agent?.email || '',
      adresse: agent?.adresse || '',
      direction: agent?.direction || '',
      etat: agent?.etat || '',
      stat: agent?.stat || 'ACTIF',
      photo: agent?.photo || undefined,
      categorie: agent?.categorie || undefined,
    });
    // on reset aussi la sélection de photo
    setPhotoFile(undefined);
    setPhotoPreview(undefined);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }, [agent?.id]); // se base sur l'id; si tu veux réagir à tout changement, remplace par [agent]

  // --------- Gestion photo ---------
  const [photoFile, setPhotoFile] = useState<File | undefined>(undefined);
  const [photoPreview, setPhotoPreview] = useState<string | undefined>(undefined);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [showWebcamModal, setShowWebcamModal] = useState(false);

  const existingPhotoUrl = useMemo(
    () => agentService.photoUrl(form.photo, true), // cache-busting
    [form.photo]
  );

  useEffect(() => {
    if (!photoFile) {
      setPhotoPreview(undefined);
      return;
    }
    const url = URL.createObjectURL(photoFile);
    setPhotoPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [photoFile]);

  const update = <K extends keyof Agent>(key: K, value: Agent[K]) => {
    setForm((f) => ({ ...f, [key]: value }));
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (!f) {
      setPhotoFile(undefined);
      return;
    }
    setPhotoFile(f);
  };

  const handleClearPhotoSelection = () => {
    setPhotoFile(undefined);
    setPhotoPreview(undefined);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  // --------- Submit ---------
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const nextErrors: Record<string, string> = {};
    if (!form.nom?.trim()) nextErrors.nom = 'Nom obligatoire';
    if (!form.postnom?.trim()) nextErrors.postnom = 'Postnom obligatoire';
    if (!form.prenom?.trim()) nextErrors.prenom = 'Prénom obligatoire';
    if (!form.lnaiss?.trim()) nextErrors.lnaiss = 'Lieu de naissance obligatoire';
    if (!form.genre) nextErrors.genre = 'Genre obligatoire';

    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSubmitting(true);
    try {
    const payload: Agent = {
      ...form,
      datenaiss: form.datenaiss ? toIsoDate(form.datenaiss) : undefined,
      lnaiss: form.lnaiss.trim(),
      etatc: form.etatc?.trim() || undefined,
      village: form.village?.trim() || undefined,
      groupement: form.groupement?.trim() || undefined,
      secteur: form.secteur?.trim() || undefined,
      territoire: form.territoire?.trim() || undefined,
      district: form.district?.trim() || undefined,
      province: form.province?.trim() || undefined,
      nationalite: form.nationalite?.trim() || undefined,
      telephone: form.telephone?.trim() || undefined,
      email: form.email?.trim() || undefined,
      adresse: form.adresse?.trim() || undefined,
      direction: form.direction?.trim() || undefined,
      etat: form.etat?.trim() || undefined,
      stat: form.stat?.trim() || undefined,
      // NB: on ne touche pas à form.photo ici. Si tu envoies un fichier,
      // l’endpoint multipart écrasera la photo; sinon, on conserve la valeur actuelle côté back.
    };

    await onSubmit(payload, photoFile);
    } finally {
      setSubmitting(false);
    }
  };

  const inputClass = 'w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent';
  const labelClass = 'block text-sm font-medium text-slate-700 mb-2';

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <WebcamCaptureModal
        open={showWebcamModal}
        onClose={() => setShowWebcamModal(false)}
        onCapture={(file) => {
          setPhotoFile(file);
          setShowWebcamModal(false);
        }}
        title="Prendre une photo (webcam)"
      />
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-slate-200 px-6 py-4 flex justify-between items-center">
          <h2 className="text-2xl font-bold text-slate-800">
            {isEdit ? 'Modifier un agent' : 'Nouvel agent'}
          </h2>
          <button onClick={onCancel} className="p-2 hover:bg-slate-100 rounded-lg transition-colors" aria-label="Fermer">
            <X className="w-6 h-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6">
          {Object.keys(errors).length > 0 && (
            <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
              Veuillez remplir les champs obligatoires (nom, postnom, prénom, lieu de naissance, genre).
            </div>
          )}

          {/* Photo — même disposition que formulaire Sénateur */}
          <div className="mb-6">
            <label className={labelClass}>Photo</label>
            <div className="flex flex-wrap items-center gap-3">
              {photoPreview ? (
                <img src={photoPreview} alt="" className="w-24 h-24 rounded-lg object-cover border-2 border-slate-200" />
              ) : existingPhotoUrl ? (
                <img src={existingPhotoUrl} alt="" className="w-24 h-24 rounded-lg object-cover border-2 border-slate-200" onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }} />
              ) : (
                <div className="w-24 h-24 rounded-lg bg-slate-100 flex items-center justify-center">
                  <User className="w-12 h-12 text-slate-400" />
                </div>
              )}
              <input ref={fileInputRef} type="file" accept="image/*" onChange={handleFileChange} className="hidden" />
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="flex items-center space-x-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
              >
                <Upload className="w-4 h-4" />
                <span className="text-sm font-medium">{photoPreview || existingPhotoUrl ? 'Changer une photo' : 'Choisir une photo'}</span>
              </button>
              <button
                type="button"
                onClick={() => setShowWebcamModal(true)}
                className="flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                title="Prendre une photo avec la webcam"
              >
                <Camera className="w-4 h-4" />
                <span className="text-sm font-medium">Prendre une photo</span>
              </button>
              {photoPreview && (
                <button type="button" onClick={handleClearPhotoSelection} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">
                  Annuler
                </button>
              )}
            </div>
          </div>

          {/* Identité */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div>
              <label className={labelClass}>Nom <span className="text-red-500">*</span></label>
              <input value={form.nom} onChange={(e) => update('nom', e.target.value)} className={inputClass} required />
            </div>
            <div>
              <label className={labelClass}>Postnom <span className="text-red-500">*</span></label>
              <input value={form.postnom || ''} onChange={(e) => update('postnom', e.target.value)} className={inputClass} required />
            </div>
            <div>
              <label className={labelClass}>Prénom</label>
              <input value={form.prenom || ''} onChange={(e) => update('prenom', e.target.value)} className={inputClass} />
            </div>
          </div>

          {/* Genre, Catégorie, Date et lieu de naissance */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div>
              <label className={labelClass}>Genre <span className="text-red-500">*</span></label>
              <select value={form.genre || ''} onChange={(e) => update('genre', (e.target.value || undefined) as Agent['genre'])} className={inputClass} required>
                <option value="">—</option>
                <option value="M">Masculin</option>
                <option value="F">Féminin</option>
              </select>
            </div>
            <div>
              <label className={labelClass}>Catégorie</label>
              <select value={form.categorie || ''} onChange={(e) => update('categorie', e.target.value || undefined)} className={inputClass}>
                <option value="">—</option>
                <option value="Personnel d'appoint">Personnel d'appoint</option>
                <option value="Agent Administratif">Agent Administratif</option>
                <option value="Cadre Administratif">Cadre Administratif</option>
              </select>
            </div>
            <div>
              <label className={labelClass}>Date de naissance</label>
              <input type="date" value={form.datenaiss || ''} onChange={(e) => update('datenaiss', e.target.value)} className={inputClass} />
              {form.datenaiss && getAgeFromDate(form.datenaiss) !== null && (
                <p className="mt-1 text-sm text-slate-500">Âge : {getAgeFromDate(form.datenaiss)} ans</p>
              )}
            </div>
            <div className="md:col-span-2">
              <label className={labelClass}>Lieu de naissance <span className="text-red-500">*</span></label>
              <input value={form.lnaiss || ''} onChange={(e) => update('lnaiss', e.target.value)} className={inputClass} required />
            </div>
          </div>

          {/* Contact */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div>
              <label className={labelClass}>Téléphone</label>
              <input type="tel" value={form.telephone || ''} onChange={(e) => update('telephone', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Email</label>
              <input type="email" value={form.email || ''} onChange={(e) => update('email', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Adresse</label>
              <input value={form.adresse || ''} onChange={(e) => update('adresse', e.target.value)} className={inputClass} />
            </div>
          </div>

          {/* Affectation */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div>
              <label className={labelClass}>Direction</label>
              <input value={form.direction || ''} onChange={(e) => update('direction', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>État</label>
              <input value={form.etat || ''} onChange={(e) => update('etat', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Stat <span className="text-red-500">*</span></label>
              <input value={form.stat || ''} onChange={(e) => update('stat', e.target.value)} className={inputClass} required />
            </div>
            <div>
              <label className={labelClass}>État civil</label>
              <input value={form.etatc || ''} onChange={(e) => update('etatc', e.target.value)} className={inputClass} />
            </div>
          </div>

          {/* Localisation */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div>
              <label className={labelClass}>Village</label>
              <input value={form.village || ''} onChange={(e) => update('village', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Groupement</label>
              <input value={form.groupement || ''} onChange={(e) => update('groupement', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Secteur</label>
              <input value={form.secteur || ''} onChange={(e) => update('secteur', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Territoire</label>
              <input value={form.territoire || ''} onChange={(e) => update('territoire', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>District</label>
              <input value={form.district || ''} onChange={(e) => update('district', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Province</label>
              <input value={form.province || ''} onChange={(e) => update('province', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Nationalité</label>
              <input value={form.nationalite || ''} onChange={(e) => update('nationalite', e.target.value)} className={inputClass} />
            </div>
          </div>

          <div className="flex justify-end space-x-3 pt-6 border-t border-slate-200">
            <button
              type="button"
              onClick={onCancel}
              className="px-6 py-2 border border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="inline-flex items-center justify-center gap-2 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {submitting ? (
                <>
                  <Loader2 className="w-5 h-5 shrink-0 animate-spin" aria-hidden />
                  <span>Enregistrement…</span>
                </>
              ) : (
                isEdit ? 'Enregistrer' : 'Créer'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
