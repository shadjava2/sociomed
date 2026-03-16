// src/services/senateurService.ts
import api, { API_BASE_URL } from '../config/api';
import { Conjoint } from './conjointService'; // ✅ ajout
/* ================== Types ================== */
export type Senateur = {
  id?: number;
  nom?: string;
  postnom?: string;
  prenom?: string;
  genre?: 'M' | 'F';
  datenaiss?: string | Date;          // "yyyy-MM-dd" | "dd/MM/yyyy" | Date
  statut?: 'EN_ACTIVITE' | 'HONORAIRE';
  telephone?: string | null;
  legislature?: string | null;
  email?: string | null;
  adresse?: string | null;
  photo?: string | null;
  
  // ✅ ajout pour corriger les accès senateur.conjoint.* dans l’UI
  conjoint?: Conjoint | null;
};

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  first: boolean;
  last: boolean;
  empty: boolean;
};

/* ================== Utils ================== */
function toISODate(input?: string | Date | null): string | undefined {
  if (!input) return undefined;

  if (input instanceof Date) {
    const yyyy = input.getFullYear();
    const mm = String(input.getMonth() + 1).padStart(2, '0');
    const dd = String(input.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
  const s = String(input).trim();
  const ddmmyyyy = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(s);
  if (ddmmyyyy) return `${ddmmyyyy[3]}-${ddmmyyyy[2]}-${ddmmyyyy[1]}`;
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;

  const d = new Date(s);
  if (!isNaN(d.getTime())) {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
  return undefined;
}

function compact<T extends Record<string, any>>(obj: T): Partial<T> {
  const out: Record<string, any> = {};
  for (const [k, v] of Object.entries(obj)) {
    if (v === undefined || v === null) continue;
    if (typeof v === 'string' && v.trim() === '') continue;
    out[k] = v;
  }
  return out as Partial<T>;
}

function normalizeForApi(s: Senateur): Partial<Senateur> {
  return compact({
    ...s,
    datenaiss: toISODate(s.datenaiss as any),
  });
}

function toJsonBlob(obj: any) {
  return new Blob([JSON.stringify(obj)], { type: 'application/json' });
}

function buildFormData(s: Senateur, photoFile?: File) {
  const fd = new FormData();
  fd.append('senateur', toJsonBlob(normalizeForApi(s)));
  if (photoFile) fd.append('photoFile', photoFile);
  return fd;
}

/* ================ Service ================ */
export const senateurService = {
  listPaged: async (params: {
    q?: string;
    statut?: 'EN_ACTIVITE' | 'HONORAIRE';
    genre?: 'M' | 'F';
    legislature?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: 'asc' | 'desc';
  }): Promise<PageResponse<Senateur>> => {
    const clean = compact(params);
    const { data } = await api.get('/api/senateurs', { params: clean });
    return data;
  },

  getById: async (id: number): Promise<Senateur> => {
    const { data } = await api.get(`/api/senateurs/${id}`);
    return data;
  },

  create: async (senateur: Senateur, photoFile?: File) => {
    if (photoFile) {
      const fd = buildFormData(senateur, photoFile);
      return api.post('/api/senateurs', fd); // multipart alias
    }
    return api.post('/api/senateurs', normalizeForApi(senateur)); // JSON pur
  },

  update: async (id: number, senateur: Senateur, photoFile?: File) => {
    if (photoFile) {
      const fd = buildFormData(senateur, photoFile);
      return api.put(`/api/senateurs/${id}`, fd); // multipart alias
    }
    return api.put(`/api/senateurs/${id}`, normalizeForApi(senateur)); // JSON
  },

  delete: async (id: number) => api.delete(`/api/senateurs/${id}`),

  uploadPhotoOnly: async (id: number, file: File) => {
    const fd = new FormData();
    fd.append('photoFile', file);
    return api.post(`/api/senateurs/${id}/photo`, fd);
  },

  photoUrl: (filename?: string | null, bust?: boolean) => {
    if (!filename) return '';
    const safe = encodeURIComponent(filename);
    const base = `${API_BASE_URL}/api/senateurs/photos/${safe}`;
    return bust ? `${base}?t=${Date.now()}` : base;
  },
};
