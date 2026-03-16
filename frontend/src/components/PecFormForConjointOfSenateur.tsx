import React, { useEffect, useState } from 'react';
import { X, Stethoscope, Loader2 } from 'lucide-react';
import { useAppDialog } from '../contexts/AppDialogContext';
import { pecService, PecCreatePayload } from '../services/pecService';
import hopitalService, { HopitalSummary } from '../services/hopitalService';

type Props = {
  conjointId: number; // id du conjoint (lié au sénateur côté back)
  parentSenateurName: string; // ex: "KABILA Joseph"
  onCreated: (pecId: number) => void | Promise<void>;
  onCancel: () => void;
};

export const PecFormForConjointOfSenateur: React.FC<Props> = ({
  conjointId,
  parentSenateurName,
  onCreated,
  onCancel,
}) => {
  const { showError, showInfo } = useAppDialog();
  const [form, setForm] = useState<PecCreatePayload>({
    hopitalId: 0,
    etablissement: '',
    motif: '',
    actes: '',
    remarque: '',
    adresseMalade: '',
    qualiteMalade: `Conjoint(e) du Sénateur ${parentSenateurName}`.trim(),
  });

  const [submitting, setSubmitting] = useState(false);
  const [hopitaux, setHopitaux] = useState<HopitalSummary[]>([]);
  const [loadingHopitaux, setLoadingHopitaux] = useState(false);
  const [hopitauxError, setHopitauxError] = useState<string | null>(null);

  const update = <K extends keyof PecCreatePayload>(k: K, v: PecCreatePayload[K]) =>
    setForm((f) => ({ ...f, [k]: v }));

  useEffect(() => {
    const loadHopitaux = async () => {
      setLoadingHopitaux(true);
      setHopitauxError(null);
      try {
        const res = await hopitalService.listActifs({
          page: 0,
          size: 200,
          sortBy: 'nom',
          sortDir: 'asc',
        });
        setHopitaux(res.content ?? []);
      } catch (err: any) {
        setHopitauxError(
          err?.response?.data?.error ||
            err?.message ||
            "Impossible de charger les hôpitaux."
        );
      } finally {
        setLoadingHopitaux(false);
      }
    };

    loadHopitaux();
  }, []);

  const handleHopitalChange = (value: string) => {
    const selectedId = Number(value);
    const selectedHopital = hopitaux.find((h) => h.id === selectedId);

    setForm((f) => ({
      ...f,
      hopitalId: selectedId,
      etablissement:
        !f.etablissement?.trim() && selectedHopital
          ? selectedHopital.nom
          : f.etablissement,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!form.hopitalId || form.hopitalId <= 0) {
      showInfo("Veuillez choisir un hôpital.", "Champ requis");
      return;
    }

    setSubmitting(true);
    try {
      const created = await pecService.createForConjoint(conjointId, form);
      await onCreated(created.id);
    } catch (err: any) {
      showError(
        err?.response?.data?.error ||
          err?.message ||
          "Erreur lors de la création de la PEC."
      );
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
            Nouvelle PEC —{' '}
            <span className="text-slate-600">
              Conjoint(e) du Sénateur {parentSenateurName}
            </span>
          </h2>
          <button
            type="button"
            onClick={onCancel}
            className="p-2 rounded-lg hover:bg-slate-100 text-slate-600"
            aria-label="Fermer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-5 space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs text-slate-500 mb-1">Hôpital *</label>
              <select
                value={form.hopitalId || ''}
                onChange={(e) => handleHopitalChange(e.target.value)}
                className="w-full px-3 py-2 border rounded-lg bg-white"
                required
                disabled={loadingHopitaux}
              >
                <option value="">
                  {loadingHopitaux
                    ? 'Chargement des hôpitaux...'
                    : '-- Sélectionner un hôpital --'}
                </option>

                {hopitaux.map((h) => (
                  <option key={h.id} value={h.id}>
                    {h.nom}
                  </option>
                ))}
              </select>

              {hopitauxError && (
                <p className="mt-1 text-xs text-red-600">{hopitauxError}</p>
              )}
            </div>

            <div>
              <label className="block text-xs text-slate-500 mb-1">
                Établissement (facultatif)
              </label>
              <input
                type="text"
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
                type="text"
                value={form.qualiteMalade || ''}
                onChange={(e) => update('qualiteMalade', e.target.value)}
                className="w-full px-3 py-2 border rounded-lg"
                placeholder='ex: "Conjoint(e) du Sénateur X Y"'
              />
            </div>

            <div>
              <label className="block text-xs text-slate-500 mb-1">
                Adresse du malade (facultatif)
              </label>
              <input
                type="text"
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
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 rounded-lg border border-slate-300 hover:bg-slate-50"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={submitting || loadingHopitaux}
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