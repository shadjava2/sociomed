import React, { createContext, useContext, useState, useEffect } from 'react';
import { authService, JwtResponse } from '../services/authService';

interface AuthContextType {
  user: JwtResponse | null;
  login: (username: string, password: string, totpCode?: string) => Promise<void>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  isLoading: boolean;
  hasPermission: (code: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<JwtResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const currentUser = authService.getCurrentUser();
    setUser(currentUser);
    setIsLoading(false);
  }, []);

  const login = async (username: string, password: string, totpCode?: string) => {
    // Backend attend le champ "otp" (6 chiffres) uniquement si 2FA activé
    const userData = await authService.login({ username, password, ...(totpCode ? { otp: totpCode } : {}) });
    setUser(userData);
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
  };

  // Si pas de tableau permissions (ancienne session / JWT sans permissions), on considère accès complet pour éviter page blanche
  const hasPermission = (code: string) => {
    if (!user) return false;
    if (!Array.isArray(user.permissions)) return true;
    return user.permissions.includes(code);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        login,
        logout,
        isAuthenticated: !!user,
        isLoading,
        hasPermission,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
