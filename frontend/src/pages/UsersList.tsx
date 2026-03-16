import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { userService, User, PageResponse } from '../services/userService';
import { UserForm } from '../components/UserForm';
import { Plus, CreditCard as Edit, Trash2, ToggleLeft, ToggleRight, Search, RefreshCw, Loader2 } from 'lucide-react';
import { Skeleton, SkeletonTableRow } from '../components/ui/Skeleton';
import { QuestionDialog } from '../components/QuestionDialog';

/** Skeleton de la page Gestion des Utilisateurs (rendu progressif) */
const UsersListSkeleton: React.FC = () => (
  <div className="space-y-6" aria-hidden>
    <div className="flex items-center justify-between">
      <Skeleton className="h-9 w-72" />
      <Skeleton className="h-10 w-40 rounded-lg" />
    </div>
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
      <div className="mb-6">
        <Skeleton className="h-12 w-full rounded-lg" />
      </div>
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead className="bg-slate-50 border-b border-slate-200">
            <tr>
              {['Nom complet', 'Username', 'Email', 'Rôle', 'Statut', 'Actions'].map((label) => (
                <th key={label} className="px-4 py-3 text-left text-sm font-medium text-slate-500">
                  {label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200">
            {Array.from({ length: 8 }).map((_, i) => (
              <SkeletonTableRow key={i} cols={6} />
            ))}
          </tbody>
        </table>
      </div>
      <div className="flex items-center justify-between mt-6 pt-6 border-t border-slate-200">
        <Skeleton className="h-4 w-52" />
        <div className="flex gap-2">
          <Skeleton className="h-10 w-24 rounded-lg" />
          <Skeleton className="h-10 w-24 rounded-lg" />
        </div>
      </div>
    </div>
  </div>
);

export const UsersList: React.FC = () => {
  const { hasPermission } = useAuth();
  const [users, setUsers] = useState<PageResponse<User>>({
    content: [],
    totalElements: 0,
    totalPages: 0,
    size: 10,
    number: 0,
  });
  const [showForm, setShowForm] = useState(false);
  const [editingUser, setEditingUser] = useState<User | undefined>();
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  const loadUsers = async () => {
    setIsLoading(true);
    try {
      const data = await userService.getAllUsers({
        searchTerm: searchTerm || undefined,
        page,
        size: 10,
        sortBy: 'createdAt',
        sortDir: 'desc',
      });
      setUsers(data);
    } catch (error) {
      console.error('Erreur chargement utilisateurs:', error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, [page, searchTerm]);

  const handleSubmit = async (user: User) => {
    if (editingUser) {
      await userService.updateUser(editingUser.id!, user);
    } else {
      await userService.createUser(user);
    }
    setShowForm(false);
    setEditingUser(undefined);
    loadUsers();
  };

  const performDeleteUser = async (id: number) => {
    await userService.deleteUser(id);
    loadUsers();
  };

  const handleToggleStatus = async (id: number) => {
    await userService.toggleUserStatus(id);
    loadUsers();
  };

  if (isLoading && users.content.length === 0) {
    return <UsersListSkeleton />;
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h1 className="text-2xl sm:text-3xl font-bold text-slate-800">Gestion des Utilisateurs</h1>
        {hasPermission('USERS_CREATE') && (
          <button
            onClick={() => setShowForm(true)}
            className="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors min-h-[44px] sm:min-h-0"
          >
            <Plus className="w-5 h-5" />
            <span>Nouvel utilisateur</span>
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-4 sm:p-6">
        <div className="mb-4 sm:mb-6 flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-slate-400 w-5 h-5" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setPage(0);
              }}
              placeholder="Rechercher par nom, prénom, email..."
              className="w-full pl-10 pr-4 py-3 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent min-h-[44px]"
            />
          </div>
          <button
            type="button"
            onClick={() => loadUsers()}
            disabled={isLoading}
            className="inline-flex items-center justify-center gap-2 px-4 py-3 bg-slate-100 text-slate-700 rounded-lg hover:bg-slate-200 border border-slate-300 min-h-[44px] disabled:opacity-70 disabled:cursor-not-allowed"
            title="Actualiser la liste"
          >
            {isLoading ? <Loader2 className="w-4 h-4 animate-spin" aria-hidden /> : <RefreshCw className="w-4 h-4" />}
            Actualiser
          </button>
        </div>

        {isLoading ? (
          <div className="text-center py-8 text-slate-500">Chargement...</div>
        ) : users.content.length === 0 ? (
          <div className="text-center py-8 text-slate-500">Aucun utilisateur trouvé</div>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="min-w-full w-max">
                <thead className="bg-slate-50 border-b border-slate-200">
                  <tr>
                    <th className="px-4 py-3 text-left text-sm font-medium text-slate-700 whitespace-nowrap">Nom complet</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-slate-700 whitespace-nowrap">Username</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-slate-700 whitespace-nowrap">Email</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-slate-700 whitespace-nowrap">Rôle</th>
                    <th className="px-4 py-3 text-left text-sm font-medium text-slate-700 whitespace-nowrap">Statut</th>
                    <th className="px-4 py-3 text-right text-sm font-medium text-slate-700 whitespace-nowrap sticky right-0 bg-slate-50 shadow-[-4px_0_6px_rgba(0,0,0,0.04)] z-10">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {users.content.map((user) => (
                    <tr key={user.id} className="hover:bg-slate-50 bg-white">
                      <td className="px-4 py-3 text-sm text-slate-800 whitespace-nowrap">{user.nom} {user.prenom}</td>
                      <td className="px-4 py-3 text-sm text-slate-600 whitespace-nowrap">{user.username}</td>
                      <td className="px-4 py-3 text-sm text-slate-600 whitespace-nowrap">{user.email}</td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        <span
                          className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
                            (user.roleDesignation || user.role) === 'ADMIN'
                              ? 'bg-red-100 text-red-700'
                              : (user.roleDesignation || user.role) === 'MANAGER'
                              ? 'bg-yellow-100 text-yellow-700'
                              : 'bg-blue-100 text-blue-700'
                          }`}
                        >
                          {user.roleDesignation ?? user.role}
                        </span>
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap">
                        <span
                          className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
                            user.active
                              ? 'bg-green-100 text-green-700'
                              : 'bg-slate-100 text-slate-700'
                          }`}
                        >
                          {user.active ? 'Actif' : 'Inactif'}
                        </span>
                      </td>
                      <td className="px-4 py-3 whitespace-nowrap sticky right-0 bg-white shadow-[-4px_0_6px_rgba(0,0,0,0.04)] z-10">
                        <div className="flex justify-end gap-1 sm:gap-2 flex-shrink-0">
                          {hasPermission('USERS_EDIT') && (
                            <button
                              onClick={() => handleToggleStatus(user.id!)}
                              className="p-2 text-slate-600 hover:bg-slate-100 rounded-lg"
                              title={user.active ? 'Désactiver' : 'Activer'}
                            >
                              {user.active ? (
                                <ToggleRight className="w-4 h-4" />
                              ) : (
                                <ToggleLeft className="w-4 h-4" />
                              )}
                            </button>
                          )}
                          {hasPermission('USERS_EDIT') && (
                            <button
                              onClick={() => {
                                setEditingUser(user);
                                setShowForm(true);
                              }}
                              className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg"
                            >
                              <Edit className="w-4 h-4" />
                            </button>
                          )}
                          {hasPermission('USERS_DELETE') && (
                            <button
                              onClick={() => setDeleteConfirmId(user.id!)}
                              className="p-2 text-red-600 hover:bg-red-50 rounded-lg min-w-[44px] min-h-[44px] sm:min-w-0 sm:min-h-0 flex items-center justify-center"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {users.totalPages > 1 && (
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mt-6 pt-6 border-t border-slate-200">
                <div className="text-sm text-slate-600">
                  Page {users.number + 1} sur {users.totalPages} ({users.totalElements} total)
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={users.number === 0}
                    className="px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Précédent
                  </button>
                  <button
                    onClick={() => setPage((p) => Math.min(users.totalPages - 1, p + 1))}
                    disabled={users.number >= users.totalPages - 1}
                    className="px-4 py-2 border border-slate-300 rounded-lg hover:bg-slate-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Suivant
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {showForm && (
        <UserForm
          user={editingUser}
          onSubmit={handleSubmit}
          onCancel={() => {
            setShowForm(false);
            setEditingUser(undefined);
          }}
        />
      )}

      {deleteConfirmId !== null && (
        <QuestionDialog
          open
          title="Confirmer la suppression"
          message="Voulez-vous vraiment supprimer cet utilisateur ? Cette action est irréversible."
          variant="danger"
          confirmLabel="Supprimer"
          onConfirm={async () => {
            await performDeleteUser(deleteConfirmId);
            setDeleteConfirmId(null);
          }}
          onCancel={() => setDeleteConfirmId(null)}
        />
      )}
    </div>
  );
};
