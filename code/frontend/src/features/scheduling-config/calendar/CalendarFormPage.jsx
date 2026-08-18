import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { calendarSchema } from './schemas';
import { useCalendar, useCreateCalendar, useUpdateCalendar } from './hooks';
import { useCampuses } from '@/features/master-data/campus/hooks';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';

export default function CalendarFormPage() {
  const navigate = useNavigate();
  const { calendarId } = useParams();
  const isEdit = Boolean(calendarId);
  const numericId = calendarId ? Number(calendarId) : undefined;

  const { data: calendar, isLoading } = useCalendar(numericId);
  const { data: campusData } = useCampuses({ size: 100 });
  const createMutation = useCreateCalendar();
  const updateMutation = useUpdateCalendar(numericId ?? 0);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(calendarSchema),
    defaultValues: {
      name: '',
      campusId: '',
      academicYear: '',
      semesterType: '',
      startDate: '',
      endDate: '',
    },
  });

  useEffect(() => {
    if (calendar) {
      reset({
        name: calendar.name,
        campusId: calendar.campusId,
        academicYear: calendar.academicYear,
        semesterType: calendar.semesterType,
        startDate: calendar.startDate,
        endDate: calendar.endDate,
      });
    }
  }, [calendar, reset]);

  const onSubmit = (data) => {
    const mutation = isEdit ? updateMutation : createMutation;
    mutation.mutate(data, {
      onSuccess: () => navigate('/admin/scheduling-config/calendars'),
      onError: (error) => {
        if (error.response?.status === 409) {
          setError('name', {
            type: 'server',
            message: error.response.data?.message ?? 'A calendar with this configuration already exists.',
          });
        }
        if (error.fieldErrors) {
          Object.entries(error.fieldErrors).forEach(([field, message]) => {
            setError(field, { type: 'server', message });
          });
        }
      },
    });
  };

  if (isEdit && isLoading) {
    return <LoadingSkeleton rows={6} columns={2} />;
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">
        {isEdit ? 'Edit Academic Calendar' : 'Create Academic Calendar'}
      </h1>

      <form onSubmit={handleSubmit(onSubmit)} className="bg-white border border-gray-200 rounded-lg p-6 space-y-4">
        {/* Name */}
        <div>
          <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
            Name <span className="text-red-500">*</span>
          </label>
          <input
            id="name"
            {...register('name')}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          {errors.name && <p className="text-xs text-red-600 mt-1">{errors.name.message}</p>}
        </div>

        {/* Campus */}
        <div>
          <label htmlFor="campusId" className="block text-sm font-medium text-gray-700 mb-1">
            Campus <span className="text-red-500">*</span>
          </label>
          <select
            id="campusId"
            {...register('campusId')}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Select a campus</option>
            {campusData?.data?.map((campus) => (
              <option key={campus.id} value={campus.id}>
                {campus.name}
              </option>
            ))}
          </select>
          {errors.campusId && <p className="text-xs text-red-600 mt-1">{errors.campusId.message}</p>}
        </div>

        {/* Academic Year */}
        <div>
          <label htmlFor="academicYear" className="block text-sm font-medium text-gray-700 mb-1">
            Academic Year <span className="text-red-500">*</span>
          </label>
          <input
            id="academicYear"
            {...register('academicYear')}
            placeholder="e.g., 2025-2026"
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          {errors.academicYear && <p className="text-xs text-red-600 mt-1">{errors.academicYear.message}</p>}
        </div>

        {/* Semester Type */}
        <div>
          <label htmlFor="semesterType" className="block text-sm font-medium text-gray-700 mb-1">
            Semester Type <span className="text-red-500">*</span>
          </label>
          <select
            id="semesterType"
            {...register('semesterType')}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Select semester type</option>
            <option value="ODD">ODD</option>
            <option value="EVEN">EVEN</option>
            <option value="SUMMER">SUMMER</option>
          </select>
          {errors.semesterType && <p className="text-xs text-red-600 mt-1">{errors.semesterType.message}</p>}
        </div>

        {/* Start Date & End Date */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="startDate" className="block text-sm font-medium text-gray-700 mb-1">
              Start Date <span className="text-red-500">*</span>
            </label>
            <input
              id="startDate"
              type="date"
              {...register('startDate')}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.startDate && <p className="text-xs text-red-600 mt-1">{errors.startDate.message}</p>}
          </div>
          <div>
            <label htmlFor="endDate" className="block text-sm font-medium text-gray-700 mb-1">
              End Date <span className="text-red-500">*</span>
            </label>
            <input
              id="endDate"
              type="date"
              {...register('endDate')}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.endDate && <p className="text-xs text-red-600 mt-1">{errors.endDate.message}</p>}
          </div>
        </div>

        {/* Actions */}
        <div className="flex justify-between pt-4 border-t border-gray-100">
          <button
            type="button"
            onClick={() => navigate('/admin/scheduling-config/calendars')}
            className="px-4 py-2 text-sm text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting || createMutation.isPending || updateMutation.isPending}
            className="px-4 py-2 text-sm text-white bg-blue-600 rounded-md hover:bg-blue-700 disabled:opacity-50"
          >
            {isSubmitting || createMutation.isPending || updateMutation.isPending
              ? 'Saving...'
              : isEdit
                ? 'Update Calendar'
                : 'Create Calendar'}
          </button>
        </div>
      </form>
    </div>
  );
}
