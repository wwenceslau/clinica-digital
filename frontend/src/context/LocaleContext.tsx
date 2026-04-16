import { createContext, useContext, useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import type { ShellLocale } from "../types/shell.types";

type LocaleContextValue = {
  locale: ShellLocale;
  setLocale: (locale: ShellLocale) => void;
};

const DEFAULT_LOCALE: ShellLocale = "pt-BR";

const LocaleContext = createContext<LocaleContextValue | undefined>(undefined);

function getLocaleKey(userId: string): string {
  return `shell.locale.${userId}`;
}

type LocaleProviderProps = {
  children: ReactNode;
  userId?: string;
  initialLocale?: ShellLocale;
};

export function LocaleContextProvider({
  children,
  userId = "anonymous",
  initialLocale = DEFAULT_LOCALE,
}: LocaleProviderProps) {
  const [locale, setLocaleState] = useState<ShellLocale>(initialLocale);

  useEffect(() => {
    const stored = window.localStorage.getItem(getLocaleKey(userId));
    if (stored === "pt-BR" || stored === "en-US") {
      setLocaleState(stored);
    }
  }, [userId]);

  useEffect(() => {
    window.localStorage.setItem(getLocaleKey(userId), locale);
  }, [locale, userId]);

  const value = useMemo<LocaleContextValue>(
    () => ({
      locale,
      setLocale: (nextLocale) => setLocaleState(nextLocale),
    }),
    [locale],
  );

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useLocaleContext(): LocaleContextValue {
  const ctx = useContext(LocaleContext);
  if (!ctx) {
    throw new Error("useLocaleContext must be used inside LocaleContextProvider");
  }
  return ctx;
}
