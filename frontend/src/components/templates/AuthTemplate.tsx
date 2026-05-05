/**
 * T058 [US4] AuthTemplate layout template.
 *
 * Unauthenticated layout: full-viewport centered card.
 * Wraps login and other pre-auth screens.
 *
 * Refs: FR-004
 */

import Box from '@mui/material/Box';
import Container from '@mui/material/Container';

interface AuthTemplateProps {
  children: React.ReactNode;
}

export function AuthTemplate({ children }: AuthTemplateProps) {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        bgcolor: 'background.default',
      }}
      data-testid="auth-template"
    >
      <Container maxWidth="sm">{children}</Container>
    </Box>
  );
}
