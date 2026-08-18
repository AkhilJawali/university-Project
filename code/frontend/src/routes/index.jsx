import { createBrowserRouter } from 'react-router-dom';
import { lazy, Suspense } from 'react';
import { AdminLayout } from '@/layouts/AdminLayout';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';

const CampusListPage = lazy(() => import('@/features/master-data/campus/CampusListPage'));
const CampusFormPage = lazy(() => import('@/features/master-data/campus/CampusFormPage'));
const DepartmentListPage = lazy(() => import('@/features/master-data/department/DepartmentListPage'));
const DepartmentFormPage = lazy(() => import('@/features/master-data/department/DepartmentFormPage'));
const ProgramListPage = lazy(() => import('@/features/master-data/program/ProgramListPage'));
const ProgramFormPage = lazy(() => import('@/features/master-data/program/ProgramFormPage'));
const BatchListPage = lazy(() => import('@/features/master-data/batch/BatchListPage'));
const BatchFormPage = lazy(() => import('@/features/master-data/batch/BatchFormPage'));
const SectionListPage = lazy(() => import('@/features/master-data/section/SectionListPage'));
const SectionFormPage = lazy(() => import('@/features/master-data/section/SectionFormPage'));
const HierarchyTreePage = lazy(() => import('@/features/master-data/hierarchy/HierarchyTreePage'));

// Scheduling Config
const CalendarListPage = lazy(() => import('@/features/scheduling-config/calendar/CalendarListPage'));
const CalendarFormPage = lazy(() => import('@/features/scheduling-config/calendar/CalendarFormPage'));
const CalendarDetailPage = lazy(() => import('@/features/scheduling-config/calendar/CalendarDetailPage'));
const GridListPage = lazy(() => import('@/features/scheduling-config/grid/GridListPage'));
const GridFormPage = lazy(() => import('@/features/scheduling-config/grid/GridFormPage'));
const GridDetailPage = lazy(() => import('@/features/scheduling-config/grid/GridDetailPage'));

function SuspenseWrapper({ children }) {
  return <Suspense fallback={<LoadingSkeleton rows={10} columns={5} />}>{children}</Suspense>;
}

export const router = createBrowserRouter([
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      {
        path: 'master-data/campuses',
        element: <SuspenseWrapper><CampusListPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/new',
        element: <SuspenseWrapper><CampusFormPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId',
        element: <SuspenseWrapper><CampusFormPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments',
        element: <SuspenseWrapper><DepartmentListPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments/new',
        element: <SuspenseWrapper><DepartmentFormPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments/:deptId',
        element: <SuspenseWrapper><DepartmentFormPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments/:deptId/programs',
        element: <SuspenseWrapper><ProgramListPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments/:deptId/programs/new',
        element: <SuspenseWrapper><ProgramFormPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments/:deptId/programs/:progId',
        element: <SuspenseWrapper><ProgramFormPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches',
        element: <SuspenseWrapper><BatchListPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches/new',
        element: <SuspenseWrapper><BatchFormPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches/:batchId',
        element: <SuspenseWrapper><BatchFormPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches/:batchId/sections',
        element: <SuspenseWrapper><SectionListPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches/:batchId/sections/new',
        element: <SuspenseWrapper><SectionFormPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/campuses/:campusId/departments/:deptId/programs/:progId/batches/:batchId/sections/:sectionId',
        element: <SuspenseWrapper><SectionFormPage /></SuspenseWrapper>,
      },
      {
        path: 'master-data/hierarchy/:campusId',
        element: <SuspenseWrapper><HierarchyTreePage /></SuspenseWrapper>,
      },
      // Scheduling Config — Calendars
      {
        path: 'scheduling-config/calendars',
        element: <SuspenseWrapper><CalendarListPage /></SuspenseWrapper>,
      },
      {
        path: 'scheduling-config/calendars/new',
        element: <SuspenseWrapper><CalendarFormPage /></SuspenseWrapper>,
      },
      {
        path: 'scheduling-config/calendars/:calendarId',
        element: <SuspenseWrapper><CalendarDetailPage /></SuspenseWrapper>,
      },
      {
        path: 'scheduling-config/calendars/:calendarId/edit',
        element: <SuspenseWrapper><CalendarFormPage /></SuspenseWrapper>,
      },
      // Scheduling Config — Grids
      {
        path: 'scheduling-config/grids',
        element: <SuspenseWrapper><GridListPage /></SuspenseWrapper>,
      },
      {
        path: 'scheduling-config/grids/new',
        element: <SuspenseWrapper><GridFormPage /></SuspenseWrapper>,
      },
      {
        path: 'scheduling-config/grids/:gridId',
        element: <SuspenseWrapper><GridDetailPage /></SuspenseWrapper>,
      },
      {
        path: 'scheduling-config/grids/:gridId/edit',
        element: <SuspenseWrapper><GridFormPage /></SuspenseWrapper>,
      },
      {
        index: true,
        element: <SuspenseWrapper><CampusListPage /></SuspenseWrapper>,
      },
    ],
  },
  {
    path: '/',
    element: <SuspenseWrapper><CampusListPage /></SuspenseWrapper>,
  },
  {
    path: '*',
    element: (
      <div className="flex flex-col items-center justify-center h-screen">
        <h1 className="text-2xl font-semibold text-gray-900 mb-2">Page not found</h1>
        <p className="text-sm text-gray-600">The page you are looking for does not exist.</p>
      </div>
    ),
  },
]);
