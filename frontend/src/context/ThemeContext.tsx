import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import type { ShellTheme } from "../types/shell.types";

type ThemeContextValue = {
  theme: ShellTheme;
  setTheme: (theme: ShellTheme) => void;
  toggleTheme: () => void;
};

const DEFAULT_THEME: ShellTheme = "light";

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

function getThemeKey(userId: string): string {
  return `shell.theme.${userId}`;
}

type ThemeProviderProps = {
  children: ReactNode;
  userId?: string;
  initialTheme?: ShellTheme;
};

export function ThemeContextProvider({
  children,
  userId = "anonymous",
  initialTheme = DEFAULT_THEME,
}: ThemeProviderProps) {
  const [theme, setThemeState] = useState<ShellTheme>(initialTheme);

  useEffect(() => {
    const stored = window.localStorage.getItem(getThemeKey(userId));
    if (stored === "light" || stored === "dark") {
      setThemeState(stored);
    }
  }, [userId]);

  useEffect(() => {
    document.documentElement.setAttribute("data-theme", theme);
    window.localStorage.setItem(getThemeKey(userId), theme);
  }, [theme, userId]);

  const value = useMemo<ThemeContextValue>(
    () => ({
      theme,
      setTheme: (nextTheme) => setThemeState(nextTheme),
      toggleTheme: () => setThemeState((current) => (current === "light" ? "dark" : "light")),
    }),
    [theme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useThemeContext(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error("useThemeContext must be used inside ThemeContextProvider");
  }
  return ctx;
}
