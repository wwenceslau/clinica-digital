import Collapse from "@mui/material/Collapse";
import List from "@mui/material/List";
import ListItemButton from "@mui/material/ListItemButton";
import ListItemText from "@mui/material/ListItemText";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import type { SidebarDomainGroup } from "../../types/domain.types";
import { SidebarItem } from "../atoms/SidebarItem";

type SidebarGroupProps = {
  group: SidebarDomainGroup;
  onNavigate?: (route: string) => void;
};

export function SidebarGroup({ group, onNavigate }: SidebarGroupProps) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);

  return (
    <li>
      <ListItemButton
        onClick={() => setExpanded((current) => !current)}
        aria-expanded={expanded}
        aria-controls={`sidebar-domain-${group.domain_id}`}
        data-testid={`sidebar-domain-toggle-${group.domain_id}`}
      >
        <ListItemText primary={t(group.domain_label_key)} />
      </ListItemButton>
      <Collapse in={expanded} timeout="auto" unmountOnExit>
        <List id={`sidebar-domain-${group.domain_id}`} disablePadding sx={{ pl: 1 }}>
          {group.resources.map((resource) => (
            <SidebarItem key={resource.resource_id} item={resource} onNavigate={onNavigate} />
          ))}
        </List>
      </Collapse>
    </li>
  );
}
