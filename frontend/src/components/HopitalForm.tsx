// src/components/HopitalForm.tsx
import React, { useState, useEffect } from 'react';
import { Loader2 } from 'lucide-react';
import type {
  CategorieHopital,
  HopitalCreateRequest,
  HopitalDetail,
  HopitalUpdateRequest,
} from '../services/hopitalService';

type Props = {
  hopital?: HopitalDetail; // si fourni => édition
  onSubmit: (data: HopitalCreateRequest | HopitalUpdateRequest) => Promise<void> | void;
  onCancel: () => void;
};

const CATEGORIES: CategorieHopital[] = ['PUBLIC', 'PRIVE', 'CONFESSIONNEL', 'AUTRE'];

export const HopitalForm: React.FC<Props> = ({ hopital, onSubmit, onCancel }) => {
  const [model, setModel] = useState<HopitalCreateRequest>({
    code: '',
    nom: '',
    categorie: 'PRIVE',
    actif: true,
  });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (hopital) {
      setModel({
        code: hopital.code ?? '',
        nom: hopital.nom ?? '',
        categorie: (hopital.categorie as CategorieHopital) ?? 'PRIVE',
        adresse: hopital.adresse ?? '',
        commune: hopital.commune ?? '',
        ville: hopital.ville ?? '',
        contactNom: hopital.contactNom ?? '',
        contactTelephone: hopital.contactTelephone ?? '',
        email: hopital.email ?? '',
        siteWeb: hopital.siteWeb ?? '',
        actif: hopital.actif ?? true,
        numeroConvention: hopital.numeroConvention ?? '',
        // LocalDateTime -> "yyyy-MM-ddTHH:mm" si dispo
        conventionDebut: hopital.conventionDebut ? hopital.conventionDebut.slice(0, 16) : undefined,
        conventionFin: hopital.conventionFin ? hopital.conventionFin.slice(0, 16) : undefined,
      });
    }
  }, [hopital]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    setModel((m) => ({
      ...m,
      [name]: type === 'checkbox'
        ? (e.target as HTMLInputElement).checked
        : value,
    }));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await onSubmit(model);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/30 z-50 flex items-center justify-center p-4">
      <div className="bg-white w-full max-w-3xl rounded-xl shadow-lg border border-slate-200">
        <div className="px-5 py-4 border-b">
          <h3 className="text-lg font-semibold">
            {hopital ? 'Modifier un hôpital' : 'Créer un hôpital'}
          </h3>
        </div>

        <form onSubmit={submit} className="p-5 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div>
              <label className="text-sm text-slate-600">Code</label>
              <input
                name="code"
                value={model.code}
                onChange={handleChange}
                required
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>

            <div className="md:col-span-2">
              <label className="text-sm text-slate-600">Nom</label>
              <input
                name="nom"
                value={model.nom}
                onChange={handleChange}
                required
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>

            <div>
              <label className="text-sm text-slate-600">Catégorie</label>
              <select
                name="categorie"
                value={model.categorie}
                onChange={handleChange}
                className="w-full px-3 py-2 border rounded-lg"
              >
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>{c}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-sm text-slate-600">Ville</label>
              <input
                name="ville"
                value={model.ville || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>

            <div>
              <label className="text-sm text-slate-600">Commune</label>
              <input
                name="commune"
                value={model.commune || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>

            <div className="md:col-span-3">
              <label className="text-sm text-slate-600">Adresse</label>
              <input
                name="adresse"
                value={model.adresse || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>

            <div>
              <label className="text-sm text-slate-600">Contact (Nom)</label>
              <input
                name="contactNom"
                value={model.contactNom || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>

            <div>
              <label className="text-sm text-slate-600">Contact (Téléphone)</label>
              <input
                name="contactTelephone"
                value={model.contactTelephone || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>

            <div>
              <label className="text-sm text-slate-600">Email</label>
              <input
                type="email"
                name="email"
                value={model.email || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>

            <div>
              <label className="text-sm text-slate-600">Site web</label>
              <input
                name="siteWeb"
                value={model.siteWeb || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>

            <div className="flex items-center gap-2">
              <input
                id="actif"
                name="actif"
                type="checkbox"
                checked={!!model.actif}
                onChange={handleChange}
              />
              <label htmlFor="actif" className="text-sm text-slate-700">Actif</label>
            </div>

            <div>
              <label className="text-sm text-slate-600">N° Convention</label>
              <input
                name="numeroConvention"
                value={model.numeroConvention || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>

            <div>
              <label className="text-sm text-slate-600">Convention début</label>
              <input
                type="datetime-local"
                name="conventionDebut"
                value={model.conventionDebut || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>

            <div>
              <label className="text-sm text-slate-600">Convention fin</label>
              <input
                type="datetime-local"
                name="conventionFin"
                value={model.conventionFin || ''}
                onChange={handleChange}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button type="button" onClick={onCancel} className="px-4 py-2 border rounded-lg">
              Annuler
            </button>
            <button
              type="submit"
              disabled={saving}
              className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {saving ? (
                <>
                  <Loader2 className="w-5 h-5 shrink-0 animate-spin" aria-hidden />
                  <span>Enregistrement…</span>
                </>
              ) : (
                hopital ? 'Mettre à jour' : 'Créer'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
