import { Outlet, Link, useLocation } from 'react-router-dom';
import { useUiStore } from '@/stores/uiStore';
import { Menu, Building2, TreePine, Calendar, Clock } from 'lucide-react';

export function AdminLayout() {
  const { sidebarOpen, toggleSidebar } = useUiStore();
  const location = useLocation();

  const navSections = [
    {
      label: 'Master Data',
      items: [
        { path: '/admin/master-data/campuses', label: 'Campuses', icon: Building2 },
        { path: '/admin/master-data/hierarchy/1', label: 'Hierarchy Tree', icon: TreePine },
      ],
    },
    {
      label: 'Scheduling Config',
      items: [
        { path: '/admin/scheduling-config/calendars', label: 'Calendars', icon: Calendar },
        { path: '/admin/scheduling-config/grids', label: 'Grids', icon: Clock },
      ],
    },
  ];

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <aside
        className={`${sidebarOpen ? 'w-64' : 'w-16'} bg-white border-r border-gray-200 transition-all duration-300 flex flex-col`}
      >
        <div className="p-4 border-b border-gray-200 flex items-center justify-between">
          {sidebarOpen && <h1 className="text-lg font-semibold text-gray-900">UTMS Admin</h1>}
          <button
            onClick={toggleSidebar}
            className="p-2 rounded-md hover:bg-gray-100"
            aria-label="Toggle sidebar"
          >
            <Menu className="w-5 h-5" />
          </button>
        </div>

        <nav className="flex-1 p-2 space-y-1">
          {navSections.map((section) => (
            <div key={section.label}>
              <div className="px-3 py-2 text-xs font-medium text-gray-500 uppercase">
                {sidebarOpen && section.label}
              </div>
              {section.items.map((item) => {
                const Icon = item.icon;
                const isActive = location.pathname.startsWith(item.path.split('/').slice(0, 4).join('/'));
                return (
                  <Link
                    key={item.path}
                    to={item.path}
                    className={`flex items-center gap-3 px-3 py-2 rounded-md text-sm ${
                      isActive
                        ? 'bg-blue-50 text-blue-700 font-medium'
                        : 'text-gray-700 hover:bg-gray-100'
                    }`}
                  >
                    <Icon className="w-5 h-5 flex-shrink-0" />
                    {sidebarOpen && <span>{item.label}</span>}
                  </Link>
                );
              })}
            </div>
          ))}
        </nav>
      </aside>

      {/* Main Content */}
      <main className="flex-1 overflow-auto">
        <div className="max-w-[1440px] mx-auto p-6">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
