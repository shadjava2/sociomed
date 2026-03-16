// src/components/EnfantFormSenateur.tsx
import React, { useState, useEffect, useRef } from 'react';
import { Enfant, enfantService } from '../services/enfantService';
import { X, Upload, User, Camera, Loader2 } from 'lucide-react';
import { WebcamCaptureModal } from './WebcamCaptureModal';

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

interface EnfantFormSenateurProps {
  enfant?: Enfant;
  onSubmit: (enfant: Enfant, photoFile?: File) => Promise<void>;
  onCancel: () => void;
}

export const EnfantFormSenateur: React.FC<EnfantFormSenateurProps> = ({
  enfant,
  onSubmit,
  onCancel,
}) => {
  const [formData, setFormData] = useState<Enfant>({
    nomEnfant: '',
    postnomEnfant: '',
    prenomEnfant: '',
    genre: 'M',
    datenaiss: '',
    categorie: 'LEGITIME',
    stat: '',
    reference: '',
    ...enfant,
  });

  const [photoFile, setPhotoFile] = useState<File | undefined>();
  const [photoPreview, setPhotoPreview] = useState<string | undefined>();
  const [showWebcamModal, setShowWebcamModal] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  // URL de la photo existante (serveur enfants)
  const existingPhotoUrl =
    enfant?.photo ? enfantService.photoUrl(enfant.photo, true) : undefined;

  useEffect(() => {
    // reset preview si on change d’enfant
    setPhotoFile(undefined);
    setPhotoPreview(undefined);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }, [enfant?.id]);

  const handlePhotoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) {
      setPhotoFile(undefined);
      setPhotoPreview(undefined);
      return;
    }
    setPhotoFile(file);
    const reader = new FileReader();
    reader.onloadend = () => setPhotoPreview(reader.result as string);
    reader.readAsDataURL(file);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setIsLoading(true);

    try {
      await onSubmit(formData, photoFile);
    } catch (err: any) {
      setError(err?.response?.data?.error || "Erreur lors de l'enregistrement");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <WebcamCaptureModal
        open={showWebcamModal}
        onClose={() => setShowWebcamModal(false)}
        onCapture={(file) => {
          setPhotoFile(file);
          setPhotoPreview(URL.createObjectURL(file));
          setShowWebcamModal(false);
        }}
        title="Prendre une photo (webcam)"
      />
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-slate-200 px-6 py-4 flex justify-between items-center">
          <h2 className="text-2xl font-bold text-slate-800">
            {enfant ? "Modifier l'enfant" : 'Nouvel enfant'}
          </h2>
          <button onClick={onCancel} className="p-2 hover:bg-slate-100 rounded-lg">
            <X className="w-6 h-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6">
          {error && (
            <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          {/* Photo — même disposition que formulaire Agent */}
          <div className="mb-6">
            <label className="block text-sm font-medium text-slate-700 mb-2">Photo</label>
            <div className="flex flex-wrap items-center gap-3">
              {photoPreview ? (
                <img src={photoPreview} alt="" className="w-24 h-24 rounded-lg object-cover border-2 border-slate-200" />
              ) : existingPhotoUrl ? (
                <img
                  src={existingPhotoUrl}
                  alt=""
                  className="w-24 h-24 rounded-lg object-cover border-2 border-slate-200"
                  onError={(e) => { (e.currentTarget as HTMLImageElement).style.display = 'none'; }}
                />
              ) : (
                <div className="w-24 h-24 rounded-lg bg-slate-100 flex items-center justify-center">
                  <User className="w-12 h-12 text-slate-400" />
                </div>
              )}
              <input ref={fileInputRef} type="file" accept="image/*" onChange={handlePhotoChange} className="hidden" />
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
                <button
                  type="button"
                  onClick={() => {
                    setPhotoFile(undefined);
                    setPhotoPreview(undefined);
                    if (fileInputRef.current) fileInputRef.current.value = '';
                  }}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
                >
                  Annuler
                </button>
              )}
            </div>
          </div>

          {/* Identité */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Nom <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.nomEnfant}
                onChange={(e) => setFormData({ ...formData, nomEnfant: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Postnom <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.postnomEnfant}
                onChange={(e) => setFormData({ ...formData, postnomEnfant: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Prénom</label>
              <input
                type="text"
                value={formData.prenomEnfant || ''}
                onChange={(e) => setFormData({ ...formData, prenomEnfant: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          {/* Genre / date / catégorie */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Genre <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.genre}
                onChange={(e) => setFormData({ ...formData, genre: e.target.value as 'M' | 'F' })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              >
                <option value="M">Masculin</option>
                <option value="F">Féminin</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Date de naissance <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                value={formData.datenaiss}
                onChange={(e) => setFormData({ ...formData, datenaiss: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              />
              {formData.datenaiss && getAgeFromDate(formData.datenaiss) !== null && (
                <p className="mt-1 text-sm text-slate-500">Âge : {getAgeFromDate(formData.datenaiss)} ans</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Catégorie <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.categorie}
                onChange={(e) =>
                  setFormData({ ...formData, categorie: e.target.value as 'LEGITIME' | 'ADOPTIF' })
                }
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              >
                <option value="LEGITIME">Légitime</option>
                <option value="ADOPTIF">Adoptif</option>
              </select>
            </div>
          </div>

          {/* Référence / Stat */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Référence</label>
              <input
                type="text"
                value={formData.reference || ''}
                onChange={(e) => setFormData({ ...formData, reference: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Statut</label>
              <input
                type="text"
                value={formData.stat || ''}
                onChange={(e) => setFormData({ ...formData, stat: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          {/* Actions */}
          <div className="flex justify-end space-x-3 pt-6 border-t border-slate-200">
            <button
              type="button"
              onClick={onCancel}
              className="px-6 py-2 border border-slate-300 text-slate-700 rounded-lg hover:bg-slate-50"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="inline-flex items-center justify-center gap-2 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-5 h-5 shrink-0 animate-spin" aria-hidden />
                  <span>Enregistrement…</span>
                </>
              ) : (
                'Enregistrer'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
