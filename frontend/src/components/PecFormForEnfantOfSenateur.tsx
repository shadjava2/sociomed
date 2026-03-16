import React, { useState } from 'react';
import { X, Stethoscope, Loader2 } from 'lucide-react';
import { useAppDialog } from '../contexts/AppDialogContext';
import { pecService, PecCreatePayload } from '../services/pecService';

type Props = {
  enfantId: number;                     // id de l’enfant (lié au sénateur côté back)
  parentSenateurName: string;           // ex: "KABILA Joseph"
  enfantFullname: string;               // pour l’en-tête : "Nom Postnom Prénom" de l’enfant
  onCreated: (pecId: number) => void | Promise<void>;
  onCancel: () => void;
};

export const PecFormForEnfantOfSenateur: React.FC<Props> = ({
  enfantId,
  parentSenateurName,
  enfantFullname,
  onCreated,
  onCancel
}) => {
  const { showError, showInfo } = useAppDialog();
  const [form, setForm] = useState<PecCreatePayload>({
    hopitalId: 0,
    etablissement: '',
    motif: '',
    actes: '',
    remarque: '',
    adresseMalade: '',
    // qualité par défaut explicite pour l’impression
    qualiteMalade: `Enfant du Sénateur ${parentSenateurName}`.trim(),
  });
  const [submitting, setSubmitting] = useState(false);

  const update = <K extends keyof PecCreatePayload>(k: K, v: PecCreatePayload[K]) =>
    setForm((f) => ({ ...f, [k]: v }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.hopitalId || form.hopitalId <= 0) {
      showInfo("Veuillez choisir un hôpital.", "Champ requis");
      return;
    }
    setSubmitting(true);
    try {
      const created = await pecService.createForEnfant(enfantId, form);
      await onCreated(created.id);
    } catch (err: any) {
      showError(err?.response?.data?.error || err?.message || "Erreur lors de la création de la PEC.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start md:items-center justify-center bg-black/30 p-4 overflow-y-auto">
      <div className="w-full max-w-xl bg-white rounded-xl shadow-xl border border-slate-200">
        <div className="flex items-center justify-between px-5 py-4 border-b">
          <h2 className="text-lg font-semibold text-slate-800 flex items-center gap-2">
            <Stethoscope className="w-5 h-5" />
            Nouvelle PEC — <span className="text-slate-600">{enfantFullname}</span>
          </h2>
          <button onClick={onCancel} className="p-2 rounded-lg hover:bg-slate-100 text-slate-600" aria-label="Fermer">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-slate-500 mb-1">Hôpital ID *</label>
              <input
                type="number"
                value={form.hopitalId || ''}
                min={1}
                onChange={(e) => update('hopitalId', Number(e.target.value))}
                className="w-full px-3 py-2 border rounded-lg"
                required
              />
            </div>
            <div>
              <label className="block text-xs text-slate-500 mb-1">Établissement (facultatif)</label>
              <input
                value={form.etablissement || ''}
                onChange={(e) => update('etablissement', e.target.value)}
                className="w-full px-3 py-2 border rounded-lg"
                placeholder="Par défaut = nom de l'hôpital"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-slate-500 mb-1">Qualité (facultatif)</label>
              <input
                value={form.qualiteMalade || ''}
                onChange={(e) => update('qualiteMalade', e.target.value)}
                className="w-full px-3 py-2 border rounded-lg"
                placeholder='ex: "Enfant du Sénateur X Y"'
              />
            </div>
            <div>
              <label className="block text-xs text-slate-500 mb-1">Adresse du malade (facultatif)</label>
              <input
                value={form.adresseMalade || ''}
                onChange={(e) => update('adresseMalade', e.target.value)}
                className="w-full px-3 py-2 border rounded-lg"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs text-slate-500 mb-1">Motif / Diagnostic</label>
            <textarea
              value={form.motif || ''}
              onChange={(e) => update('motif', e.target.value)}
              className="w-full px-3 py-2 border rounded-lg"
              rows={3}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-slate-500 mb-1">Actes (facultatif)</label>
              <textarea
                value={form.actes || ''}
                onChange={(e) => update('actes', e.target.value)}
                className="w-full px-3 py-2 border rounded-lg"
                rows={2}
              />
            </div>
            <div>
              <label className="block text-xs text-slate-500 mb-1">Remarque (facultatif)</label>
              <textarea
                value={form.remarque || ''}
                onChange={(e) => update('remarque', e.target.value)}
                className="w-full px-3 py-2 border rounded-lg"
                rows={2}
              />
            </div>
          </div>

          <div className="flex items-center justify-end gap-2 pt-2">
            <button type="button" onClick={onCancel} className="px-4 py-2 rounded-lg border border-slate-300 hover:bg-slate-50">Annuler</button>
            <button
              type="submit"
              disabled={submitting}
              className="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {submitting ? (
                <>
                  <Loader2 className="w-5 h-5 shrink-0 animate-spin" aria-hidden />
                  <span>Enregistrement…</span>
                </>
              ) : (
                'Créer la PEC'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
