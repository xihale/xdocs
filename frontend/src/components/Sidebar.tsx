import { useNavigate, useLocation } from "react-router-dom";
import { useAuthStore } from "../stores/auth";
import { teamApi, kbApi } from "../api";
import { useEffect, useState } from "react";
import type { TeamVO, KnowledgeBase } from "../api/types";
import {
  FileText,
  Users,
  BookOpen,
} from "lucide-react";

export function Sidebar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuthStore();
  const [teams, setTeams] = useState<TeamVO[]>([]);
  const [kbs, setKbs] = useState<KnowledgeBase[]>([]);

  useEffect(() => {
    if (!user) return;
    teamApi.list().then(setTeams).catch(() => {});
    kbApi.listMine().then(setKbs).catch(() => {});
  }, [user]);

  return (
    <aside className="w-60 bg-surface-container-low border-r border-outline-variant flex flex-col fixed left-0 top-14 bottom-0 overflow-y-auto">
      {/* 快捷入口 */}
      <div className="p-3 space-y-1">
        <SidebarItem
          icon={<FileText className="w-4 h-4" />}
          label="首页"
          active={location.pathname === "/"}
          onClick={() => navigate("/")}
        />
        <SidebarItem
          icon={<Users className="w-4 h-4" />}
          label="工作台"
          active={location.pathname === "/workspace"}
          onClick={() => navigate("/workspace")}
        />
      </div>

      {/* 我的 TEAM */}
      <Section title="我的 TEAM">
        {teams.map((team) => (
          <SidebarItem
            key={team.id}
            icon={
              <div className="w-4 h-4 bg-primary/20 flex items-center justify-center text-[8px] font-bold text-primary">
                {team.name.charAt(0)}
              </div>
            }
            label={team.name}
            active={location.pathname === `/team/${team.id}`}
            onClick={() => navigate(`/team/${team.id}`)}
          />
        ))}
      </Section>

      {/* 我的知识库 */}
      <Section title="我的知识库">
        {kbs.map((kb) => (
          <SidebarItem
            key={kb.id}
            icon={<BookOpen className="w-4 h-4" />}
            label={kb.name}
            active={location.pathname === `/kb/${kb.id}`}
            onClick={() => navigate(`/kb/${kb.id}`)}
          />
        ))}
      </Section>
    </aside>
  );
}

function SidebarItem({
  icon,
  label,
  active,
  onClick,
}: {
  icon: React.ReactNode;
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={`w-full flex items-center gap-2.5 px-3 py-2 text-sm transition-colors ${
        active
          ? "bg-primary/10 text-primary font-medium"
          : "text-on-surface-variant hover:bg-surface-container"
      }`}
    >
      {icon}
      <span className="truncate">{label}</span>
    </button>
  );
}

function Section({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="px-3 pt-4">
      <div className="flex items-center justify-between px-3 mb-1">
        <span className="text-[11px] font-bold text-on-surface-variant uppercase tracking-wider">
          {title}
        </span>
      </div>
      <div className="space-y-0.5">{children}</div>
    </div>
  );
}
