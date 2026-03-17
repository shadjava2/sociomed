// src/services/pecPublicService.ts
import publicApi, { getPublicApiBaseUrl } from '../config/publicApi';

export type PecVerifyStatus = 'VALID' | 'EXPIRED' | 'INVALID' | 'NOT_FOUND' | 'ERROR';

export interface PecVerifyResponse {
  status: PecVerifyStatus;
  message: string;
  pecId?: number | null;
  numero?: string | null;
  nom?: string | null;
  postnom?: string | null;
  prenom?: string | null;
  genre?: string | null;
  age?: string | null;
  adresseMalade?: string | null;
  qualiteMalade?: string | null;
  etablissement?: string | null;
  motif?: string | null;
  dateEmission?: string | null;
  dateExpiration?: string | null;
  createdByFullname?: string | null;  // Médecin du Sénat / émis par
  photoUrl?: string | null;
  tokenExpiresAt?: string | null;     // expiration du lien → Actif / Expiré
  pdfUrl?: string | null;
}

export const pecPublicService = {
  async verify(token: string) {
    const res = await publicApi.get(`/api/pec/public/${encodeURIComponent(token)}/verify`);
    return res.data as PecVerifyResponse;
  },

  /** Construit l'URL du PDF (avec image de fond passée en param pour le rapport) */
  buildPdfUrl(token: string): string {
    const base = getPublicApiBaseUrl();
    const path = `/api/pec/public/${encodeURIComponent(token)}/pdf`;
    if (typeof window !== 'undefined' && window.location?.origin) {
      const bgUrl = `${window.location.origin}/assets/fond_portrait_a4.png`;
      return `${base}${path}?backgroundImageUrl=${encodeURIComponent(bgUrl)}`;
    }
    return `${base}${path}`;
  },
};
