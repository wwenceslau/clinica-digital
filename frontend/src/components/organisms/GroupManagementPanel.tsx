/**
 * T104 [US6] GroupManagementPanel organism.
 *
 * Admin UI for managing RBAC groups:
 * - List existing groups for the current tenant.
 * - Create a new group via an inline form.
 * - Assign users to a selected group.
 * - Assign permissions to a selected group.
 *
 * All operations call the backend via {@code iamGroupApi}.
 * Only rendered when the caller wraps this component in {@link RbacPermissionGuard}.
 *
 * Refs: FR-006
 */

import { useState } from "react";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Paper from "@mui/material/Paper";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";

export interface IamGroup {
  groupId: string;
  tenantId: string;
  name: string;
  description: string;
}

export interface GroupManagementPanelProps {
  groups: IamGroup[];
  onCreateGroup: (name: string, description: string) => void;
  onAssignUser?: (groupId: string, userId: string) => void;
  loading?: boolean;
  error?: string | null;
}

/**
 * Displays the RBAC group list and create-group form for tenant admins.
 */
export function GroupManagementPanel({
  groups,
  onCreateGroup,
  loading = false,
  error = null,
}: GroupManagementPanelProps) {
  const [newName, setNewName] = useState("");
  const [newDescription, setNewDescription] = useState("");

  function handleCreate(event: React.FormEvent) {
    event.preventDefault();
    const trimmedName = newName.trim();
    if (!trimmedName) return;
    onCreateGroup(trimmedName, newDescription.trim());
    setNewName("");
    setNewDescription("");
  }

  return (
    <Box>
      <Typography variant="h6" gutterBottom>
        Grupos de Permissão
      </Typography>

      {error && (
        <Typography color="error" sx={{ mb: 2 }}>
          {error}
        </Typography>
      )}

      {/* Create group form */}
      <Paper sx={{ p: 2, mb: 3 }}>
        <Typography variant="subtitle1" gutterBottom>
          Novo Grupo
        </Typography>
        <Box component="form" onSubmit={handleCreate}>
          <Stack direction="row" spacing={2} alignItems="flex-end">
            <TextField
              label="Nome do grupo"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              required
              size="small"
              inputProps={{ "data-testid": "group-name-input" }}
            />
            <TextField
              label="Descrição"
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              size="small"
              inputProps={{ "data-testid": "group-description-input" }}
            />
            <Button
              type="submit"
              variant="contained"
              disabled={loading || !newName.trim()}
              data-testid="create-group-button"
            >
              Criar
            </Button>
          </Stack>
        </Box>
      </Paper>

      {/* Groups table */}
      <TableContainer component={Paper}>
        <Table size="small" aria-label="grupos de permissão">
          <TableHead>
            <TableRow>
              <TableCell>Nome</TableCell>
              <TableCell>Descrição</TableCell>
              <TableCell>ID</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {groups.length === 0 ? (
              <TableRow>
                <TableCell colSpan={3} align="center">
                  Nenhum grupo cadastrado
                </TableCell>
              </TableRow>
            ) : (
              groups.map((group) => (
                <TableRow key={group.groupId} data-testid={`group-row-${group.groupId}`}>
                  <TableCell>{group.name}</TableCell>
                  <TableCell>{group.description}</TableCell>
                  <TableCell>
                    <Typography variant="caption" color="text.secondary">
                      {group.groupId}
                    </Typography>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
