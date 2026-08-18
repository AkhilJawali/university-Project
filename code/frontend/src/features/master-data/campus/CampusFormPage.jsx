import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { campusSchema } from './schemas';
import { useCampus, useCreateCampus, useUpdateCampus } from './hooks';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';

export default function CampusFormPage() {
  const navigate = useNavigate();
  const { campusId } = useParams();
  const isEdit = Boolean(campusId);
  const numericId = campusId ? Number(campusId) : undefined;

  const { data: campus, isLoading } = useCampus(numericId);
  const createMutation = useCreateCampus();
  const updateMutation = useUpdateCampus(numericId ?? 0);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(campusSchema),
    defaultValues: { name: '', code: '', address: '', city: '', state: '', timezone: '' },
  });

  useEffect(() => {
    if (campus) {
      reset({
        name: campus.name,
        code: campus.code,
        address: campus.address ?? '',
        city: campus.city ?? '',
        state: campus.state ?? '',
        timezone: campus.timezone,
      });
    }
  }, [campus, reset]);

  const onSubmit = (data) => {
    const mutation = isEdit ? updateMutation : createMutation;
    mutation.mutate(data, {
      onSuccess: () => navigate('/admin/master-data/campuses'),
      onError: (error) => {
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
        {isEdit ? 'Edit Campus' : 'Create Campus'}
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

        {/* Code */}
        <div>
          <label htmlFor="code" className="block text-sm font-medium text-gray-700 mb-1">
            Code <span className="text-red-500">*</span>
          </label>
          <input
            id="code"
            {...register('code', {
              onChange: (e) => setValue('code', e.target.value.toUpperCase()),
            })}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          {errors.code && <p className="text-xs text-red-600 mt-1">{errors.code.message}</p>}
        </div>

        {/* Address */}
        <div>
          <label htmlFor="address" className="block text-sm font-medium text-gray-700 mb-1">Address</label>
          <input
            id="address"
            {...register('address')}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* City & State */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="city" className="block text-sm font-medium text-gray-700 mb-1">City</label>
            <input
              id="city"
              {...register('city')}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div>
            <label htmlFor="state" className="block text-sm font-medium text-gray-700 mb-1">State</label>
            <input
              id="state"
              {...register('state')}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        {/* Timezone */}
        <div>
          <label htmlFor="timezone" className="block text-sm font-medium text-gray-700 mb-1">
            Timezone <span className="text-red-500">*</span>
          </label>
          <input
            id="timezone"
            {...register('timezone')}
            placeholder="e.g., Asia/Kolkata"
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          {errors.timezone && <p className="text-xs text-red-600 mt-1">{errors.timezone.message}</p>}
        </div>

        {/* Actions */}
        <div className="flex justify-between pt-4 border-t border-gray-100">
          <button
            type="button"
            onClick={() => navigate('/admin/master-data/campuses')}
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
              : isEdit ? 'Update Campus' : 'Create Campus'}
          </button>
        </div>
      </form>
    </div>
  );
}
