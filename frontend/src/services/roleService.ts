import api from '../config/api';

export interface Role {
  id: number;
  designation: string;
  permissions?: PermissionRef[];
}

export interface PermissionRef {
  id: number;
  designation: string;
  coderbac: string;
}

export interface RoleCreateRequest {
  designation: string;
}

export interface RoleRightsUpdateRequest {
  permissionIds: number[];
}

export const roleService = {
  async findAll(): Promise<Role[]> {
    const { data } = await api.get<Role[]>('/api/roles');
    return data;
  },

  async findById(id: number): Promise<Role> {
    const { data } = await api.get<Role>(`/api/roles/${id}`);
    return data;
  },

  async create(request: RoleCreateRequest): Promise<Role> {
    const { data } = await api.post<Role>('/api/roles', request);
    return data;
  },

  async update(id: number, designation: string): Promise<Role> {
    const { data } = await api.put<Role>(`/api/roles/${id}`, { designation });
    return data;
  },

  async delete(id: number): Promise<void> {
    await api.delete(`/api/roles/${id}`);
  },

  async updateRights(roleId: number, permissionIds: number[]): Promise<Role> {
    const { data } = await api.put<Role>(`/api/roles/${roleId}/permissions`, { permissionIds });
    return data;
  },
};
