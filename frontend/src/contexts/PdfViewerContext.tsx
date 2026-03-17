// src/contexts/PdfViewerContext.tsx
import React, { createContext, useContext, useState, useCallback, useMemo } from 'react';
import { PdfViewerModal } from '../components/PdfViewerModal';

type PdfViewerContextType = {
  openPdf: (blob: Blob, filename: string, title?: string) => void;
};

const PdfViewerContext = createContext<PdfViewerContextType | null>(null);

export const PdfViewerProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [open, setOpen] = useState(false);
  const [blob, setBlob] = useState<Blob | null>(null);
  const [filename, setFilename] = useState('');
  const [title, setTitle] = useState<string | undefined>(undefined);

  const openPdf = useCallback((b: Blob, fname: string, t?: string) => {
    setBlob(b);
    setFilename(fname);
    setTitle(t);
    setOpen(true);
  }, []);

  const close = useCallback(() => {
    setOpen(false);
    setBlob(null);
    setFilename('');
    setTitle(undefined);
  }, []);

  const value = useMemo(() => ({ openPdf }), [openPdf]);
  return (
    <PdfViewerContext.Provider value={value}>
      {children}
      <PdfViewerModal
        open={open}
        title={title}
        filename={filename}
        blob={blob}
        onClose={close}
      />
    </PdfViewerContext.Provider>
  );
};

export function usePdfViewer(): PdfViewerContextType {
  const ctx = useContext(PdfViewerContext);
  if (!ctx) {
    throw new Error('usePdfViewer must be used within PdfViewerProvider');
  }
  return ctx;
}
