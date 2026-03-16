// src/services/enfantService.ts
import api, { API_BASE_URL } from '../config/api';

/* ======================== Types ======================== */
export interface Enfant {
  id?: number;
  nomEnfant: string;
  postnomEnfant: string;
  prenomEnfant?: string;
  genre: 'M' | 'F';
  datenaiss: string | Date;             // "yyyy-MM-dd" | "dd/MM/yyyy" | Date
  categorie: 'LEGITIME' | 'ADOPTIF';
  reference?: string;
  photo?: string;
  stat?: string;
}

/* ======================== Utils ======================== */
function toISODate(input?: string | Date | null): string | undefined {
  if (!input) return undefined;

  if (input instanceof Date) {
    const yyyy = input.getFullYear();
    const mm = String(input.getMonth() + 1).padStart(2, '0');
    const dd = String(input.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  const s = String(input).trim();
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(s); // dd/MM/yyyy
  if (m) return `${m[3]}-${m[2]}-${m[1]}`;
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;     // yyyy-MM-dd

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

function normalizeEnfantForApi(e: Enfant): Partial<Enfant> {
  return compact({
    ...e,
    datenaiss: toISODate(e.datenaiss as any),
  });
}

function buildFormData(enfant: Enfant, photoFile?: File): FormData {
  const fd = new FormData();
  const payload = normalizeEnfantForApi(enfant);
  fd.append('enfant', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
  if (photoFile) fd.append('photoFile', photoFile);
  return fd;
}

/* ======================== Service ======================== */
export const enfantService = {
  /* ---------- CREATE pour AGENT ---------- */
  async createForAgent(agentId: number, enfant: Enfant, photoFile?: File): Promise<Enfant> {
    if (photoFile) {
      const res = await api.post<Enfant>(`/api/agents/${agentId}/enfants`, buildFormData(enfant, photoFile));
      return res.data;
    } else {
      const payload = normalizeEnfantForApi(enfant);
      const res = await api.post<Enfant>(`/api/agents/${agentId}/enfants`, payload);
      return res.data;
    }
  },

  /* ---------- CREATE pour SENATEUR ---------- */
  async createForSenateur(senateurId: number, enfant: Enfant, photoFile?: File): Promise<Enfant> {
    if (photoFile) {
      const res = await api.post<Enfant>(`/api/senateurs/${senateurId}/enfants`, buildFormData(enfant, photoFile));
      return res.data;
    } else {
      const payload = normalizeEnfantForApi(enfant);
      const res = await api.post<Enfant>(`/api/senateurs/${senateurId}/enfants`, payload);
      return res.data;
    }
  },

  /* ---------- UPDATE ---------- */
  async update(id: number, enfant: Enfant, photoFile?: File): Promise<Enfant> {
    if (photoFile) {
      const res = await api.put<Enfant>(`/api/enfants/${id}`, buildFormData(enfant, photoFile));
      return res.data;
    } else {
      const payload = normalizeEnfantForApi(enfant);
      const res = await api.put<Enfant>(`/api/enfants/${id}`, payload);
      return res.data;
    }
  },

  /* ---------- Upload photo dédié ---------- */
  async uploadPhoto(id: number, photoFile: File): Promise<Enfant> {
    const fd = new FormData();
    fd.append('photoFile', photoFile);
    const res = await api.post<Enfant>(`/api/enfants/${id}/photo`, fd);
    return res.data;
  },

  /* ---------- READ ---------- */
  async getById(id: number): Promise<Enfant> {
    const response = await api.get<Enfant>(`/api/enfants/${id}`);
    return response.data;
  },

  async listByAgent(agentId: number): Promise<Enfant[]> {
    const response = await api.get<Enfant[]>(`/api/agents/${agentId}/enfants`);
    return response.data;
  },

  async listBySenateur(senateurId: number): Promise<Enfant[]> {
    const response = await api.get<Enfant[]>(`/api/senateurs/${senateurId}/enfants`);
    return response.data;
  },

  /* ---------- DELETE ---------- */
  async delete(id: number): Promise<void> {
    await api.delete(`/api/enfants/${id}`);
  },

  /* ---------- URL Photo ---------- */
  photoUrl(filename?: string, bust?: boolean) {
    if (!filename) return undefined;
    const safe = encodeURIComponent(filename);
    const base = `${API_BASE_URL}/api/enfants/photos/${safe}`;
    return bust ? `${base}?t=${Date.now()}` : base;
  },
};

export default enfantService;
