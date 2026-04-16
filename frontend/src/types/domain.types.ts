export interface SidebarResourceItem {
  resource_id: string;
  label_key: string;
  icon_id: string;
  route: string;
  domain_id: string;
  permission_key: string;
  permitted: boolean;
  disabled_reason?: string;
  metadata?: {
    badge_count?: number;
    new_badge?: boolean;
    feature_flag?: boolean;
    tenant_ids?: string[];
  };
}

export interface SidebarDomainGroup {
  domain_id: string;
  domain_label_key: string;
  description?: string;
  resources: SidebarResourceItem[];
  visible?: boolean;
  disabled_reason?: string;
}

export interface SidebarDomainSchema {
  version?: string;
  tenant_id?: string;
  domains: SidebarDomainGroup[];
}
