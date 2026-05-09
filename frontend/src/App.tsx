import { Outlet } from "react-router-dom";
import { Navbar } from "./components/Navbar";
import { Sidebar } from "./components/Sidebar";

export function App() {
  return (
    <div className="min-h-screen bg-surface flex flex-col font-sans">
      <Navbar />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar />
        <main className="flex-1 overflow-y-auto p-8 max-w-7xl mx-auto w-full ml-60">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
