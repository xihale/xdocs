import { createBrowserRouter, Navigate } from "react-router-dom";
import { App } from "../App";
import { LoginPage } from "../views/LoginPage";
import { DashboardPage } from "../views/DashboardPage";
import { WorkspacePage } from "../views/WorkspacePage";
import { TeamDetailPage } from "../views/TeamDetailPage";
import { KbDetailPage } from "../views/KbDetailPage";
import { ArticleDetailPage } from "../views/ArticleDetailPage";
import { ArticleEditPage } from "../views/ArticleEditPage";
import { ProfilePage } from "../views/ProfilePage";
import { AuthGuard } from "./AuthGuard";

export const router = createBrowserRouter([
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/article/:id/edit",
    element: <AuthGuard><ArticleEditPage /></AuthGuard>,
  },
  {
    path: "/",
    element: <AuthGuard><App /></AuthGuard>,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: "workspace", element: <WorkspacePage /> },
      { path: "team/:id", element: <TeamDetailPage /> },
      { path: "kb/:id", element: <KbDetailPage /> },
      { path: "article/:id", element: <ArticleDetailPage /> },
      { path: "profile", element: <ProfilePage /> },
      { path: "*", element: <Navigate to="/" replace /> },
    ],
  },
]);
