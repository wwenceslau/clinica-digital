/**
 * T035 [US2] LocationSelector molecule.
 *
 * A WAI-ARIA compliant combobox that lets the practitioner switch their active
 * clinical location within the shell header.
 *
 * Props:
 *   - locations: list of available LocationOption objects
 *   - activeLocationId: the ID of the currently selected location
 *   - onSelect: called with the new location_id when the user picks a location
 *
 * Accessibility contract (required for T034 HeaderA11y tests):
 *   - root element: role="combobox" + aria-expanded + aria-label="Localização ativa"
 *   - dropdown list: role="listbox"
 *   - each option: role="option" + aria-selected (true for active)
 *   - keyboard: Enter/Space opens, Escape closes, ArrowDown/Up navigates
 *
 * Refs: FR-006, SC-004, NFR-003
 */

import { useEffect, useRef, useState } from "react";
import Box from "@mui/material/Box";
import IconButton from "@mui/material/IconButton";
import Paper from "@mui/material/Paper";
import Typography from "@mui/material/Typography";
import KeyboardArrowDownIcon from "@mui/icons-material/KeyboardArrowDown";
import KeyboardArrowUpIcon from "@mui/icons-material/KeyboardArrowUp";
import LocationOnIcon from "@mui/icons-material/LocationOn";
import type { LocationOption } from "../../types/shell.types";

export interface LocationSelectorProps {
  locations: LocationOption[];
  activeLocationId: string;
  onSelect: (locationId: string) => void;
}

export function LocationSelector({ locations, activeLocationId, onSelect }: LocationSelectorProps) {
  const [open, setOpen] = useState(false);
  const [focusedIndex, setFocusedIndex] = useState(-1);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  const activeLocation = locations.find((l) => l.location_id === activeLocationId);
  const activeName = activeLocation?.location_name ?? activeLocationId;

  function handleToggle() {
    setOpen((prev) => !prev);
    setFocusedIndex(-1);
  }

  function handleSelect(locationId: string) {
    onSelect(locationId);
    setOpen(false);
    triggerRef.current?.focus();
  }

  function handleTriggerKeyDown(e: React.KeyboardEvent<HTMLButtonElement>) {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      setOpen(true);
      setFocusedIndex(0);
    } else if (e.key === "Escape") {
      setOpen(false);
    }
  }

  function handleListKeyDown(e: React.KeyboardEvent<HTMLUListElement>) {
    if (e.key === "Escape") {
      e.preventDefault();
      setOpen(false);
      triggerRef.current?.focus();
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      setFocusedIndex((prev) => Math.min(prev + 1, locations.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setFocusedIndex((prev) => Math.max(prev - 1, 0));
    } else if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      if (focusedIndex >= 0 && focusedIndex < locations.length) {
        handleSelect(locations[focusedIndex].location_id);
      }
    }
  }

  // Focus the highlighted item when focusedIndex changes
  useEffect(() => {
    if (open && listRef.current && focusedIndex >= 0) {
      const items = listRef.current.querySelectorAll<HTMLLIElement>('[role="option"]');
      items[focusedIndex]?.focus();
    }
  }, [open, focusedIndex]);

  // Close when clicking outside
  useEffect(() => {
    if (!open) return;
    function handleOutside(e: MouseEvent) {
      if (
        triggerRef.current &&
        !triggerRef.current.contains(e.target as Node) &&
        listRef.current &&
        !listRef.current.contains(e.target as Node)
      ) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleOutside);
    return () => document.removeEventListener("mousedown", handleOutside);
  }, [open]);

  const listboxId = "location-selector-listbox";

  return (
    <Box sx={{ position: "relative", display: "inline-flex", alignItems: "center" }}>
      <IconButton
        ref={triggerRef}
        role="combobox"
        aria-label="Localização ativa"
        aria-expanded={open}
        aria-haspopup="listbox"
        aria-controls={open ? listboxId : undefined}
        data-testid="header-location-selector"
        onClick={handleToggle}
        onKeyDown={handleTriggerKeyDown}
        size="small"
        sx={{
          borderRadius: 1,
          px: 1.5,
          py: 0.5,
          gap: 0.5,
          color: "inherit",
          "&:hover": { backgroundColor: "rgba(255,255,255,0.1)" },
        }}
      >
        <LocationOnIcon fontSize="small" />
        <Typography variant="body2" component="span" noWrap sx={{ maxWidth: 180 }}>
          {activeName}
        </Typography>
        {open ? <KeyboardArrowUpIcon fontSize="small" /> : <KeyboardArrowDownIcon fontSize="small" />}
      </IconButton>

      {open && (
        <Paper
          elevation={4}
          sx={{
            position: "absolute",
            top: "100%",
            left: 0,
            mt: 0.5,
            minWidth: 220,
            zIndex: 1300,
          }}
        >
          <Box
            component="ul"
            id={listboxId}
            ref={listRef}
            role="listbox"
            aria-label="Selecione a localização"
            tabIndex={-1}
            onKeyDown={handleListKeyDown}
            sx={{ listStyle: "none", m: 0, p: 0.5 }}
          >
            {locations.map((loc, idx) => {
              const isActive = loc.location_id === activeLocationId;
              return (
                <Box
                  component="li"
                  key={loc.location_id}
                  role="option"
                  aria-selected={isActive}
                  tabIndex={focusedIndex === idx ? 0 : -1}
                  onClick={() => handleSelect(loc.location_id)}
                  sx={{
                    px: 2,
                    py: 1,
                    cursor: "pointer",
                    borderRadius: 0.5,
                    fontWeight: isActive ? 600 : 400,
                    backgroundColor: isActive ? "action.selected" : "transparent",
                    "&:hover, &:focus": {
                      backgroundColor: "action.hover",
                      outline: "none",
                    },
                  }}
                >
                  <Typography variant="body2">{loc.location_name}</Typography>
                </Box>
              );
            })}
          </Box>
        </Paper>
      )}
    </Box>
  );
}
