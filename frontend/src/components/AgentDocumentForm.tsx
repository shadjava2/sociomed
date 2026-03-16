// src/components/AgentDocumentForm.tsx
import React, { useMemo, useState } from 'react';
import { X, UploadCloud, FileText, Loader2 } from 'lucide-react';
import type { AgentDocumentType } from '../services/agentDocumentService';

type Props = {
  open: boolean;
  onClose: () => void;
  onSubmit: (formData: FormData) => Promise<void>;
};

const TYPES: { value: AgentDocumentType; label: string }[] = [
  { value: 'CARTE_SERVICE', label: 'Carte de service' },
  { value: 'CNI', label: 'CNI' },
  { value: 'PASSEPORT', label: 'Passeport' },
  { value: 'MATRICULE', label: 'Matricule' },
  { value: 'ARRETE', label: 'Arrêté / Décision' },
  { value: 'DIPLOME', label: 'Diplôme' },
  { value: 'ATTESTATION', label: 'Attestation' },
  { value: 'CONTRAT', label: 'Contrat' },
  { value: 'CERTIFICAT_MEDICAL', label: 'Certificat médical' },
  { value: 'AUTRE', label: 'Autre' },
];

export const AgentDocumentForm: React.FC<Props> = ({ open, onClose, onSubmit }) => {
  const [type, setType] = useState<AgentDocumentType>('CARTE_SERVICE');
  const [title, setTitle] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string>('');

  const accept = useMemo(() => 'application/pdf,image/jpeg,image/png,image/webp', []);

  if (!open) return null;

  const handleSubmit = async () => {
    setErr('');
    if (!file) {
      setErr('Veuillez sélectionner un fichier (PDF ou image).');
      return;
    }

    const fd = new FormData();
    fd.append('file', file);
    fd.append('type', type);
    if (title.trim()) fd.append('title', title.trim());

    try {
      setLoading(true);
      await onSubmit(fd);
      setLoading(false);
      onClose();
    } catch (e: any) {
      setLoading(false);
      setErr(e?.response?.data?.message || e?.message || 'Erreur lors de l’upload.');
    }
  };

  return (
    <div className="fixed inset-0 z-[9998] bg-black/60 flex items-center justify-center p-4" onClick={onClose}>
      <div className="w-[min(640px,96vw)] bg-white rounded-2xl shadow-xl overflow-hidden" onClick={(e) => e.stopPropagation()}>
        <div className="px-4 py-3 border-b flex items-center justify-between">
          <div className="font-semibold text-slate-800 flex items-center gap-2">
            <FileText className="w-5 h-5" />
            Ajouter un document
          </div>
          <button onClick={onClose} className="p-2 rounded-lg hover:bg-slate-100">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-4 space-y-4">
          {err && (
            <div className="p-3 rounded-lg bg-red-50 text-red-700 text-sm border border-red-200">
              {err}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-700">Type</label>
              <select
                value={type}
                onChange={(e) => setType(e.target.value as AgentDocumentType)}
                className="w-full border border-slate-200 rounded-lg px-3 py-2"
              >
                {TYPES.map((t) => (
                  <option key={t.value} value={t.value}>{t.label}</option>
                ))}
              </select>
            </div>

            <div className="space-y-1">
              <label className="text-sm font-medium text-slate-700">Titre (optionnel)</label>
              <input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ex: Arrêté de nomination"
                className="w-full border border-slate-200 rounded-lg px-3 py-2"
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-700">Fichier</label>
            <input
              type="file"
              accept={accept}
              onChange={(e) => setFile(e.target.files?.[0] || null)}
              className="w-full border border-slate-200 rounded-lg px-3 py-2"
            />
            <div className="text-xs text-slate-500">Formats: PDF, JPG, PNG, WEBP</div>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              onClick={onClose}
              disabled={loading}
              className="px-4 py-2 rounded-lg border border-slate-200 hover:bg-slate-50"
            >
              Annuler
            </button>
            <button
              onClick={handleSubmit}
              disabled={loading}
              className="inline-flex items-center justify-center gap-2 px-4 py-2 rounded-lg bg-slate-900 text-white hover:bg-slate-800 disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {loading ? (
                <>
                  <Loader2 className="w-5 h-5 shrink-0 animate-spin" aria-hidden />
                  <span>Envoi…</span>
                </>
              ) : (
                <>
                  <UploadCloud className="w-4 h-4" />
                  Uploader
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
