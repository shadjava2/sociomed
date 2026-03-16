// src/components/Layout.tsx
import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import {
  Users,
  CircleUser as UserCircle,
  LogOut,
  Menu,
  X,
  Award,
  LayoutGrid,
  Settings,
  Building2,
  Stethoscope,
  Shield,
} from 'lucide-react';

/** Petit composant logo (réutilisé dans sidebar desktop + drawer mobile) */
const SenateLogo: React.FC = () => (
  // 🔁 Dashboard comme “home”
  <Link to="/tableau-de-bord" className="flex items-center gap-3 px-3 py-4">
    <img
      src="/assets/senat-logo.png"
      alt="Sénat - Accueil"
      className="h-20 w-20 rounded-full object-contain ring-1 ring-black/10"
      loading="eager"
    />
    <span className="text-sm font-semibold text-slate-800 tracking-tight">
      Sénat
    </span>
  </Link>
);

export const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, logout, hasPermission } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const isActive = (path: string) => location.pathname === path;

  const navItems = [
    ...(hasPermission('MENU_AGENTS') ? [{ path: '/agents', label: 'Agents', icon: Users }] : []),
    ...(hasPermission('MENU_SENATEURS') ? [{ path: '/senateurs', label: 'Sénateurs', icon: Award }] : []),
    ...(hasPermission('MENU_USERS') ? [{ path: '/users', label: 'Utilisateurs', icon: UserCircle }] : []),
    ...(hasPermission('MENU_ROLES') ? [{ path: '/roles', label: 'Rôles et droits', icon: Shield }] : []),
  ];

  const extraItems = [
    ...(hasPermission('MENU_HOPITAUX') ? [{ path: '/hopitaux', label: 'Hôpitaux', icon: Building2 }] : []),
    ...(hasPermission('MENU_PEC') ? [{ path: '/pec', label: 'Prises en charge', icon: Stethoscope }] : []),
    ...(hasPermission('MENU_DASHBOARD') ? [{ path: '/tableau-de-bord', label: 'Tableau de bord', icon: LayoutGrid }] : []),
    ...(hasPermission('MENU_PARAMETRES') ? [{ path: '/parametres', label: 'Paramètres', icon: Settings }] : []),
  ];

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Header (bordeaux) */}
      <header className="sticky top-0 z-40 bg-[#800020] text-white">
        <div className="flex items-center justify-between h-14 px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setSidebarOpen(true)}
              className="md:hidden p-2 rounded-lg hover:bg-white/10"
              aria-label="Menu"
            >
              <Menu className="w-6 h-6" />
            </button>
            <h1 className="text-lg sm:text-xl font-bold tracking-tight">
              PRISE EN CHARGE MÉDICALE
            </h1>
          </div>

          {/* Mobile : raccourcis tableau de bord + déconnexion */}
          <div className="flex md:hidden items-center gap-2">
            <Link
              to="/tableau-de-bord"
              className="p-2 rounded-lg bg-white/10 hover:bg-white/20"
              title="Tableau de bord"
            >
              <LayoutGrid className="w-5 h-5" />
            </Link>
            <button
              onClick={handleLogout}
              className="p-2 rounded-lg bg-white/10 hover:bg-white/20"
              title="Déconnexion"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>

          {/* Desktop */}
          <div className="hidden md:flex items-center gap-4">
            <Link
              to="/tableau-de-bord"
              className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-white/10 hover:bg-white/20"
              title="Aller au tableau de bord"
            >
              <LayoutGrid className="w-4 h-4" />
              <span>Tableau de bord</span>
            </Link>
            <div className="text-right">
              <div className="text-sm font-semibold">
                {user?.prenom} {user?.nom}
              </div>
              <div className="text-xs opacity-90">
                {user?.role ?? 'Utilisateur'}
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-white/10 hover:bg-white/20"
            >
              <LogOut className="w-4 h-4" />
              <span>Déconnexion</span>
            </button>
          </div>
        </div>
      </header>

      {/* Sidebar (desktop) */}
      <aside
        className="
          hidden md:flex
          fixed
          top-14 bottom-0 left-0
          z-30 w-64 flex-col
          border-r border-slate-200
          bg-white
        "
      >
        <div className="flex-1 overflow-y-auto">
          {/* Logo en tête */}
          <div className="border-b border-slate-200">
            <SenateLogo />
          </div>

          <nav className="px-3 py-4 space-y-6">
            <div>
              <div className="px-3 text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                Navigation
              </div>
              <ul className="space-y-1">
                {navItems.map((item) => {
                  const Icon = item.icon;
                  const active = isActive(item.path);
                  return (
                    <li key={item.path}>
                      <Link
                        to={item.path}
                        className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors
                          ${active ? 'bg-[#800020] text-white' : 'text-slate-700 hover:bg-slate-100'}`}
                      >
                        <Icon className="w-4 h-4" />
                        <span className="text-sm font-medium">{item.label}</span>
                      </Link>
                    </li>
                  );
                })}
              </ul>
            </div>

            <div>
              <div className="px-3 text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                Modules
              </div>
              <ul className="space-y-1">
                {extraItems.map((item) => {
                  const Icon = item.icon;
                  const active = isActive(item.path);
                  return (
                    <li key={item.path}>
                      <Link
                        to={item.path}
                        className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors
                          ${active ? 'bg-[#800020] text-white' : 'text-slate-700 hover:bg-slate-100'}`}
                      >
                        <Icon className="w-4 h-4" />
                        <span className="text-sm font-medium">{item.label}</span>
                      </Link>
                    </li>
                  );
                })}
              </ul>
            </div>
          </nav>
        </div>

        <div className="p-3 border-t border-slate-200">
          <button
            onClick={handleLogout}
            className="w-full inline-flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-slate-700 hover:bg-slate-100"
          >
            <LogOut className="w-4 h-4" />
            <span>Déconnexion</span>
          </button>
        </div>
      </aside>

      {/* Sidebar (mobile drawer) */}
      {sidebarOpen && (
        <>
          <div
            className="fixed inset-0 z-40 bg-black/40 md:hidden"
            onClick={() => setSidebarOpen(false)}
          />
          <div
            className="
              fixed top-14 bottom-0 left-0
              z-50 md:hidden
              w-72 max-w-[85%]
              bg-white border-r border-slate-200
              flex flex-col
            "
          >
            <div className="flex items-center justify-between h-14 px-3 border-b border-slate-200">
              <SenateLogo />
              <button
                onClick={() => setSidebarOpen(false)}
                className="p-2 rounded-lg hover:bg-slate-100"
                aria-label="Fermer"
              >
                <X className="w-6 h-6" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto">
              <nav className="px-3 py-4 space-y-6">
                <div>
                  <div className="px-3 text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                    Navigation
                  </div>
                  <ul className="space-y-1">
                    {navItems.map((item) => {
                      const Icon = item.icon;
                      const active = isActive(item.path);
                      return (
                        <li key={item.path}>
                          <Link
                            to={item.path}
                            onClick={() => setSidebarOpen(false)}
                            className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors
                              ${active ? 'bg-[#800020] text-white' : 'text-slate-700 hover:bg-slate-100'}`}
                          >
                            <Icon className="w-4 h-4" />
                            <span className="text-sm font-medium">{item.label}</span>
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                </div>

                <div>
                  <div className="px-3 text-xs font-semibold text-slate-500 uppercase tracking-wide mb-2">
                    Modules
                  </div>
                  <ul className="space-y-1">
                    {extraItems.map((item) => {
                      const Icon = item.icon;
                      const active = isActive(item.path);
                      return (
                        <li key={item.path}>
                          <Link
                            to={item.path}
                            onClick={() => setSidebarOpen(false)}
                            className={`flex items-center gap-3 px-3 py-2 rounded-lg transition-colors
                              ${active ? 'bg-[#800020] text-white' : 'text-slate-700 hover:bg-slate-100'}`}
                          >
                            <Icon className="w-4 h-4" />
                            <span className="text-sm font-medium">{item.label}</span>
                          </Link>
                        </li>
                      );
                    })}
                  </ul>
                </div>
              </nav>
            </div>

            <div className="p-3 border-t border-slate-200">
              <button
                onClick={() => {
                  setSidebarOpen(false);
                  handleLogout();
                }}
                className="w-full inline-flex items-center justify-center gap-2 px-3 py-2 rounded-lg text-slate-700 hover:bg-slate-100"
              >
                <LogOut className="w-4 h-4" />
                <span>Déconnexion</span>
              </button>
            </div>
          </div>
        </>
      )}

      {/* Contenu — min-w-0 pour permettre le shrink des tables en scroll */}
      <main className="md:ml-64 px-4 sm:px-6 lg:px-8 pt-4 pb-6 min-w-0">
        {children}
      </main>
    </div>
  );
};
