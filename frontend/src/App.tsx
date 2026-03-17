// src/App.tsx
import React, { Suspense, lazy } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { AppDialogProvider } from './contexts/AppDialogContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { Login } from './components/Login';
import { Layout } from './components/Layout';
import { SkeletonLayout } from './components/SkeletonLayout';
import { OfflineBanner } from './components/OfflineBanner';
import { InstallPWAPrompt } from './components/InstallPWAPrompt';
import { ToastProvider } from './components/Toast';
import { PdfViewerProvider } from './contexts/PdfViewerContext';

/** Redirige "/" vers la première route autorisée pour éviter boucle avec ProtectedRoute. */
function HomeRedirect() {
  const { hasPermission } = useAuth();
  if (hasPermission('MENU_DASHBOARD')) return <Navigate to="/tableau-de-bord" replace />;
  if (hasPermission('MENU_AGENTS')) return <Navigate to="/agents" replace />;
  if (hasPermission('MENU_SENATEURS')) return <Navigate to="/senateurs" replace />;
  if (hasPermission('MENU_USERS')) return <Navigate to="/users" replace />;
  if (hasPermission('MENU_ROLES')) return <Navigate to="/roles" replace />;
  if (hasPermission('MENU_HOPITAUX')) return <Navigate to="/hopitaux" replace />;
  if (hasPermission('MENU_PEC')) return <Navigate to="/pec" replace />;
  if (hasPermission('MENU_PARAMETRES')) return <Navigate to="/parametres" replace />;
  return (
    <Layout>
      <div className="p-6 text-center text-slate-600">
        Vous n&apos;avez accès à aucune section. Contactez l&apos;administrateur.
      </div>
    </Layout>
  );
}

// Lazy des pages pour réduire le bundle initial
const AgentsList = lazy(() => import('./pages/AgentsList').then((m) => ({ default: m.AgentsList })));
const AgentDetails = lazy(() => import('./components/AgentDetails').then((m) => ({ default: m.AgentDetails })));
const SenateursList = lazy(() => import('./pages/SenateursList').then((m) => ({ default: m.SenateursList })));
const SenateurDetails = lazy(() => import('./components/SenateurDetails').then((m) => ({ default: m.SenateurDetails })));
const UsersList = lazy(() => import('./pages/UsersList').then((m) => ({ default: m.UsersList })));
const HopitauxList = lazy(() => import('./pages/HopitauxList').then((m) => ({ default: m.HopitauxList })));
const Parametres = lazy(() => import('./pages/Parametres').then((m) => ({ default: m.Parametres })));
const DashboardPec = lazy(() => import('./pages/DashboardPec').then((m) => ({ default: m.DashboardPec })));
const PecList = lazy(() => import('./pages/PecList').then((m) => ({ default: m.PecList })));
const RolesList = lazy(() => import('./pages/RolesList').then((m) => ({ default: m.RolesList })));
const PublicPecVerifyPage = lazy(() => import('./pages/PublicPecVerifyPage'));
const ForgotPassword = lazy(() => import('./pages/ForgotPassword').then((m) => ({ default: m.ForgotPassword })));
const DossiersPage = lazy(() => import('./pages/DossiersPage').then((m) => ({ default: m.DossiersPage })));

function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
      <PdfViewerProvider>
      <AppDialogProvider>
      <AuthProvider>
        <OfflineBanner />
        <InstallPWAPrompt />
        <Suspense fallback={<SkeletonLayout />}>
          <Routes>
            <Route path="/public/pec/:token" element={<PublicPecVerifyPage />} />
            <Route path="/login" element={<Login />} />
            <Route path="/mot-de-passe-oublie" element={<ForgotPassword />} />
            <Route path="/" element={<ProtectedRoute><HomeRedirect /></ProtectedRoute>} />

            <Route
              path="/agents"
              element={
                <ProtectedRoute requiredPermission="MENU_AGENTS">
                  <Layout>
                    <AgentsList />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/tableau-de-bord"
              element={
                <ProtectedRoute requiredPermission="MENU_DASHBOARD">
                  <Layout>
                    <DashboardPec />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/pec"
              element={
                <ProtectedRoute requiredPermission="MENU_PEC">
                  <Layout>
                    <PecList />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/hopitaux"
              element={
                <ProtectedRoute requiredPermission="MENU_HOPITAUX">
                  <Layout>
                    <HopitauxList />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/agents/:id"
              element={
                <ProtectedRoute requiredPermission="MENU_AGENTS">
                  <Layout>
                    <AgentDetails />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/senateurs"
              element={
                <ProtectedRoute requiredPermission="MENU_SENATEURS">
                  <Layout>
                    <SenateursList />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/senateurs/:id"
              element={
                <ProtectedRoute requiredPermission="MENU_SENATEURS">
                  <Layout>
                    <SenateurDetails />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/users"
              element={
                <ProtectedRoute requiredPermission="MENU_USERS">
                  <Layout>
                    <UsersList />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/roles"
              element={
                <ProtectedRoute requiredPermission="MENU_ROLES">
                  <Layout>
                    <RolesList />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/parametres"
              element={
                <ProtectedRoute requiredPermission="MENU_PARAMETRES">
                  <Layout>
                    <Parametres />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/dossiers"
              element={
                <ProtectedRoute>
                  <Layout>
                    <DossiersPage />
                  </Layout>
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </AuthProvider>
      </AppDialogProvider>
      </PdfViewerProvider>
      </ToastProvider>
    </BrowserRouter>
  );
}

export default App;
