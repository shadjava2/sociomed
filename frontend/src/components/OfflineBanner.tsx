// src/components/OfflineBanner.tsx
import React from 'react';
import { WifiOff } from 'lucide-react';
import { useOnline } from '../hooks/useOnline';

export const OfflineBanner: React.FC = () => {
  const online = useOnline();

  if (online) return null;

  return (
    <div
      className="fixed top-0 left-0 right-0 z-50 bg-amber-600 text-white px-4 py-2 text-center text-sm font-medium flex items-center justify-center gap-2"
      role="status"
      aria-live="polite"
    >
      <WifiOff className="w-4 h-4 shrink-0" />
      Pas de connexion. Les données peuvent être indisponibles.
    </div>
  );
};
