import Box from "@mui/material/Box";
import Container from "@mui/material/Container";
import Stack from "@mui/material/Stack";
import { useMemo } from "react";
import type { MainTemplateProps } from "../../types/shell.types";
import type { SidebarDomainSchema } from "../../types/domain.types";
import { ensureSecurityDomain, filterNavigationByTenant, getDefaultNavigationSchema } from "../../services/navigationSchema";
import { Sidebar } from "../organisms/Sidebar";

type MainTemplateShellProps = MainTemplateProps & {
  navigationSchema?: SidebarDomainSchema;
  onNavigate?: (route: string) => void;
};

export function MainTemplate({ children, trainingContext, navigationSchema, onNavigate }: MainTemplateShellProps) {
  const schema = useMemo(() => {
    const sourceSchema = navigationSchema ?? getDefaultNavigationSchema(trainingContext.tenant_id);
    const withSecurity = ensureSecurityDomain(sourceSchema);
    return filterNavigationByTenant(withSecurity, trainingContext.tenant_id);
  }, [navigationSchema, trainingContext.tenant_id]);

  const handleNavigate = (route: string) => {
    if (onNavigate) {
      onNavigate(route);
      return;
    }
    window.history.pushState({}, "", route);
  };

  return (
    <Box sx={{ minHeight: "100vh", bgcolor: "background.default" }} data-testid="shell-main-template">
      <Container maxWidth={false} sx={{ py: 3 }}>
        <Stack direction={{ xs: "column", md: "row" }} spacing={3}>
          <Box component="aside" sx={{ width: { xs: "100%", md: 320 } }}>
            <Sidebar schema={schema} onNavigate={handleNavigate} />
          </Box>
          <Box component="main" sx={{ flex: 1 }}>
            {children}
          </Box>
        </Stack>
      </Container>
    </Box>
  );
}
