import React, { useEffect, useMemo, useRef, useState } from 'react';
import { enfantService, Enfant } from '../services/enfantService';
import { X, Upload, User, Camera, Loader2 } from 'lucide-react';
import { WebcamCaptureModal } from './WebcamCaptureModal';

type Props = {
  agentId?: number;
  senateurId?: number;
  enfant?: Enfant; // si présent = édition
  onSubmit: (enfant: Enfant, photoFile?: File) => Promise<void> | void;
  onCancel: () => void;
};

const toIsoDate = (value?: string | Date) => {
  if (!value) return '';
  if (typeof value === 'string') {
    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) return value;
    const m = value.match(/^(\d{2})\/(\d{2})\/(\d{4})$/);
    if (m) return `${m[3]}-${m[2]}-${m[1]}`;
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

export const EnfantForm: React.FC<Props> = ({ agentId, senateurId, enfant, onSubmit, onCancel }) => {
  const isEdit = Boolean(enfant?.id);

  const [form, setForm] = useState<Enfant>({
    nomEnfant: enfant?.nomEnfant || '',
    postnomEnfant: enfant?.postnomEnfant || '',
    prenomEnfant: enfant?.prenomEnfant || '',
    genre: enfant?.genre || 'M',
    datenaiss: toIsoDate(enfant?.datenaiss),
    categorie: enfant?.categorie || 'LEGITIME',
    stat: enfant?.stat || '',
    reference: enfant?.reference || '',
    photo: enfant?.photo || undefined,
  });

  useEffect(() => {
    setForm({
      nomEnfant: enfant?.nomEnfant || '',
      postnomEnfant: enfant?.postnomEnfant || '',
      prenomEnfant: enfant?.prenomEnfant || '',
      genre: enfant?.genre || 'M',
      datenaiss: toIsoDate(enfant?.datenaiss),
      categorie: enfant?.categorie || 'LEGITIME',
      stat: enfant?.stat || '',
      reference: enfant?.reference || '',
      photo: enfant?.photo || undefined,
    });
    setPhotoFile(undefined);
    setPhotoPreview(undefined);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }, [enfant?.id]);

  // ------- Photo -------
  const [photoFile, setPhotoFile] = useState<File | undefined>();
  const [photoPreview, setPhotoPreview] = useState<string | undefined>();
  const [showWebcamModal, setShowWebcamModal] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const existingPhotoUrl = useMemo(
    () => enfantService.photoUrl(form.photo, true),
    [form.photo]
  );

  useEffect(() => {
    if (!photoFile) { setPhotoPreview(undefined); return; }
    const url = URL.createObjectURL(photoFile);
    setPhotoPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [photoFile]);

  const update = <K extends keyof Enfant>(k: K, v: Enfant[K]) => setForm((f) => ({ ...f, [k]: v }));

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    setPhotoFile(f || undefined);
  };

  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const payload: Enfant = {
      ...form,
      datenaiss: form.datenaiss ? toIsoDate(form.datenaiss) : '',
      stat: form.stat?.trim() || undefined,
      reference: form.reference?.trim() || undefined,
    };

    setSubmitting(true);
    try {
      await onSubmit(payload, photoFile);
    } finally {
      setSubmitting(false);
    }
  };

  const handleClearPhotoSelection = () => {
    setPhotoFile(undefined);
    setPhotoPreview(undefined);
    if (fileInputRef.current) fileInputRef.current.value = '';
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
            {isEdit ? 'Modifier un enfant' : 'Nouvel enfant'}
          </h2>
          <button onClick={onCancel} className="p-2 hover:bg-slate-100 rounded-lg transition-colors" aria-label="Fermer">
            <X className="w-6 h-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6">
          {/* Photo — même disposition que formulaire Agent */}
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
              <button type="button" onClick={() => fileInputRef.current?.click()} className="flex items-center space-x-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors">
                <Upload className="w-4 h-4" />
                <span className="text-sm font-medium">{photoPreview || existingPhotoUrl ? 'Changer une photo' : 'Choisir une photo'}</span>
              </button>
              <button type="button" onClick={() => setShowWebcamModal(true)} className="flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors" title="Prendre une photo avec la webcam">
                <Camera className="w-4 h-4" />
                <span className="text-sm font-medium">Prendre une photo</span>
              </button>
              {photoPreview && (
                <button type="button" onClick={handleClearPhotoSelection} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">Annuler</button>
              )}
            </div>
          </div>

          {/* Identité */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div>
              <label className={labelClass}>Nom <span className="text-red-500">*</span></label>
              <input value={form.nomEnfant} onChange={(e) => update('nomEnfant', e.target.value)} className={inputClass} required />
            </div>
            <div>
              <label className={labelClass}>Postnom <span className="text-red-500">*</span></label>
              <input value={form.postnomEnfant} onChange={(e) => update('postnomEnfant', e.target.value)} className={inputClass} required />
            </div>
            <div>
              <label className={labelClass}>Prénom</label>
              <input value={form.prenomEnfant || ''} onChange={(e) => update('prenomEnfant', e.target.value)} className={inputClass} />
            </div>
          </div>

          {/* Genre, Date, Catégorie */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div>
              <label className={labelClass}>Genre</label>
              <select value={form.genre} onChange={(e) => update('genre', e.target.value as Enfant['genre'])} className={inputClass}>
                <option value="M">Masculin</option>
                <option value="F">Féminin</option>
              </select>
            </div>
            <div>
              <label className={labelClass}>Date de naissance</label>
              <input type="date" value={(form.datenaiss as string) || ''} onChange={(e) => update('datenaiss', e.target.value)} className={inputClass} />
              {form.datenaiss && getAgeFromDate(form.datenaiss) !== null && (
                <p className="mt-1 text-sm text-slate-500">Âge : {getAgeFromDate(form.datenaiss)} ans</p>
              )}
            </div>
            <div>
              <label className={labelClass}>Catégorie</label>
              <select value={form.categorie} onChange={(e) => update('categorie', e.target.value as Enfant['categorie'])} className={inputClass}>
                <option value="LEGITIME">Légitime</option>
                <option value="ADOPTIF">Adoptif</option>
              </select>
            </div>
            <div>
              <label className={labelClass}>Statut</label>
              <input value={form.stat || ''} onChange={(e) => update('stat', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Référence</label>
              <input value={form.reference || ''} onChange={(e) => update('reference', e.target.value)} className={inputClass} />
            </div>
          </div>

          <div className="flex justify-end space-x-3 pt-6 border-t border-slate-200">
            <button type="button" onClick={onCancel} className="px-6 py-2 border border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50 transition-colors">Annuler</button>
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
