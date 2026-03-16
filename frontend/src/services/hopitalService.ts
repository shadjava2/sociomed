// src/services/hopitalService.ts
import api from '../config/api';
import type { PageResponse } from './agentService';

/* ================= Types alignés sur HopitalDTO ================= */

export type CategorieHopital = 'PUBLIC' | 'PRIVE' | 'CONFESSIONNEL' | 'AUTRE';

export interface HopitalSummary {
  id: number;
  code: string;
  nom: string;
  categorie: CategorieHopital;
  ville?: string | null;
  actif: boolean;
}

export interface HopitalDetail {
  id: number;
  code: string;
  nom: string;
  categorie: CategorieHopital;

  adresse?: string | null;
  commune?: string | null;
  ville?: string | null;

  contactNom?: string | null;
  contactTelephone?: string | null;
  email?: string | null;
  siteWeb?: string | null;

  actif: boolean;

  numeroConvention?: string | null;
  conventionDebut?: string | null; // LocalDateTime ISO string
  conventionFin?: string | null;   // LocalDateTime ISO string

  createdAt?: string | null;       // LocalDateTime ISO string
  updatedAt?: string | null;       // LocalDateTime ISO string
}

/** Alias pratique si tu veux importer `type Hopital` côté UI */
export type Hopital = HopitalDetail;

/* ================= DTO Write (Create/Update) ================= */

export interface HopitalCreateRequest {
  code: string;
  nom: string;
  categorie: CategorieHopital;

  adresse?: string;
  commune?: string;
  ville?: string;

  contactNom?: string;
  contactTelephone?: string;
  email?: string;
  siteWeb?: string;

  actif?: boolean;

  numeroConvention?: string;
  conventionDebut?: string; // 'yyyy-MM-ddTHH:mm' d'un <input type="datetime-local">
  conventionFin?: string;   // idem
}

export type HopitalUpdateRequest = Partial<HopitalCreateRequest>;

/* ================= Helpers ================= */

function compact<T extends Record<string, any>>(obj: T): Partial<T> {
  const out: Record<string, any> = {};
  for (const [k, v] of Object.entries(obj || {})) {
    if (v === undefined || v === null) continue;
    if (typeof v === 'string' && v.trim() === '') continue;
    out[k] = v;
  }
  return out;
}

type ListParams = {
  q?: string;
  actif?: boolean;
  categorie?: CategorieHopital;
  page?: number;
  size?: number;
  sortBy?: string;           // par défaut "nom"
  sortDir?: 'asc' | 'desc';  // par défaut "asc"
};

/* ================= Service ================= */

export const hopitalService = {
  async listPaged(params: ListParams = {}): Promise<PageResponse<HopitalSummary>> {
    const res = await api.get<PageResponse<HopitalSummary>>('/api/hopitaux', {
      params: compact(params),
    });
    return res.data;
  },

  async listActifs(params: Omit<ListParams, 'actif'> = {}): Promise<PageResponse<HopitalSummary>> {
    const res = await api.get<PageResponse<HopitalSummary>>('/api/hopitaux/actifs', {
      params: compact(params),
    });
    return res.data;
  },

  async getById(id: number): Promise<HopitalDetail> {
    const res = await api.get<HopitalDetail>(`/api/hopitaux/${id}`);
    return res.data;
  },

  async getByCode(code: string): Promise<HopitalDetail> {
    const res = await api.get<HopitalDetail>(`/api/hopitaux/code/${encodeURIComponent(code)}`);
    return res.data;
  },

  async create(payload: HopitalCreateRequest): Promise<HopitalDetail> {
    const res = await api.post<HopitalDetail>('/api/hopitaux', compact(payload));
    return res.data;
  },

  async update(id: number, payload: HopitalUpdateRequest): Promise<HopitalDetail> {
    const res = await api.put<HopitalDetail>(`/api/hopitaux/${id}`, compact(payload));
    return res.data;
  },

  async delete(id: number): Promise<void> {
    await api.delete(`/api/hopitaux/${id}`);
  },

  async setActif(id: number, value: boolean): Promise<HopitalDetail> {
    const res = await api.patch<HopitalDetail>(`/api/hopitaux/${id}/actif`, null, {
      params: { value },
    });
    return res.data;
  },
};

export default hopitalService;
