import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useCalendar, useHolidays, useCreateHoliday, useDeleteHoliday, useExamWindows, useCreateExamWindow, useDeleteExamWindow, useSpecialPeriods, useCreateSpecialPeriod, useDeleteSpecialPeriod } from './hooks';
import { useSchedulingConfigStore } from '@/stores/schedulingConfigStore';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { holidaySchema, examWindowSchema, specialPeriodSchema } from './schemas';
import { CalendarTimeline } from './components/CalendarTimeline';
import { ImpactWarningBanner } from './components/ImpactWarningBanner';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';
import { ErrorState } from '@/components/ErrorState';
import { Pencil, Trash2, Plus, Calendar } from 'lucide-react';

export default function CalendarDetailPage() {
  const { calendarId } = useParams();
  const navigate = useNavigate();
  const numericId = Number(calendarId);

  const { activeCalendarTab, setActiveCalendarTab } = useSchedulingConfigStore();
  const [showForm, setShowForm] = useState(false);
  const [impactedSessions, setImpactedSessions] = useState(null);

  const { data: calendar, isLoading, isError, refetch } = useCalendar(numericId);
  const { data: holidays } = useHolidays(numericId);
  const { data: examWindows } = useExamWindows(numericId);
  const { data: specialPeriods } = useSpecialPeriods(numericId);

  const createHolidayMutation = useCreateHoliday(numericId);
  const deleteHolidayMutation = useDeleteHoliday(numericId);
  const createExamWindowMutation = useCreateExamWindow(numericId);
  const deleteExamWindowMutation = useDeleteExamWindow(numericId);
  const createSpecialPeriodMutation = useCreateSpecialPeriod(numericId);
  const deleteSpecialPeriodMutation = useDeleteSpecialPeriod(numericId);

  const tabs = [
    { key: 'holidays', label: 'Holidays' },
    { key: 'examWindows', label: 'Exam Windows' },
    { key: 'specialPeriods', label: 'Special Periods' },
  ];

  if (isLoading) return <LoadingSkeleton rows={8} columns={4} />;
  if (isError) return <ErrorState message="Failed to load calendar" onRetry={refetch} />;
  if (!calendar) return null;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">{calendar.name}</h1>
          <p className="text-sm text-gray-600 mt-1">
            {calendar.campusName} &middot; {calendar.academicYear} &middot; {calendar.semesterType} &middot;{' '}
            {calendar.startDate} to {calendar.endDate}
          </p>
        </div>
        <button
          onClick={() => navigate(`/admin/scheduling-config/calendars/${numericId}/edit`)}
          className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 text-sm font-medium rounded-md hover:bg-gray-200"
        >
          <Pencil className="w-4 h-4" />
          Edit
        </button>
      </div>

      {/* Timeline */}
      <CalendarTimeline
        startDate={calendar.startDate}
        endDate={calendar.endDate}
        holidays={holidays?.data || holidays || []}
        examWindows={examWindows?.data || examWindows || []}
        specialPeriods={specialPeriods?.data || specialPeriods || []}
      />

      {/* Impact Warning */}
      {impactedSessions && (
        <ImpactWarningBanner
          sessions={impactedSessions}
          onDismiss={() => setImpactedSessions(null)}
        />
      )}

      {/* Tabs */}
      <div className="border-b border-gray-200">
        <nav className="flex gap-4" aria-label="Calendar detail tabs">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => { setActiveCalendarTab(tab.key); setShowForm(false); }}
              className={`px-1 py-2 text-sm font-medium border-b-2 transition-colors ${
                activeCalendarTab === tab.key
                  ? 'border-blue-600 text-blue-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      {activeCalendarTab === 'holidays' && (
        <HolidaysTab
          holidays={holidays?.data || holidays || []}
          showForm={showForm}
          setShowForm={setShowForm}
          createMutation={createHolidayMutation}
          deleteMutation={deleteHolidayMutation}
          onImpact={setImpactedSessions}
        />
      )}
      {activeCalendarTab === 'examWindows' && (
        <ExamWindowsTab
          examWindows={examWindows?.data || examWindows || []}
          showForm={showForm}
          setShowForm={setShowForm}
          createMutation={createExamWindowMutation}
          deleteMutation={deleteExamWindowMutation}
        />
      )}
      {activeCalendarTab === 'specialPeriods' && (
        <SpecialPeriodsTab
          specialPeriods={specialPeriods?.data || specialPeriods || []}
          showForm={showForm}
          setShowForm={setShowForm}
          createMutation={createSpecialPeriodMutation}
          deleteMutation={deleteSpecialPeriodMutation}
        />
      )}
    </div>
  );
}

function HolidaysTab({ holidays, showForm, setShowForm, createMutation, deleteMutation, onImpact }) {
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: zodResolver(holidaySchema),
    defaultValues: { name: '', date: '', dayType: '', isRecurring: false },
  });

  const onSubmit = (data) => {
    createMutation.mutate(data, {
      onSuccess: (response) => {
        reset();
        setShowForm(false);
        if (response?.impactedSessions?.length > 0) {
          onImpact(response.impactedSessions);
        }
      },
    });
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-medium text-gray-900">Holidays ({holidays.length})</h2>
        <button
          onClick={() => setShowForm(!showForm)}
          className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700"
        >
          <Plus className="w-3.5 h-3.5" />
          Add Holiday
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit(onSubmit)} className="bg-gray-50 border border-gray-200 rounded-lg p-4 space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="holiday-name" className="block text-xs font-medium text-gray-600 mb-1">Name</label>
              <input id="holiday-name" {...register('name')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm" />
              {errors.name && <p className="text-xs text-red-600 mt-0.5">{errors.name.message}</p>}
            </div>
            <div>
              <label htmlFor="holiday-date" className="block text-xs font-medium text-gray-600 mb-1">Date</label>
              <input id="holiday-date" type="date" {...register('date')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm" />
              {errors.date && <p className="text-xs text-red-600 mt-0.5">{errors.date.message}</p>}
            </div>
            <div>
              <label htmlFor="holiday-type" className="block text-xs font-medium text-gray-600 mb-1">Type</label>
              <select id="holiday-type" {...register('dayType')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm">
                <option value="">Select type</option>
                <option value="NATIONAL">National</option>
                <option value="STATE">State</option>
                <option value="INSTITUTIONAL">Institutional</option>
                <option value="RESTRICTED">Restricted</option>
              </select>
              {errors.dayType && <p className="text-xs text-red-600 mt-0.5">{errors.dayType.message}</p>}
            </div>
            <div className="flex items-end">
              <label className="flex items-center gap-2 text-sm text-gray-700">
                <input type="checkbox" {...register('isRecurring')} className="rounded border-gray-300" />
                Recurring annually
              </label>
            </div>
          </div>
          <div className="flex gap-2">
            <button type="submit" disabled={createMutation.isPending} className="px-3 py-1.5 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 disabled:opacity-50">
              {createMutation.isPending ? 'Adding...' : 'Add'}
            </button>
            <button type="button" onClick={() => { setShowForm(false); reset(); }} className="px-3 py-1.5 bg-gray-200 text-gray-700 text-sm rounded hover:bg-gray-300">
              Cancel
            </button>
          </div>
        </form>
      )}

      {holidays.length > 0 ? (
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Name</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Date</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Type</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Recurring</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {holidays.map((h) => (
                <tr key={h.id} className="hover:bg-gray-50">
                  <td className="px-4 py-2 text-gray-900">{h.name}</td>
                  <td className="px-4 py-2 text-gray-600">{h.date}</td>
                  <td className="px-4 py-2 text-gray-600">{h.dayType}</td>
                  <td className="px-4 py-2 text-gray-600">{h.isRecurring ? 'Yes' : 'No'}</td>
                  <td className="px-4 py-2">
                    <button
                      onClick={() => deleteMutation.mutate(h.id)}
                      className="p-1 text-gray-400 hover:text-red-600 rounded"
                      aria-label={`Delete ${h.name}`}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="text-sm text-gray-500 py-4 text-center">No holidays added yet.</p>
      )}
    </div>
  );
}

function ExamWindowsTab({ examWindows, showForm, setShowForm, createMutation, deleteMutation }) {
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: zodResolver(examWindowSchema),
    defaultValues: { name: '', examType: '', startDate: '', endDate: '' },
  });

  const onSubmit = (data) => {
    createMutation.mutate(data, {
      onSuccess: () => { reset(); setShowForm(false); },
    });
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-medium text-gray-900">Exam Windows ({examWindows.length})</h2>
        <button
          onClick={() => setShowForm(!showForm)}
          className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700"
        >
          <Plus className="w-3.5 h-3.5" />
          Add Exam Window
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit(onSubmit)} className="bg-gray-50 border border-gray-200 rounded-lg p-4 space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="ew-name" className="block text-xs font-medium text-gray-600 mb-1">Name</label>
              <input id="ew-name" {...register('name')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm" />
              {errors.name && <p className="text-xs text-red-600 mt-0.5">{errors.name.message}</p>}
            </div>
            <div>
              <label htmlFor="ew-type" className="block text-xs font-medium text-gray-600 mb-1">Exam Type</label>
              <select id="ew-type" {...register('examType')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm">
                <option value="">Select type</option>
                <option value="MIDTERM">Midterm</option>
                <option value="ENDTERM">Endterm</option>
                <option value="SUPPLEMENTARY">Supplementary</option>
                <option value="PRACTICAL">Practical</option>
              </select>
              {errors.examType && <p className="text-xs text-red-600 mt-0.5">{errors.examType.message}</p>}
            </div>
            <div>
              <label htmlFor="ew-start" className="block text-xs font-medium text-gray-600 mb-1">Start Date</label>
              <input id="ew-start" type="date" {...register('startDate')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm" />
              {errors.startDate && <p className="text-xs text-red-600 mt-0.5">{errors.startDate.message}</p>}
            </div>
            <div>
              <label htmlFor="ew-end" className="block text-xs font-medium text-gray-600 mb-1">End Date</label>
              <input id="ew-end" type="date" {...register('endDate')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm" />
              {errors.endDate && <p className="text-xs text-red-600 mt-0.5">{errors.endDate.message}</p>}
            </div>
          </div>
          <div className="flex gap-2">
            <button type="submit" disabled={createMutation.isPending} className="px-3 py-1.5 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 disabled:opacity-50">
              {createMutation.isPending ? 'Adding...' : 'Add'}
            </button>
            <button type="button" onClick={() => { setShowForm(false); reset(); }} className="px-3 py-1.5 bg-gray-200 text-gray-700 text-sm rounded hover:bg-gray-300">
              Cancel
            </button>
          </div>
        </form>
      )}

      {examWindows.length > 0 ? (
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Name</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Type</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Start</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">End</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {examWindows.map((ew) => (
                <tr key={ew.id} className="hover:bg-gray-50">
                  <td className="px-4 py-2 text-gray-900">{ew.name}</td>
                  <td className="px-4 py-2 text-gray-600">{ew.examType}</td>
                  <td className="px-4 py-2 text-gray-600">{ew.startDate}</td>
                  <td className="px-4 py-2 text-gray-600">{ew.endDate}</td>
                  <td className="px-4 py-2">
                    <button
                      onClick={() => deleteMutation.mutate(ew.id)}
                      className="p-1 text-gray-400 hover:text-red-600 rounded"
                      aria-label={`Delete ${ew.name}`}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="text-sm text-gray-500 py-4 text-center">No exam windows added yet.</p>
      )}
    </div>
  );
}

function SpecialPeriodsTab({ specialPeriods, showForm, setShowForm, createMutation, deleteMutation }) {
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: zodResolver(specialPeriodSchema),
    defaultValues: { name: '', periodType: '', startDate: '', endDate: '' },
  });

  const onSubmit = (data) => {
    createMutation.mutate(data, {
      onSuccess: () => { reset(); setShowForm(false); },
    });
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-medium text-gray-900">Special Periods ({specialPeriods.length})</h2>
        <button
          onClick={() => setShowForm(!showForm)}
          className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700"
        >
          <Plus className="w-3.5 h-3.5" />
          Add Special Period
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleSubmit(onSubmit)} className="bg-gray-50 border border-gray-200 rounded-lg p-4 space-y-3">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="sp-name" className="block text-xs font-medium text-gray-600 mb-1">Name</label>
              <input id="sp-name" {...register('name')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm" />
              {errors.name && <p className="text-xs text-red-600 mt-0.5">{errors.name.message}</p>}
            </div>
            <div>
              <label htmlFor="sp-type" className="block text-xs font-medium text-gray-600 mb-1">Period Type</label>
              <select id="sp-type" {...register('periodType')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm">
                <option value="">Select type</option>
                <option value="ORIENTATION">Orientation</option>
                <option value="REGISTRATION">Registration</option>
                <option value="BREAK">Break</option>
                <option value="WORKSHOP">Workshop</option>
                <option value="OTHER">Other</option>
              </select>
              {errors.periodType && <p className="text-xs text-red-600 mt-0.5">{errors.periodType.message}</p>}
            </div>
            <div>
              <label htmlFor="sp-start" className="block text-xs font-medium text-gray-600 mb-1">Start Date</label>
              <input id="sp-start" type="date" {...register('startDate')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm" />
              {errors.startDate && <p className="text-xs text-red-600 mt-0.5">{errors.startDate.message}</p>}
            </div>
            <div>
              <label htmlFor="sp-end" className="block text-xs font-medium text-gray-600 mb-1">End Date</label>
              <input id="sp-end" type="date" {...register('endDate')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm" />
              {errors.endDate && <p className="text-xs text-red-600 mt-0.5">{errors.endDate.message}</p>}
            </div>
          </div>
          <div className="flex gap-2">
            <button type="submit" disabled={createMutation.isPending} className="px-3 py-1.5 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 disabled:opacity-50">
              {createMutation.isPending ? 'Adding...' : 'Add'}
            </button>
            <button type="button" onClick={() => { setShowForm(false); reset(); }} className="px-3 py-1.5 bg-gray-200 text-gray-700 text-sm rounded hover:bg-gray-300">
              Cancel
            </button>
          </div>
        </form>
      )}

      {specialPeriods.length > 0 ? (
        <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b border-gray-200">
              <tr>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Name</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Type</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Start</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">End</th>
                <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {specialPeriods.map((sp) => (
                <tr key={sp.id} className="hover:bg-gray-50">
                  <td className="px-4 py-2 text-gray-900">{sp.name}</td>
                  <td className="px-4 py-2 text-gray-600">{sp.periodType}</td>
                  <td className="px-4 py-2 text-gray-600">{sp.startDate}</td>
                  <td className="px-4 py-2 text-gray-600">{sp.endDate}</td>
                  <td className="px-4 py-2">
                    <button
                      onClick={() => deleteMutation.mutate(sp.id)}
                      className="p-1 text-gray-400 hover:text-red-600 rounded"
                      aria-label={`Delete ${sp.name}`}
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="text-sm text-gray-500 py-4 text-center">No special periods added yet.</p>
      )}
    </div>
  );
}
