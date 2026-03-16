import api from '../config/api';

// Backend: /api/2fa/*
// - POST /api/2fa/setup  -> { secret, otpauthUrl, qrCodeDataUrl }
// - POST /api/2fa/verify -> { code }
// - POST /api/2fa/disable -> { code }
// (Optionnel) GET /api/2fa/status -> { twoFactorEnabled, hasSecret }

export type TwoFaSetupResponse = {
  secret: string;
  otpauthUrl: string;
  qrCodeDataUrl: string; // ex: "data:image/png;base64,..."
};

export type TwoFaStatusResponse = {
  twoFactorEnabled: boolean;
  hasSecret: boolean;
};

export const twoFaService = {
  async status(): Promise<TwoFaStatusResponse | null> {
    try {
      const res = await api.get<TwoFaStatusResponse>('/api/2fa/status');
      return res.data;
    } catch {
      // endpoint optionnel selon backend
      return null;
    }
  },

  async setup(): Promise<TwoFaSetupResponse> {
    const res = await api.post<TwoFaSetupResponse>('/api/2fa/setup');
    return res.data;
  },

  async verify(code: string): Promise<void> {
    await api.post('/api/2fa/verify', { code });
  },

  async disable(code: string): Promise<void> {
    await api.post('/api/2fa/disable', { code });
  },
};
