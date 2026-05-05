/**
 * T076 [US5] AuthContext — authentication state for the Shell.
 *
 * Holds the authenticated session and exposes login/logout callbacks.
 * Session is stored in React state only (no localStorage); clearing on logout
 * or session expiry redirects to /login per FR-024.
 *
 * Refs: FR-012, FR-024
 */

import { createContext, useContext, useState } from 'react';
import type { ReactNode } from 'react';
import type { SessionIssuedResponse } from '../services/iamAuthApi';

export interface AuthContextValue {
  /** True when a valid session is held in state. */
  isAuthenticated: boolean;
  /** The current session, or null when unauthenticated. */
  session: SessionIssuedResponse | null;
  /** Store a new session after successful login. */
  login: (session: SessionIssuedResponse) => void;
  /** Clear session state (triggers route guard redirect to /login). */
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [session, setSession] = useState<SessionIssuedResponse | null>(null);

  function login(newSession: SessionIssuedResponse) {
    setSession(newSession);
  }

  function logout() {
    setSession(null);
  }

  return (
    <AuthContext.Provider
      value={{ isAuthenticated: session !== null, session, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return ctx;
}
