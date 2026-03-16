// Composant réutilisable pour la capture photo par webcam (comme dans AgentForm).
import React, { useEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';

interface WebcamCaptureModalProps {
  open: boolean;
  onClose: () => void;
  onCapture: (file: File) => void;
  title?: string;
}

export const WebcamCaptureModal: React.FC<WebcamCaptureModalProps> = ({
  open,
  onClose,
  onCapture,
  title = 'Prendre une photo (webcam)',
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const [error, setError] = useState<string | null>(null);

  const close = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
    setError(null);
    onClose();
  };

  useEffect(() => {
    if (!open) return;
    setError(null);
    const video = videoRef.current;
    if (!video) return;

    if (!navigator.mediaDevices?.getUserMedia) {
      const isSecure = typeof window !== 'undefined' && (window.isSecureContext || window.location?.hostname === 'localhost' || window.location?.hostname === '127.0.0.1');
      setError(
        isSecure
          ? "Votre navigateur ne prend pas en charge l'accès à la caméra."
          : "La caméra n'est disponible qu'en connexion sécurisée (HTTPS) ou sur localhost. Ouvrez l'application en https:// ou depuis ce même ordinateur en http://localhost:5173 pour utiliser la prise de photo."
      );
      return;
    }

    const constraints: MediaStreamConstraints = {
      video: { facingMode: 'environment', width: { ideal: 640 }, height: { ideal: 480 } },
    };
    navigator.mediaDevices
      .getUserMedia(constraints)
      .then((stream) => {
        streamRef.current = stream;
        video.srcObject = stream;
        video.play().catch(() => {});
      })
      .catch((err) => {
        setError(
          err?.message || "Impossible d'accéder à la caméra. Vérifiez les autorisations."
        );
      });
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((t) => t.stop());
        streamRef.current = null;
      }
    };
  }, [open]);

  const capture = () => {
    const video = videoRef.current;
    if (!video?.srcObject || video.readyState !== 4) return;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.drawImage(video, 0, 0);
    canvas.toBlob(
      (blob) => {
        if (!blob) return;
        const file = new File([blob], `photo-${Date.now()}.jpg`, {
          type: 'image/jpeg',
        });
        onCapture(file);
        close();
      },
      'image/jpeg',
      0.92
    );
  };

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/60"
      style={{
        paddingLeft: 'max(1rem, env(safe-area-inset-left))',
        paddingRight: 'max(1rem, env(safe-area-inset-right))',
      }}
      aria-modal="true"
      role="dialog"
      aria-labelledby="webcam-modal-title"
    >
      <div className="bg-white rounded-2xl shadow-2xl max-w-lg w-full overflow-hidden">
        <div className="px-4 py-3 sm:py-4 border-b border-slate-200 flex justify-between items-center">
          <h3
            id="webcam-modal-title"
            className="font-semibold text-slate-800 text-sm sm:text-base"
          >
            {title}
          </h3>
          <button
            type="button"
            onClick={close}
            className="min-w-[44px] min-h-[44px] flex items-center justify-center rounded-xl hover:bg-slate-100 active:bg-slate-200 text-slate-600"
            aria-label="Fermer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="p-4 sm:p-5">
          {error ? (
            <p className="text-red-600 text-sm">{error}</p>
          ) : (
            <>
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                className="w-full rounded-xl bg-slate-900 aspect-video object-cover"
              />
              <div className="mt-4 flex flex-col-reverse sm:flex-row gap-3 justify-end">
                <button
                  type="button"
                  onClick={close}
                  className="min-h-[44px] px-4 py-3 rounded-xl border border-slate-300 hover:bg-slate-50 active:bg-slate-100 text-slate-700 font-medium"
                >
                  Annuler
                </button>
                <button
                  type="button"
                  onClick={capture}
                  className="min-h-[44px] px-5 py-3 rounded-xl bg-[#800020] text-white font-semibold hover:bg-[#650018] active:bg-[#500014] transition-colors"
                >
                  Capturer
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
