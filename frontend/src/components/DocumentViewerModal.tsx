// src/components/DocumentViewerModal.tsx
import React, { useEffect } from 'react';
import { X, ExternalLink, Download } from 'lucide-react';

type Props = {
  open: boolean;
  title?: string;
  blobUrl?: string;
  contentType?: string;
  onClose: () => void;
  onDownload?: () => void;
};

export const DocumentViewerModal: React.FC<Props> = ({ open, title, blobUrl, contentType, onClose, onDownload }) => {
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    if (open) window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  const ct = (contentType || '').toLowerCase();
  const isPdf = ct.includes('pdf');
  const isImage = ct.startsWith('image/');

  return (
    <div className="fixed inset-0 z-[9999] bg-black/60 flex items-center justify-center p-4" onClick={onClose}>
      <div className="w-[min(1100px,96vw)] h-[min(92vh,900px)] bg-white rounded-2xl shadow-xl overflow-hidden flex flex-col" onClick={(e) => e.stopPropagation()}>
        <div className="px-4 py-3 border-b flex items-center justify-between gap-3">
          <div className="min-w-0">
            <div className="font-semibold text-slate-800 truncate">{title || 'Document'}</div>
            <div className="text-xs text-slate-500 truncate">{contentType || ''}</div>
          </div>

          <div className="flex items-center gap-2">
            {blobUrl && (
              <a
                href={blobUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-slate-200 hover:bg-slate-50 text-sm"
                title="Ouvrir dans un nouvel onglet"
              >
                <ExternalLink className="w-4 h-4" />
                Ouvrir
              </a>
            )}
            {onDownload && (
              <button
                onClick={onDownload}
                className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-slate-200 hover:bg-slate-50 text-sm"
                title="Télécharger"
              >
                <Download className="w-4 h-4" />
                Télécharger
              </button>
            )}
            <button
              onClick={onClose}
              className="p-2 rounded-lg hover:bg-slate-100"
              title="Fermer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        <div className="flex-1 bg-slate-50">
          {!blobUrl && (
            <div className="h-full flex items-center justify-center text-slate-600">
              Aucun contenu à afficher
            </div>
          )}

          {blobUrl && isPdf && (
            <iframe
              title="PDF Viewer"
              src={blobUrl}
              className="w-full h-full"
            />
          )}

          {blobUrl && isImage && (
            <div className="w-full h-full overflow-auto flex items-center justify-center p-4">
              <img src={blobUrl} alt={title || 'document'} className="max-w-full max-h-full rounded-lg shadow" />
            </div>
          )}

          {blobUrl && !isPdf && !isImage && (
            <div className="h-full flex items-center justify-center text-slate-600 p-6 text-center">
              Type de document non prévisualisable ({contentType}). Utilise “Télécharger”.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
