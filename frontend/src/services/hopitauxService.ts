// src/services/hopitauxService.ts
import api from '../config/api';

export interface HopitalLite {
  id: number;
  nom: string;
}

export const hopitauxService = {
  /**
   * Récupère une liste simple d'hôpitaux (id, nom).
   * Adapte l’URL à ton backend si besoin.
   */
  async listLite(): Promise<HopitalLite[]> {
    // ➜ OPTION A : si ton back renvoie une page { content: [...] }
    try {
      const res = await api.get('/api/hopitaux?page=0&size=1000');
      const items = Array.isArray(res.data?.content)
        ? res.data.content
        : Array.isArray(res.data)
        ? res.data
        : [];
      return items.map((h: any) => ({ id: h.id, nom: h.nom })) as HopitalLite[];
    } catch (e) {
      console.error('Erreur hopitauxService.listLite', e);
      return [];
    }

    // ➜ OPTION B (si tu as un endpoint dédié, décommente et utilise-le) :
    // const res = await api.get('/api/hopitaux/lite');
    // return res.data as HopitalLite[];
  },
};
