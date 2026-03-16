// src/services/conjointService.ts
import api, { API_BASE_URL } from '../config/api';

/* ======================== Types ======================== */
export interface Conjoint {
  id?: number;
  nom: string;
  postnom: string;
  prenom?: string;
  genre: 'M' | 'F';
  datenaiss?: string | Date;   // "yyyy-MM-dd" | "dd/MM/yyyy" | Date
  profession?: string;
  telephone?: string;
  email?: string;
  photo?: string;
  // lnaiss?: string; // décommente si tu l’exposes côté front
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

function compact<T extends Record<string, any>>(obj: T): Partial<T> {
  const out: Record<string, any> = {};
  for (const [k, v] of Object.entries(obj)) {
    if (v === undefined || v === null) continue;
    if (typeof v === 'string' && v.trim() === '') continue;
    out[k] = v;
  }
  return out as Partial<T>;
}

function normalizeConjointForApi(c: Conjoint): Partial<Conjoint> {
  return compact({
    ...c,
    datenaiss: toISODate(c.datenaiss as any),
  });
}

/** Construit un FormData propre pour les endpoints /multipart */
function buildFormData(conjoint: Conjoint, photoFile?: File) {
  const fd = new FormData();
  const payload = normalizeConjointForApi(conjoint);
  fd.append('conjoint', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
  if (photoFile) fd.append('photoFile', photoFile);
  return fd;
}

/* ======================== Service ======================== */
export const conjointService = {
  /* ---------- Création imbriquée (compat historique) ---------- */
  async createForAgent(agentId: number, conjoint: Conjoint, photoFile?: File): Promise<Conjoint> {
    const res = await api.post<Conjoint>(
      `/api/agents/${agentId}/conjoint`,
      buildFormData(conjoint, photoFile)
    );
    return res.data;
  },

  async createForSenateur(senateurId: number, conjoint: Conjoint, photoFile?: File): Promise<Conjoint> {
    const res = await api.post<Conjoint>(
      `/api/senateurs/${senateurId}/conjoint`,
      buildFormData(conjoint, photoFile)
    );
    return res.data;
  },

  /* ---------- Nouvelles routes génériques /api/conjoints ---------- */
  /** Création JSON en passant agentId XOR senateurId (via reqPayload) */
  async createJson(reqPayload: {
    agentId?: number;
    senateurId?: number;
    conjoint: Conjoint;
    photoFile?: File; // si fourni, on bascule en multipart alias
  }): Promise<Conjoint> {
    const { agentId, senateurId, conjoint, photoFile } = reqPayload;

    // Alias multipart si un fichier est fourni
    if (photoFile) {
      const fd = buildFormData({ ...conjoint }, photoFile);
      // ConjointController accepte POST /api/conjoints (multipart alias)
      // avec le DTO CreateRequest contenant agentId/senateurId.
      // On injecte agentId/senateurId dans le JSON du 'conjoint' via payload.
      // Ici, plus simple: on ajoute dans la querystring ?agentId= / ?senateurId= si tu l'as prévu côté back.
      // Sinon, inclure ces ids dans l'objet 'conjoint' si ton DTO les porte.
      // -> Comme nos DTOs portent agentId/senateurId, on préfère un endpoint dédié JSON.
      // Pour rester safe, on tombe sur la version JSON si id parent présent :
    }

    // Version JSON (recommandée)
    const body = {
      nom: conjoint.nom,
      postnom: conjoint.postnom,
      prenom: conjoint.prenom,
      genre: conjoint.genre,
      datenaiss: toISODate(conjoint.datenaiss as any),
      profession: conjoint.profession,
      // lnaiss: conjoint.lnaiss,
      // photo gérée par upload dédié / multipart
      photo: conjoint.photo,
      agentId,
      senateurId,
    };
    const res = await api.post<Conjoint>('/api/conjoints', body);
    return res.data;
  },

  /** Mise à jour par id (JSON ou alias multipart si photo fournie) */
  async update(id: number, conjoint: Conjoint, photoFile?: File): Promise<Conjoint> {
    if (photoFile) {
      const res = await api.put<Conjoint>(`/api/conjoints/${id}`, buildFormData(conjoint, photoFile));
      return res.data;
    } else {
      const payload = normalizeConjointForApi(conjoint);
      const res = await api.put<Conjoint>(`/api/conjoints/${id}`, payload);
      return res.data;
    }
  },

  /** Upload photo dédié */
  async uploadPhoto(id: number, photoFile: File): Promise<Conjoint> {
    const fd = new FormData();
    fd.append('photoFile', photoFile);
    const res = await api.post<Conjoint>(`/api/conjoints/${id}/photo`, fd);
    return res.data;
  },

  /* ---------- Read / Delete ---------- */
  async getById(id: number): Promise<Conjoint> {
    const res = await api.get<Conjoint>(`/api/conjoints/${id}`);
    return res.data;
  },

  async delete(id: number): Promise<void> {
    await api.delete(`/api/conjoints/${id}`);
  },

  /** URL absolue de la photo du conjoint */
  photoUrl(filename?: string, bust?: boolean) {
    if (!filename) return undefined;
    const safe = encodeURIComponent(filename);
    const base = `${API_BASE_URL}/api/conjoints/photos/${safe}`;
    return bust ? `${base}?t=${Date.now()}` : base;
  },
};

export default conjointService;
