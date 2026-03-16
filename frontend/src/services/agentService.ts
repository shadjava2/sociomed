// src/services/agentService.ts
import api, { API_BASE_URL } from '../config/api';
import type { Conjoint } from './conjointService';

/* ======================== Types ======================== */
export interface Enfant {
  id?: number;
  nomEnfant: string;
  postnomEnfant: string;
  prenomEnfant?: string;
  genre: 'M' | 'F';
  datenaiss: string | Date;         // "yyyy-MM-dd" | Date
  categorie: 'LEGITIME' | 'ADOPTIF';
  stat?: string;
  reference?: string;
  photo?: string;
}

export interface Agent {
  id?: number;
  nom: string;
  postnom: string;
  prenom: string;
  genre?: 'M' | 'F';
  datenaiss?: string | Date; // accepte "dd/MM/yyyy" | "yyyy-MM-dd" | Date
  lnaiss?: string;
  etatc?: string;
  village?: string;
  groupement?: string;
  secteur?: string;
  territoire?: string;
  district?: string;
  province?: string;
  nationalite?: string;
  telephone?: string;
  email?: string;
  adresse?: string;
  direction?: string;
  etat?: string;
  stat?: string;
  photo?: string;
  /** Catégorie : Personnel d'appoint, Agent Administratif, Cadre Administratif */
  categorie?: string;

  // Relations — présentes quand on appelle /api/agents/{id}/details
  conjoint?: Conjoint | null;
  enfants?: Enfant[];               // ⬅️ NOUVEAU
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

type ListParams = {
  q?: string;
  genre?: 'M' | 'F';
  direction?: string;
  etat?: string;
  categorie?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
};

/* ======================== Helpers ======================== */
function toISODate(input?: string | Date | null): string | undefined {
  if (!input) return undefined;

  if (input instanceof Date) {
    const yyyy = input.getFullYear();
    const mm = String(input.getMonth() + 1).padStart(2, '0');
    const dd = String(input.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  const s = String(input).trim();

  // dd/MM/yyyy -> yyyy-MM-dd
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(s);
  if (m) return `${m[3]}-${m[2]}-${m[1]}`;

  // yyyy-MM-dd déjà OK
  if (/^\d{4}-\d{2}-\d{2}$/.test(s)) return s;

  // fallback parseable
  const d = new Date(s);
  if (!isNaN(d.getTime())) {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }
  return undefined;
}

/** Supprime undefined/null/"" (shallow) */
function compact<T extends Record<string, any>>(obj: T): Partial<T> {
  const out: Record<string, any> = {};
  for (const [k, v] of Object.entries(obj)) {
    if (v === undefined || v === null) continue;
    if (typeof v === 'string' && v.trim() === '') continue;
    out[k] = v;
  }
  return out as Partial<T>;
}

/** Normalise un Agent avant envoi (dates ISO + champs vides supprimés) */
function normalizeAgentForApi(agent: Agent): Partial<Agent> {
  const normalized: Partial<Agent> = {
    ...agent,
    datenaiss: toISODate(agent.datenaiss as any),
  };
  // 🔒 On ne transmet pas les relations via ce service :
  //   - 'conjoint' géré par conjointService
  //   - 'enfants'  gérés par enfantService
  delete (normalized as any).conjoint;
  delete (normalized as any).enfants;
  return compact(normalized);
}

/** Construit un FormData propre pour les endpoints /multipart */
function buildMultipart(agent: Agent, photoFile?: File): FormData {
  const fd = new FormData();
  const payload = normalizeAgentForApi(agent);
  fd.append('agent', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
  if (photoFile) fd.append('photoFile', photoFile);
  return fd;
}

/* ======================== Service ======================== */
export const agentService = {
  /* -------- LISTE / RECHERCHE paginée -------- */
  async listPaged(params: ListParams): Promise<PageResponse<Agent>> {
    const clean = compact(params);
    const res = await api.get<PageResponse<Agent>>('/api/agents', { params: clean });
    return res.data;
  },

  /* -------- CREATE --------
     - JSON si pas de fichier
     - multipart sinon (alias supporté par ton back) */
  async create(agent: Agent, photoFile?: File): Promise<Agent> {
    if (photoFile) {
      const fd = buildMultipart(agent, photoFile);
      // Ne PAS fixer 'Content-Type' : Axios gère la boundary
      const res = await api.post<Agent>('/api/agents', fd);
      return res.data;
    } else {
      const payload = normalizeAgentForApi(agent);
      const res = await api.post<Agent>('/api/agents', payload);
      return res.data;
    }
  },

  /* -------- UPDATE --------
     - JSON si pas de fichier
     - multipart sinon (alias supporté par ton back) */
  async update(id: number, agent: Agent, photoFile?: File): Promise<Agent> {
    if (photoFile) {
      const fd = buildMultipart(agent, photoFile);
      const res = await api.put<Agent>(`/api/agents/${id}`, fd);
      return res.data;
    } else {
      const payload = normalizeAgentForApi(agent);
      const res = await api.put<Agent>(`/api/agents/${id}`, payload);
      return res.data;
    }
  },

  /* -------- Upload photo seul -------- */
  async uploadPhoto(id: number, photoFile: File): Promise<Agent> {
    const fd = new FormData();
    fd.append('photoFile', photoFile);
    const res = await api.post<Agent>(`/api/agents/${id}/photo`, fd);
    return res.data;
  },

  /* -------- READ -------- */
  /** Fiche simple (sans relations — selon ton backend). */
  async getById(id: number): Promise<Agent> {
    const res = await api.get<Agent>(`/api/agents/${id}`);
    return res.data;
  },

  /** Détail complet (inclut `conjoint` **et** `enfants`). */
  async getDetails(id: number): Promise<Agent> {
    const res = await api.get<Agent>(`/api/agents/${id}/details`);
    return res.data;
  },

  /* -------- DELETE -------- */
  async delete(id: number): Promise<void> {
    await api.delete(`/api/agents/${id}`);
  },

  /* -------- URL photo agent -------- */
  /** URL absolue de la photo à utiliser dans <img src=...> */
  photoUrl(filename?: string, bust?: boolean) {
    if (!filename) return undefined;
    const safe = encodeURIComponent(filename);
    const base = `${API_BASE_URL}/api/agents/photos/${safe}`;
    return bust ? `${base}?t=${Date.now()}` : base;
  },
};
