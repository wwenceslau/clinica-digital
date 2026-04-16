import type { ReactNode } from "react";

export type ShellTheme = "light" | "dark";
export type ShellLocale = "pt-BR" | "en-US";

export interface ShellTrainingContext {
  tenant_id: string;
  trace_id: string;
  user_id: string;
  role: string[];
}

export interface MainTemplateProps {
  children: ReactNode;
  initialTheme?: ShellTheme;
  initialLocale?: ShellLocale;
  trainingContext: ShellTrainingContext;
}

export interface LocationOption {
  location_id: string;
  location_name: string;
  location_type?: string;
}

export interface HeaderContextData {
  tenant_name: string;
  tenant_id: string;
  available_locations: LocationOption[];
  active_location_id: string;
  active_location_name: string;
  practitioner_id: string;
  practitioner_name: string;
  practitioner_role: string;
  practitioner_avatar_url?: string;
}

export interface ShellTelemetryMetadataState {
  trace_id: string;
  tenant_id: string;
  user_id: string;
  timestamp_iso: string;
  visible: boolean;
  debug_mode: boolean;
}
