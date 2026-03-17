// src/components/PdfViewerModal.tsx
import React, { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import {
  X,
  Printer,
  Download,
  ExternalLink,
  Maximize2,
  Minimize2,
} from 'lucide-react';

export type PdfViewerModalProps = {
  open: boolean;
  title?: string;
  filename: string;
  blob: Blob | null;
  onClose: () => void;
};

export const PdfViewerModal: React.FC<PdfViewerModalProps> = ({
  open,
  title,
  filename,
  blob,
  onClose,
}) => {
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [fullscreen, setFullscreen] = useState(false);

  useEffect(() => {
    if (!open || !blob) {
      if (blobUrl) {
        URL.revokeObjectURL(blobUrl);
        setBlobUrl(null);
      }
      return;
    }
    const url = URL.createObjectURL(blob);
    setBlobUrl(url);
    return () => {
      URL.revokeObjectURL(url);
      setBlobUrl(null);
    };
  }, [open, blob]);

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (fullscreen) setFullscreen(false);
        else onClose();
      }
    };
    if (open) globalThis.addEventListener('keydown', onKeyDown);
    return () => globalThis.removeEventListener('keydown', onKeyDown);
  }, [open, fullscreen, onClose]);

  const handlePrint = () => {
    if (!blobUrl) return;
    const iframe = iframeRef.current;
    if (iframe?.contentWindow) {
      try {
        iframe.contentWindow.focus();
        iframe.contentWindow.print();
      } catch {
        // En PWA / certains navigateurs, imprimer via l’iframe peut échouer : proposer d’enregistrer puis d’imprimer.
        if (typeof globalThis !== 'undefined' && globalThis.document) {
          const a = document.createElement('a');
          a.href = blobUrl;
          a.download = filename || 'document.pdf';
          document.body.appendChild(a);
          a.click();
          a.remove();
        }
      }
    }
  };

  const handleDownload = () => {
    if (!blobUrl || !blob) return;
    const a = document.createElement('a');
    a.href = blobUrl;
    a.download = filename || 'document.pdf';
    document.body.appendChild(a);
    a.click();
    a.remove();
  };

  const handleOpenInNewTab = () => {
    if (!blobUrl) return;
    window.open(blobUrl, '_blank', 'noopener,noreferrer');
  };

  if (!open) return null;

  const displayTitle = title || filename || 'Rapport PDF';

  const modalContent = (
    <div
      className="fixed inset-0 z-[99999] flex items-center justify-center p-4 bg-black/60"
      style={{ isolation: 'isolate' }}
      onClick={(e) => (e.target === e.currentTarget && !fullscreen ? onClose() : null)}
      onKeyDown={(e) => (e.key === 'Escape' && !fullscreen ? onClose() : null)}
      role="presentation"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={displayTitle}
        className={`
          bg-white rounded-2xl shadow-2xl overflow-hidden flex flex-col
          ${fullscreen ? 'fixed inset-4 z-[100000] rounded-xl' : 'w-[min(1100px,96vw)] max-h-[92vh]'}
        `}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Barre d'outils */}
        <div className="flex items-center justify-between gap-3 px-4 py-3 border-b border-slate-200 bg-slate-50 shrink-0">
          <div className="min-w-0 flex items-center gap-2">
            <span className="font-semibold text-slate-800 truncate" title={displayTitle}>
              {displayTitle}
            </span>
            <span className="text-xs text-slate-500 truncate hidden sm:inline">
              {filename}
            </span>
          </div>

          <div className="flex items-center gap-1">
            <button
              type="button"
              onClick={handlePrint}
              className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-slate-200 hover:bg-white hover:border-slate-300 text-slate-700 text-sm font-medium transition-colors"
              title="Imprimer"
            >
              <Printer className="w-4 h-4" />
              <span className="hidden sm:inline">Imprimer</span>
            </button>
            <button
              type="button"
              onClick={handleDownload}
              className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-slate-200 hover:bg-white hover:border-slate-300 text-slate-700 text-sm font-medium transition-colors"
              title="Enregistrer"
            >
              <Download className="w-4 h-4" />
              <span className="hidden sm:inline">Enregistrer</span>
            </button>
            <button
              type="button"
              onClick={handleOpenInNewTab}
              className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-slate-200 hover:bg-white hover:border-slate-300 text-slate-700 text-sm font-medium transition-colors"
              title="Ouvrir dans un nouvel onglet"
            >
              <ExternalLink className="w-4 h-4" />
              <span className="hidden sm:inline">Ouvrir</span>
            </button>
            <button
              type="button"
              onClick={() => setFullscreen((v) => !v)}
              className="inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-slate-200 hover:bg-white hover:border-slate-300 text-slate-700 text-sm font-medium transition-colors"
              title={fullscreen ? 'Réduire' : 'Plein écran'}
            >
              {fullscreen ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="p-2 rounded-lg hover:bg-slate-200 text-slate-600 transition-colors"
              title="Fermer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Contenu PDF */}
        <div className="flex-1 min-h-0 bg-slate-100 flex flex-col">
          {!blobUrl && (
            <div className="flex-1 flex items-center justify-center text-slate-500">
              Chargement du document…
            </div>
          )}
          {blobUrl && (
            <iframe
              ref={iframeRef}
              title={displayTitle}
              src={blobUrl}
              className="w-full flex-1 min-h-[400px] border-0"
            />
          )}
        </div>
      </div>
    </div>
  );

  return createPortal(modalContent, document.body);
};
