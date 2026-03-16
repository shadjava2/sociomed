import React, { useEffect, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useAppDialog } from '../contexts/AppDialogContext';
import { roleService, Role } from '../services/roleService';
import { permissionService, Permission } from '../services/permissionService';
import { Plus, Edit, Trash2, Shield, Loader2, RefreshCw } from 'lucide-react';
import { QuestionDialog } from '../components/QuestionDialog';

export const RolesList: React.FC = () => {
  const { hasPermission } = useAuth();
  const { showError } = useAppDialog();
  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showRoleForm, setShowRoleForm] = useState(false);
  const [editingRole, setEditingRole] = useState<Role | undefined>();
  const [roleFormDesignation, setRoleFormDesignation] = useState('');
  const [showRightsModal, setShowRightsModal] = useState<Role | null>(null);
  const [rightsSelection, setRightsSelection] = useState<number[]>([]);
  const [savingRights, setSavingRights] = useState(false);
  const [savingRole, setSavingRole] = useState(false);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  const loadRoles = () => {
    setLoading(true);
    setError('');
    roleService
      .findAll()
      .then(setRoles)
      .catch((e) => setError(e?.response?.data?.message || e?.message || 'Erreur chargement rôles'))
      .finally(() => setLoading(false));
  };

  const loadPermissions = () => {
    permissionService.findAll().then(setPermissions).catch(() => setPermissions([]));
  };

  useEffect(() => {
    loadRoles();
    loadPermissions();
  }, []);

  const openCreateRole = () => {
    setEditingRole(undefined);
    setRoleFormDesignation('');
    setShowRoleForm(true);
  };

  const openEditRole = (r: Role) => {
    setEditingRole(r);
    setRoleFormDesignation(r.designation);
    setShowRoleForm(true);
  };

  const handleSaveRole = async () => {
    const d = roleFormDesignation.trim();
    if (!d) return;
    setError('');
    setSavingRole(true);
    try {
      if (editingRole) {
        await roleService.update(editingRole.id, d);
      } else {
        await roleService.create({ designation: d });
      }
      setShowRoleForm(false);
      setEditingRole(undefined);
      setRoleFormDesignation('');
      loadRoles();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Erreur enregistrement');
    } finally {
      setSavingRole(false);
    }
  };

  const openRights = (role: Role) => {
    setShowRightsModal(role);
    setRightsSelection([]);
    roleService
      .findById(role.id)
      .then((full) => {
        setShowRightsModal(full);
        setRightsSelection((full.permissions || []).map((p) => p.id));
      })
      .catch(() => setRightsSelection([]));
  };

  const handleSaveRights = async () => {
    if (!showRightsModal) return;
    setSavingRights(true);
    setError('');
    try {
      await roleService.updateRights(showRightsModal.id, rightsSelection);
      setShowRightsModal(null);
      loadRoles();
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Erreur enregistrement des droits');
    } finally {
      setSavingRights(false);
    }
  };

  const togglePermission = (permId: number) => {
    setRightsSelection((prev) =>
      prev.includes(permId) ? prev.filter((id) => id !== permId) : [...prev, permId]
    );
  };

  const performDelete = async (id: number) => {
    try {
      await roleService.delete(id);
      setDeleteConfirmId(null);
      loadRoles();
    } catch (e: any) {
      const msg = e?.response?.data?.error || e?.response?.data?.message || e?.message || 'Suppression impossible';
      setError(msg);
      showError(msg, 'Erreur');
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <h1 className="text-2xl sm:text-3xl font-bold text-slate-800">Rôles et droits</h1>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={loadRoles}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-slate-100 text-slate-700 rounded-lg hover:bg-slate-200 border border-slate-300 disabled:opacity-70 disabled:cursor-not-allowed"
            title="Actualiser"
          >
            {loading ? <Loader2 className="w-4 h-4 animate-spin" aria-hidden /> : <RefreshCw className="w-4 h-4" />}
            Actualiser
          </button>
          {hasPermission('ROLES_WRITE') && (
            <button
              onClick={openCreateRole}
              className="inline-flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              <Plus className="w-5 h-5" />
              Nouveau rôle
            </button>
          )}
        </div>
      </div>

      {error && (
        <div className="p-3 rounded-lg border border-red-200 bg-red-50 text-red-700">{error}</div>
      )}

      {/* Liste des rôles */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div className="px-4 py-3 border-b border-slate-200 bg-slate-50 flex items-center gap-2">
          <Shield className="w-5 h-5 text-slate-600" />
          <h2 className="text-lg font-semibold text-slate-800">Rôles</h2>
        </div>
        {loading ? (
          <div className="p-8 flex items-center justify-center gap-2 text-slate-500">
            <Loader2 className="w-5 h-5 animate-spin" />
            Chargement…
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead className="bg-slate-50 border-b border-slate-200">
                <tr>
                  <th className="text-left px-4 py-3 text-sm font-medium text-slate-600">Désignation</th>
                  <th className="text-right px-4 py-3 text-sm font-medium text-slate-600">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200">
                {roles.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 text-slate-800 font-medium">{r.designation}</td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1">
                        {hasPermission('ROLES_WRITE') && (
                          <>
                            <button
                              type="button"
                              onClick={() => openEditRole(r)}
                              className="p-2 rounded-lg hover:bg-slate-100 text-slate-600"
                              title="Modifier"
                            >
                              <Edit className="w-4 h-4" />
                            </button>
                            <button
                              type="button"
                              onClick={() => openRights(r)}
                              className="p-2 rounded-lg hover:bg-emerald-50 text-emerald-700"
                              title="Droits (permissions)"
                            >
                              <Shield className="w-4 h-4" />
                            </button>
                            <button
                              type="button"
                              onClick={() => setDeleteConfirmId(r.id)}
                              className="p-2 rounded-lg hover:bg-red-50 text-red-600"
                              title="Supprimer"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Table des permissions (lecture seule) */}
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div className="px-4 py-3 border-b border-slate-200 bg-slate-50">
          <h2 className="text-lg font-semibold text-slate-800">Liste des permissions</h2>
          <p className="text-sm text-slate-500 mt-1">
            Ces codes sont utilisés pour attribuer des droits aux rôles. Aucun formulaire de création.
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="text-left px-4 py-3 text-sm font-medium text-slate-600">Code RBAC</th>
                <th className="text-left px-4 py-3 text-sm font-medium text-slate-600">Désignation</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-200">
              {permissions.map((p) => (
                <tr key={p.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 font-mono text-sm text-slate-800">{p.coderbac}</td>
                  <td className="px-4 py-3 text-slate-600">{p.designation}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal formulaire rôle (créer / modifier) */}
      {showRoleForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6">
            <h3 className="text-xl font-bold text-slate-800 mb-4">
              {editingRole ? 'Modifier le rôle' : 'Nouveau rôle'}
            </h3>
            <div className="mb-4">
              <label className="block text-sm font-medium text-slate-700 mb-2">Désignation</label>
              <input
                type="text"
                value={roleFormDesignation}
                onChange={(e) => setRoleFormDesignation(e.target.value)}
                className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                placeholder="Ex: ADMIN, Gestionnaire"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => {
                  setShowRoleForm(false);
                  setEditingRole(undefined);
                }}
                className="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 hover:bg-slate-50"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={handleSaveRole}
                disabled={!roleFormDesignation.trim() || savingRole}
                className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-70 disabled:cursor-not-allowed"
              >
                {savingRole ? (
                  <>
                    <Loader2 className="w-5 h-5 shrink-0 animate-spin" aria-hidden />
                    <span>Enregistrement…</span>
                  </>
                ) : (
                  editingRole ? 'Enregistrer' : 'Créer'
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal droits (permissions) */}
      {showRightsModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 overflow-y-auto">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg max-h-[90vh] overflow-y-auto p-6 my-8">
            <h3 className="text-xl font-bold text-slate-800 mb-2">
              Droits du rôle : {showRightsModal.designation}
            </h3>
            <p className="text-sm text-slate-500 mb-4">
              Cochez les permissions à attribuer à ce rôle.
            </p>
            <div className="space-y-2 mb-6">
              {permissions.map((p) => (
                <label
                  key={p.id}
                  className="flex items-center gap-3 p-2 rounded-lg hover:bg-slate-50 cursor-pointer"
                >
                  <input
                    type="checkbox"
                    checked={rightsSelection.includes(p.id)}
                    onChange={() => togglePermission(p.id)}
                    className="w-4 h-4 rounded border-slate-300 text-blue-600"
                  />
                  <span className="font-mono text-sm text-slate-700">{p.coderbac}</span>
                  <span className="text-slate-500 text-sm">{p.designation}</span>
                </label>
              ))}
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setShowRightsModal(null)}
                className="px-4 py-2 border border-slate-300 rounded-lg text-slate-700 hover:bg-slate-50"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={handleSaveRights}
                disabled={savingRights}
                className="inline-flex items-center justify-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-70 disabled:cursor-not-allowed"
              >
                {savingRights ? (
                  <>
                    <Loader2 className="w-5 h-5 shrink-0 animate-spin" aria-hidden />
                    <span>Enregistrement…</span>
                  </>
                ) : (
                  'Enregistrer les droits'
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {deleteConfirmId !== null && (
        <QuestionDialog
          open
          title="Confirmer la suppression"
          message="Voulez-vous vraiment supprimer ce rôle ? Les utilisateurs rattachés doivent être réassignés avant."
          variant="danger"
          confirmLabel="Supprimer"
          onConfirm={() => performDelete(deleteConfirmId)}
          onCancel={() => setDeleteConfirmId(null)}
        />
      )}
    </div>
  );
};

export default RolesList;
