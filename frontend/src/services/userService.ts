import api from '../config/api';

export interface User {
  id?: number;
  username: string;
  email: string;
  password?: string;
  nom: string;
  prenom: string;
  fonction?: string;
  telephone?: string;
  /** ID du rôle (envoyé à l’API) */
  roleId?: number;
  /** Désignation du rôle pour l’affichage (ex: ADMIN, USER) */
  roleDesignation?: string;
  /** @deprecated Utiliser roleDesignation. Conservé pour compatibilité. */
  role?: string;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface UserStats {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
  adminUsers: number;
  regularUsers: number;
  managerUsers: number;
  usersCreatedToday: number;
}

export const userService = {
  async getAllUsers(params: {
    searchTerm?: string;
    roleId?: number;
    role?: string;
    active?: boolean;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Promise<PageResponse<User>> {
    const response = await api.get<PageResponse<User>>('/api/users', { params });
    return response.data;
  },

  async getUserById(id: number): Promise<User> {
    const response = await api.get<User>(`/api/users/${id}`);
    return response.data;
  },

  async createUser(user: User): Promise<User> {
    const { role, ...rest } = user;
    const response = await api.post<User>('/api/users', { ...rest, roleId: user.roleId } as any);
    return response.data;
  },

  async updateUser(id: number, user: User): Promise<User> {
    const { role, ...rest } = user;
    const response = await api.put<User>(`/api/users/${id}`, { ...rest, roleId: user.roleId } as any);
    return response.data;
  },

  async deleteUser(id: number): Promise<void> {
    await api.delete(`/api/users/${id}`);
  },

  async toggleUserStatus(id: number): Promise<void> {
    await api.put(`/api/users/${id}/toggle-status`);
  },

  async getStats(): Promise<UserStats> {
    const response = await api.get<UserStats>('/api/users/stats');
    return response.data;
  },

  async checkUsername(username: string): Promise<boolean> {
    const response = await api.get<{ exists: boolean }>(`/api/users/check-username/${username}`);
    return response.data.exists;
  },

  async checkEmail(email: string): Promise<boolean> {
    const response = await api.get<{ exists: boolean }>(`/api/users/check-email/${email}`);
    return response.data.exists;
  },
};
