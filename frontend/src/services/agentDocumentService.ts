// src/services/agentDocumentService.ts
import api from '../config/api';

export type AgentDocumentType =
  | 'CNI'
  | 'PASSEPORT'
  | 'CARTE_SERVICE'
  | 'ARRETE'
  | 'MATRICULE'
  | 'DIPLOME'
  | 'ATTESTATION'
  | 'CONTRAT'
  | 'CERTIFICAT_MEDICAL'
  | 'AUTRE';

export interface AgentDocument {
  id: number;
  agentId: number;
  type: AgentDocumentType;
  title?: string;
  originalName: string;
  contentType?: string;
  size?: number;
  uploadedAt?: string;
}

function extractFileNameFromContentDisposition(cd?: string | null): string | null {
  if (!cd) return null;
  // Content-Disposition: inline; filename="xxx.pdf"
  const m = /filename\*?=(?:UTF-8''|")?([^";]+)"?/i.exec(cd);
  if (!m) return null;
  try {
    return decodeURIComponent(m[1]);
  } catch {
    return m[1];
  }
}

export const agentDocumentService = {
  async list(agentId: number): Promise<AgentDocument[]> {
    const res = await api.get(`/api/agents/${agentId}/documents`);
    return res.data;
  },

  async upload(agentId: number, formData: FormData): Promise<AgentDocument> {
    const res = await api.post(`/api/agents/${agentId}/documents`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data;
  },

  async remove(docId: number): Promise<void> {
    await api.delete(`/api/agents/documents/${docId}`);
  },

  async fetchViewBlob(docId: number): Promise<{ blob: Blob; contentType?: string; fileName?: string }> {
    const res = await api.get(`/api/agents/documents/${docId}/view`, {
      responseType: 'blob',
    });
    const contentType = res.headers?.['content-type'];
    const fileName = extractFileNameFromContentDisposition(res.headers?.['content-disposition']);
    return { blob: res.data, contentType, fileName: fileName || undefined };
  },

  async fetchDownloadBlob(docId: number): Promise<{ blob: Blob; contentType?: string; fileName?: string }> {
    const res = await api.get(`/api/agents/documents/${docId}/download`, {
      responseType: 'blob',
    });
    const contentType = res.headers?.['content-type'];
    const fileName = extractFileNameFromContentDisposition(res.headers?.['content-disposition']);
    return { blob: res.data, contentType, fileName: fileName || undefined };
  },
};
