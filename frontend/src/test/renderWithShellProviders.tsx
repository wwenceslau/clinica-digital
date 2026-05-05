import { render } from "@testing-library/react";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import { I18nextProvider } from "react-i18next";
import type { ReactElement } from "react";
import i18n from "../i18n/config";
import { AuthProvider } from "../context/AuthContext";
import { LocaleContextProvider } from "../context/LocaleContext";
import { ShellContextProvider } from "../context/ShellContext";
import { TenantProvider } from "../context/TenantContext";
import { ThemeContextProvider } from "../context/ThemeContext";
import type { ShellTrainingContext } from "../types/shell.types";

const fallbackTrainingContext: ShellTrainingContext = {
  tenant_id: "tenant-test",
  trace_id: "trace-test",
  user_id: "user-test",
  role: ["support"],
};

type RenderOptions = {
  trainingContext?: ShellTrainingContext;
};

export function renderWithShellProviders(ui: ReactElement, options: RenderOptions = {}) {
  const trainingContext = options.trainingContext ?? fallbackTrainingContext;
  const muiTheme = createTheme();

  return render(
    <I18nextProvider i18n={i18n}>
      <ThemeProvider theme={muiTheme}>
        <AuthProvider>
          <TenantProvider>
            <ThemeContextProvider userId={trainingContext.user_id}>
              <LocaleContextProvider userId={trainingContext.user_id}>
                <ShellContextProvider value={{ trainingContext }}>{ui}</ShellContextProvider>
              </LocaleContextProvider>
            </ThemeContextProvider>
          </TenantProvider>
        </AuthProvider>
      </ThemeProvider>
    </I18nextProvider>,
  );
}
