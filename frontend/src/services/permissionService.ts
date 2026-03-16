import api from '../config/api';

export interface Permission {
  id: number;
  designation: string;
  coderbac: string;
}

export const permissionService = {
  async findAll(): Promise<Permission[]> {
    const { data } = await api.get<Permission[]>('/api/permissions');
    return data;
  },
};
