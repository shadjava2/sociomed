import React, { useEffect, useMemo, useRef, useState } from 'react';
import { conjointService, Conjoint } from '../services/conjointService';
import { X, Upload, User, Camera, Loader2 } from 'lucide-react';
import { WebcamCaptureModal } from './WebcamCaptureModal';

type Props = {
  // soit on rattache à un agent, soit à un sénateur (un seul requis)
  agentId?: number;
  senateurId?: number;

  conjoint?: Conjoint; // si présent = édition
  onSubmit: (conjoint: Conjoint, photoFile?: File) => Promise<void> | void;
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

export const ConjointForm: React.FC<Props> = ({ agentId, senateurId, conjoint, onSubmit, onCancel }) => {
  const isEdit = Boolean(conjoint?.id);

  const [form, setForm] = useState<Conjoint>({
    nom: conjoint?.nom || '',
    postnom: conjoint?.postnom || '',
    prenom: conjoint?.prenom || '',
    genre: conjoint?.genre || 'M',
    datenaiss: toIsoDate(conjoint?.datenaiss),
    profession: conjoint?.profession || '',
    telephone: conjoint?.telephone || '',
    email: conjoint?.email || '',
    photo: conjoint?.photo || undefined,
  });

  useEffect(() => {
    setForm({
      nom: conjoint?.nom || '',
      postnom: conjoint?.postnom || '',
      prenom: conjoint?.prenom || '',
      genre: conjoint?.genre || 'M',
      datenaiss: toIsoDate(conjoint?.datenaiss),
      profession: conjoint?.profession || '',
      telephone: conjoint?.telephone || '',
      email: conjoint?.email || '',
      photo: conjoint?.photo || undefined,
    });
    setPhotoFile(undefined);
    setPhotoPreview(undefined);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }, [conjoint?.id]);

  // ------- Photo -------
  const [photoFile, setPhotoFile] = useState<File | undefined>();
  const [photoPreview, setPhotoPreview] = useState<string | undefined>();
  const [showWebcamModal, setShowWebcamModal] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const existingPhotoUrl = useMemo(
    () => conjointService.photoUrl(form.photo, true),
    [form.photo]
  );

  useEffect(() => {
    if (!photoFile) { setPhotoPreview(undefined); return; }
    const url = URL.createObjectURL(photoFile);
    setPhotoPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [photoFile]);

  const update = <K extends keyof Conjoint>(k: K, v: Conjoint[K]) => setForm((f) => ({ ...f, [k]: v }));

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    setPhotoFile(f || undefined);
  };

  const handleClearPhotoSelection = () => {
    setPhotoFile(undefined);
    setPhotoPreview(undefined);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  // ------- Submit -------
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const payload: Conjoint = {
      ...form,
      datenaiss: form.datenaiss ? toIsoDate(form.datenaiss) : undefined,
      profession: form.profession?.trim() || undefined,
      telephone: form.telephone?.trim() || undefined,
      email: form.email?.trim() || undefined,
    };

    setSubmitting(true);
    try {
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
            {isEdit ? 'Modifier le conjoint' : 'Nouveau conjoint'}
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

          {/* Genre, Date, Profession */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div>
              <label className={labelClass}>Genre</label>
              <select value={form.genre} onChange={(e) => update('genre', e.target.value as Conjoint['genre'])} className={inputClass}>
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
              <label className={labelClass}>Profession</label>
              <input value={form.profession || ''} onChange={(e) => update('profession', e.target.value)} className={inputClass} />
            </div>
          </div>

          {/* Contact */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <div>
              <label className={labelClass}>Téléphone</label>
              <input type="tel" value={form.telephone || ''} onChange={(e) => update('telephone', e.target.value)} className={inputClass} />
            </div>
            <div>
              <label className={labelClass}>Email</label>
              <input type="email" value={form.email || ''} onChange={(e) => update('email', e.target.value)} className={inputClass} />
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
