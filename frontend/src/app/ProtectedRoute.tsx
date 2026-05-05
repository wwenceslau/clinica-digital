/**
 * T078 [US5] ProtectedRoute — guards authenticated content.
 *
 * When the user is not authenticated, renders the login view inside AuthTemplate.
 * When the session has expired (expiresAt < now), calls logout() to clear state,
 * then falls back to the login view automatically (per FR-024).
 *
 * Usage:
 *   <ProtectedRoute>
 *     <MainTemplate ...>...</MainTemplate>
 *   </ProtectedRoute>
 *
 * Refs: FR-012, FR-024
 */

import { useEffect, type ReactNode } from 'react';
import { AuthTemplate } from '../components/templates/AuthTemplate';
import { LoginForm } from '../components/organisms/LoginForm';
import { useAuth } from '../context/AuthContext';
import type { SessionIssuedResponse } from '../services/iamAuthApi';

interface ProtectedRouteProps {
  children: ReactNode;
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, session, login, logout } = useAuth();

  // Session expiry check: if a session exists but its expiry is in the past,
  // clear it immediately so the login view is shown (FR-024).
  useEffect(() => {
    if (session && new Date(session.expiresAt) < new Date()) {
      logout();
    }
  }, [session, logout]);

  if (!isAuthenticated) {
    async function handleLogin(session: SessionIssuedResponse) {
      login(session);
    }

    return (
      <AuthTemplate>
        <LoginForm onLogin={handleLogin} />
      </AuthTemplate>
    );
  }

  return <>{children}</>;
}
