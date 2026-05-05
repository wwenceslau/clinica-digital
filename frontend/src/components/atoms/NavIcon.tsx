/**
 * NavIcon — resolves an icon_id string to the corresponding MUI SvgIcon.
 *
 * Used by SidebarItem and SidebarGroup to render consistent navigation icons
 * without coupling icon imports to every consumer.
 */

import AdminPanelSettingsIcon from "@mui/icons-material/AdminPanelSettings";
import CalendarTodayIcon from "@mui/icons-material/CalendarToday";
import FolderIcon from "@mui/icons-material/Folder";
import HistoryIcon from "@mui/icons-material/History";
import LockIcon from "@mui/icons-material/Lock";
import MedicalServicesIcon from "@mui/icons-material/MedicalServices";
import PeopleIcon from "@mui/icons-material/People";
import PersonIcon from "@mui/icons-material/Person";
import ReceiptIcon from "@mui/icons-material/Receipt";
import SettingsIcon from "@mui/icons-material/Settings";
import ShieldIcon from "@mui/icons-material/Shield";
import ScienceIcon from "@mui/icons-material/Science";
import HealthAndSafetyIcon from "@mui/icons-material/HealthAndSafety";
import VerifiedUserIcon from "@mui/icons-material/VerifiedUser";
import type { SvgIconProps } from "@mui/material/SvgIcon";

type IconComponent = React.ComponentType<SvgIconProps>;

const ICON_MAP: Record<string, IconComponent> = {
  admin_panel_settings: AdminPanelSettingsIcon,
  calendar_today: CalendarTodayIcon,
  history: HistoryIcon,
  lock: LockIcon,
  medical_services: MedicalServicesIcon,
  people: PeopleIcon,
  person: PersonIcon,
  receipt: ReceiptIcon,
  settings: SettingsIcon,
  shield: ShieldIcon,
  science: ScienceIcon,
  health_and_safety: HealthAndSafetyIcon,
  verified_user: VerifiedUserIcon,
};

interface NavIconProps extends SvgIconProps {
  iconId: string;
}

export function NavIcon({ iconId, ...props }: NavIconProps) {
  const Icon = ICON_MAP[iconId] ?? FolderIcon;
  return <Icon {...props} />;
}
