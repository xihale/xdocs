import { useNavigate, useLocation } from "react-router-dom";
import { useAuthStore } from "../stores/auth";
import { LogOut, FileText, Users, User, Sun, Moon } from "lucide-react";
import { useTheme } from "../hooks/useTheme";
import { NotificationPanel } from "./NotificationPanel";

export function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuthStore();
  const { setTheme, resolvedTheme } = useTheme();

  const navItems = [
    { path: "/", label: "首页", icon: FileText },
    { path: "/workspace", label: "工作台", icon: Users },
  ];

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <header className="bg-surface-container-lowest border-b border-outline-variant h-14 flex items-center justify-between px-6 sticky top-0 z-50">
      <div className="flex items-center gap-8">
        <button
          onClick={() => navigate("/")}
          className="text-lg font-bold text-on-surface font-mono tracking-wider uppercase"
        >
          XDocs
        </button>

        <nav className="flex items-center gap-1">
          {navItems.map((item) => (
            <button
              key={item.path}
              onClick={() => navigate(item.path)}
              className={`px-4 py-2 text-sm font-medium transition-colors ${
                location.pathname === item.path
                  ? "bg-primary/10 text-primary"
                  : "text-on-surface-variant hover:bg-surface-container"
              }`}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </div>

      <div className="flex items-center gap-3">
        <NotificationPanel />
        <button
          onClick={() => setTheme(resolvedTheme === "dark" ? "light" : "dark")}
          className="p-2 text-on-surface-variant hover:bg-surface-container transition-colors"
          title={resolvedTheme === "dark" ? "切换亮色模式" : "切换暗色模式"}
        >
          {resolvedTheme === "dark" ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
        </button>
        <button
          onClick={() => navigate("/profile")}
          className="flex items-center gap-2 px-3 py-1.5 hover:bg-surface-container transition-colors"
        >
          <div className="w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center">
            <User className="w-4 h-4 text-primary" />
          </div>
          <span className="text-sm font-medium text-on-surface">
            {user?.nickname || user?.username || "用户"}
          </span>
        </button>
        <button
          onClick={handleLogout}
          className="p-2 text-on-surface-variant hover:bg-surface-container transition-colors"
          title="退出登录"
        >
          <LogOut className="w-4 h-4" />
        </button>
      </div>
    </header>
  );
}
