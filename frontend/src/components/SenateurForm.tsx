import React, { useState, useEffect } from 'react';
import { Senateur } from '../services/senateurService';
import { API_BASE_URL } from '../config/api';
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

interface SenateurFormProps {
  senateur?: Senateur;
  onSubmit: (senateur: Senateur, photoFile?: File) => Promise<void>;
  onCancel: () => void;
}

export const SenateurForm: React.FC<SenateurFormProps> = ({ senateur, onSubmit, onCancel }) => {
  const [formData, setFormData] = useState<Senateur>({
    nom: '',
    postnom: '',
    prenom: '',
    genre: 'M',
    datenaiss: '',
    statut: 'EN_ACTIVITE',
    telephone: '',
    email: '',
    legislature: '',
    adresse: '',
    photo: '',
  });

  const [photoFile, setPhotoFile] = useState<File | undefined>();
  const [photoPreview, setPhotoPreview] = useState<string | undefined>();
  const [showWebcamModal, setShowWebcamModal] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  // URL helper: utilise l'endpoint public d’images du backend
  const photoUrl = (file?: string) =>
    file ? `${API_BASE_URL}/api/senateurs/photos/${file}` : undefined;

  // Synchroniser le formulaire et l’aperçu à chaque changement de "senateur" prop
  useEffect(() => {
    if (senateur) {
      setFormData({
        nom: senateur.nom || '',
        postnom: senateur.postnom || '',
        prenom: senateur.prenom || '',
        genre: (senateur.genre as 'M' | 'F') || 'M',
        datenaiss: senateur.datenaiss || '',
        statut: (senateur.statut as 'EN_ACTIVITE' | 'HONORAIRE') || 'EN_ACTIVITE',
        telephone: senateur.telephone || '',
        email: senateur.email || '',
        legislature: senateur.legislature || '',
        adresse: senateur.adresse || '',
        photo: senateur.photo || '',
        id: senateur.id, // on conserve l'id si présent
      });

      // Si une photo existe côté serveur, on l’affiche (avec anti-cache)
      if (senateur.photo) {
        setPhotoPreview(`${photoUrl(senateur.photo)}?v=${Date.now()}`);
      } else {
        setPhotoPreview(undefined);
      }

      // reset file sélectionné quand on passe d’un sénateur à l’autre
      setPhotoFile(undefined);
    } else {
      // Création : reset propre
      setFormData({
        nom: '',
        postnom: '',
        prenom: '',
        genre: 'M',
        datenaiss: '',
        statut: 'EN_ACTIVITE',
        telephone: '',
        email: '',
        legislature: '',
        adresse: '',
        photo: '',
      });
      setPhotoFile(undefined);
      setPhotoPreview(undefined);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [senateur]);

  const handlePhotoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setPhotoFile(file);
      // preview immédiat local
      const reader = new FileReader();
      reader.onloadend = () => setPhotoPreview(reader.result as string);
      reader.readAsDataURL(file);
    }
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
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-y-auto">
        <div className="sticky top-0 bg-white border-b border-slate-200 px-6 py-4 flex justify-between items-center">
          <h2 className="text-2xl font-bold text-slate-800">
            {senateur ? 'Modifier le sénateur' : 'Nouveau sénateur'}
          </h2>
          <button onClick={onCancel} className="p-2 hover:bg-slate-100 rounded-lg transition-colors">
            <X className="w-6 h-6" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6">
          {error && (
            <div className="mb-6 bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
              {error}
            </div>
          )}

          {/* Photo */}
          <div className="mb-6">
            <label className="block text-sm font-medium text-slate-700 mb-3">Photo</label>
            <div className="flex flex-wrap items-center gap-3">
              {photoPreview ? (
                <img
                  src={photoPreview}
                  alt="Preview"
                  className="w-24 h-24 rounded-lg object-cover border-2 border-slate-200"
                  onError={() => setPhotoPreview(undefined)}
                />
              ) : (
                <div className="w-24 h-24 rounded-lg bg-slate-100 flex items-center justify-center">
                  <User className="w-12 h-12 text-slate-400" />
                </div>
              )}
              <label className="cursor-pointer">
                <div className="flex items-center space-x-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors">
                  <Upload className="w-4 h-4" />
                  <span className="text-sm font-medium">Choisir une photo</span>
                </div>
                <input type="file" accept="image/*" onChange={handlePhotoChange} className="hidden" />
              </label>
              <button
                type="button"
                onClick={() => setShowWebcamModal(true)}
                className="flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 rounded-lg transition-colors"
                title="Prendre une photo avec la webcam"
              >
                <Camera className="w-4 h-4" />
                <span className="text-sm font-medium">Prendre une photo</span>
              </button>
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
                value={formData.nom || ''}
                onChange={(e) => setFormData({ ...formData, nom: e.target.value })}
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
                value={formData.postnom || ''}
                onChange={(e) => setFormData({ ...formData, postnom: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Prénom <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={formData.prenom || ''}
                onChange={(e) => setFormData({ ...formData, prenom: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              />
            </div>
          </div>

          {/* Genre / date / statut */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">
                Genre <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.genre as 'M' | 'F'}
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
                value={formData.datenaiss || ''}
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
                Statut <span className="text-red-500">*</span>
              </label>
              <select
                value={formData.statut as 'EN_ACTIVITE' | 'HONORAIRE'}
                onChange={(e) =>
                  setFormData({ ...formData, statut: e.target.value as 'EN_ACTIVITE' | 'HONORAIRE' })
                }
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              >
                <option value="EN_ACTIVITE">En activité</option>
                <option value="HONORAIRE">Honoraire</option>
              </select>
            </div>
          </div>

          {/* Contact */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Téléphone</label>
              <input
                type="tel"
                value={formData.telephone || ''}
                onChange={(e) => setFormData({ ...formData, telephone: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Email</label>
              <input
                type="email"
                value={formData.email || ''}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
            </div>
          </div>

          {/* Divers */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Législature</label>
              <input
                type="text"
                value={formData.legislature || ''}
                onChange={(e) => setFormData({ ...formData, legislature: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Ex: 2019-2024"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Adresse</label>
              <input
                type="text"
                value={formData.adresse || ''}
                onChange={(e) => setFormData({ ...formData, adresse: e.target.value })}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              />
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
              disabled={isLoading}
              className="inline-flex items-center justify-center gap-2 px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-70 disabled:cursor-not-allowed"
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
