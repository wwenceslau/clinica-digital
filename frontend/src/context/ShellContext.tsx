import { createContext, useContext } from "react";
import type { ReactNode } from "react";
import type { HeaderContextData, ShellTelemetryMetadataState, ShellTrainingContext } from "../types/shell.types";

type ShellContextValue = {
  trainingContext: ShellTrainingContext;
  headerContext?: HeaderContextData;
  telemetry?: ShellTelemetryMetadataState;
};

const ShellContext = createContext<ShellContextValue | undefined>(undefined);

type ShellProviderProps = {
  children: ReactNode;
  value: ShellContextValue;
};

export function ShellContextProvider({ children, value }: ShellProviderProps) {
  return <ShellContext.Provider value={value}>{children}</ShellContext.Provider>;
}

export function useShellContext(): ShellContextValue {
  const ctx = useContext(ShellContext);
  if (!ctx) {
    throw new Error("useShellContext must be used inside ShellContextProvider");
  }
  return ctx;
}
