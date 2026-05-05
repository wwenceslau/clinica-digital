/**
 * T098 AdminUsuariosPage — route /admin/usuarios
 *
 * Displays the list of profile-20 users in the admin's tenant and provides
 * the "Criar Usuário" button that opens CreateUserModal.
 *
 * Refs: FR-006, FR-007, FR-013
 */

import React, { useState } from 'react';
import { Box, Button, Typography } from '@mui/material';
import { CreateUserModal } from '../components/organisms/CreateUserModal';

// Default values for the prototype; in a real integration these come from
// the authenticated user's context (location/role from /api/users/me/context).
const DEFAULT_LOCATION_ID = '';
const DEFAULT_ROLE_CODE = 'CBO-251510';

export function AdminUsuariosPage(): React.ReactElement {
  const [modalOpen, setModalOpen] = useState(false);

  function handleClose(success?: boolean) {
    setModalOpen(false);
    if (success) {
      // In a real implementation, refresh the user list here
    }
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
        <Typography variant="h5" component="h1">
          Gerenciamento de Usuários
        </Typography>
        <Button
          variant="contained"
          onClick={() => setModalOpen(true)}
          data-testid="btn-create-user"
        >
          Criar Usuário
        </Button>
      </Box>

      <Typography variant="body2" color="text.secondary">
        Lista de usuários do tenant será exibida aqui.
      </Typography>

      <CreateUserModal
        open={modalOpen}
        onClose={handleClose}
        locationId={DEFAULT_LOCATION_ID}
        roleCode={DEFAULT_ROLE_CODE}
      />
    </Box>
  );
}
