import api from '../config/api';

/* ---------- Types ---------- */

export interface PecCreatePayload {
  hopitalId: number;
  etablissement?: string;
  motif?: string;
  actes?: string;
  remarque?: string;
  adresseMalade?: string;
  qualiteMalade?: string;
}

export interface PecCreated {
  id: number;
  numero: string;
}

export interface PecDetail {
  id: number;
  numero: string;
  // étends selon PriseEnChargeDTO.Detail si besoin
}

/* ---------- Listing & pagination ---------- */

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
}

export interface PecListRow {
  id: number;
  numero: string;
  dateEmission: string;
  dateExpiration?: string | null;
  statut?: string | null;
  hopitalId: number | null;
  hopitalNom: string | null;
  beneficiaireNom: string | null;
  beneficiaireQualite: string | null;
  montant: number | null;
}

/* ---------- Stats ---------- */

export interface LabelCount {
  label: string;
  value: number;
}

/* ---------- Helpers ---------- */

const buildCreateBody = (
  payload: PecCreatePayload,
  ids: Partial<{
    agentId: number;
    senateurId: number;
    conjointId: number;
    enfantId: number;
  }>
) => ({
  agentId: ids.agentId ?? null,
  senateurId: ids.senateurId ?? null,
  conjointId: ids.conjointId ?? null,
  enfantId: ids.enfantId ?? null,

  qualiteMalade: payload.qualiteMalade ?? '',
  adresseMalade: payload.adresseMalade ?? '',
  etablissement: payload.etablissement,
  motif: payload.motif,
  actes: payload.actes,
  remarque: payload.remarque,

  hopitalId: payload.hopitalId,
});

const currentYearMonth = () => {
  const d = new Date();
  const m = `${d.getMonth() + 1}`.padStart(2, '0');
  return `${d.getFullYear()}-${m}`;
};

const nextMonthStart = (ym: string) => {
  const [y, m] = ym.split('-').map(Number);
  const n = new Date(y, m, 1);
  const mm = String(n.getMonth() + 1).padStart(2, '0');
  return `${n.getFullYear()}-${mm}-01`;
};

const openPdfBlob = (data: BlobPart, filename: string) => {
  const blob = new Blob([data], { type: 'application/pdf' });
  const url = URL.createObjectURL(blob);

  const w = window.open(url, '_blank', 'noopener,noreferrer');
  if (!w) {
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
  }

  setTimeout(() => URL.revokeObjectURL(url), 60_000);
};

/** URL absolue de l'image de fond (frontend), passée au backend pour le rapport PDF */
const getBackgroundImageUrl = (path: string): string => {
  if (typeof window === 'undefined') return '';
  const base = window.location.origin;
  return path.startsWith('/') ? `${base}${path}` : `${base}/${path}`;
};

/* ---------- Service ---------- */

export const pecService = {
  async createForAgent(agentId: number, payload: PecCreatePayload) {
    const body = buildCreateBody(payload, { agentId });
    const res = await api.post('/api/pec', body);
    return res.data as PecCreated;
  },

  async createForSenateur(senateurId: number, payload: PecCreatePayload) {
    const body = buildCreateBody(payload, { senateurId });
    const res = await api.post('/api/pec', body);
    return res.data as PecCreated;
  },

  async createForConjoint(conjointId: number, payload: PecCreatePayload) {
    const body = buildCreateBody(payload, { conjointId });
    const res = await api.post('/api/pec', body);
    return res.data as PecCreated;
  },

  async createForEnfant(enfantId: number, payload: PecCreatePayload) {
    const body = buildCreateBody(payload, { enfantId });
    const res = await api.post('/api/pec', body);
    return res.data as PecCreated;
  },

  async getById(pecId: number) {
    const res = await api.get(`/api/pec/${pecId}`);
    return res.data as PecDetail;
  },

  async listByHopital(params: {
    hopitalId?: number | null;
    month?: string | null;
    page?: number;
    size?: number;
  }) {
    const { hopitalId, month, page = 0, size = 20 } = params;
    const qs = new URLSearchParams();

    if (hopitalId != null) {
      qs.set('hopitalId', String(hopitalId));
    }

    if (month) {
      qs.set('from', `${month}-01`);
      qs.set('to', nextMonthStart(month));
    }

    qs.set('page', String(page));
    qs.set('size', String(size));

    const res = await api.get(`/api/pec/listing/by-hopital?${qs.toString()}`);
    return res.data as PageResponse<PecListRow>;
  },

  async remove(pecId: number): Promise<void> {
    await api.delete(`/api/pec/${pecId}`);
  },

  /**
   * Impression PDF de la liste
   * Backend attendu:
   * GET /api/pec/print-listing?hopitalId=...&month=YYYY-MM&limit=...
   */
  async printListing(params: {
    hopitalId?: number | null;
    month?: string | null;
    limit?: number;
  }): Promise<void> {
    const { hopitalId, month, limit = 5000 } = params;
    const qs = new URLSearchParams();

    if (hopitalId != null) {
      qs.set('hopitalId', String(hopitalId));
    }

    if (month) {
      qs.set('month', month);
    }

    qs.set('limit', String(limit));

    const fondPaysageUrl = getBackgroundImageUrl('/img/fond_paysage_a4.png');
    if (fondPaysageUrl) qs.set('backgroundImageUrl', fondPaysageUrl);

    const res = await api.get(`/api/pec/print-listing?${qs.toString()}`, {
      responseType: 'blob',
    });

    const contentType = res.headers?.['content-type'] ?? '';
    if (contentType && !String(contentType).includes('application/pdf')) {
      throw new Error("Le serveur n'a pas renvoyé un PDF.");
    }

    const suffix = month ? `-${month}` : '';
    openPdfBlob(res.data, `PEC-listing${suffix}.pdf`);
  },

  /**
   * Impression d'une note unitaire (passe l'URL du fond portrait au backend pour le rapport)
   */
  async print(pecId: number): Promise<void> {
    const fondPortraitUrl = getBackgroundImageUrl('/img/fond_portrait_a4.png');
    const url = fondPortraitUrl
      ? `/api/pec/${pecId}/print?backgroundImageUrl=${encodeURIComponent(fondPortraitUrl)}`
      : `/api/pec/${pecId}/print`;
    const res = await api.get(url, {
      responseType: 'blob',
    });

    const contentType = res.headers?.['content-type'] ?? '';
    if (contentType && !String(contentType).includes('application/pdf')) {
      throw new Error("Le serveur n'a pas renvoyé un PDF.");
    }

    openPdfBlob(res.data, `PEC-${pecId}.pdf`);
  },

  async statsByHopital(month?: string) {
    const ym = month ?? currentYearMonth();
    const res = await api.get('/api/pec/stats/hopital', {
      params: { month: ym },
    });
    return res.data as LabelCount[];
  },
};

export default pecService;