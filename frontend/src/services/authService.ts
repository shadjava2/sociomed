import api from '../config/api';

export interface LoginRequest {
  username: string;
  password: string;
  /** Code TOTP (Google Authenticator) si 2FA activé */
  otp?: string;
}

export interface JwtResponse {
  token?: string;                 // ← tolère l’absence (fallback accessToken)
  accessToken?: string;           // ← fallback courant
  id: number;
  username: string;
  email: string;
  nom: string;
  prenom: string;
  role: string;
  permissions?: string[];
}

function normalizeToken(raw?: string): string | null {
  if (!raw) return null;
  return raw.toLowerCase().startsWith('bearer ') ? raw.slice(7) : raw;
}

export const authService = {
  async login(credentials: LoginRequest): Promise<JwtResponse> {
    const response = await api.post<JwtResponse>('/api/auth/signin', credentials);

    // Récupère token sous token OU accessToken, et nettoie le préfixe "Bearer "
    const token = normalizeToken(response.data.token) || normalizeToken(response.data.accessToken);
    if (!token) {
      throw new Error("JWT manquant dans la réponse de /api/auth/signin");
    }

    localStorage.setItem('jwt_token', token);
    localStorage.setItem('user', JSON.stringify(response.data));
    return response.data;
  },

  async logout(): Promise<void> {
    try {
      await api.post('/api/auth/signout');
    } catch {
      // pas bloquant si le serveur ne répond pas
    }
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('user');
  },

  getCurrentUser(): JwtResponse | null {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) as JwtResponse : null;
  },

  getToken(): string | null {
    return localStorage.getItem('jwt_token');
  },

  isAuthenticated(): boolean {
    return !!localStorage.getItem('jwt_token');
  },
};
