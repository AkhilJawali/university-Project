import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { gridSchema } from './schemas';
import { useGrid, useCreateGrid, useUpdateGrid } from './hooks';
import { useCampuses } from '@/features/master-data/campus/hooks';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';

export default function GridFormPage() {
  const navigate = useNavigate();
  const { gridId } = useParams();
  const isEdit = Boolean(gridId);
  const numericId = gridId ? Number(gridId) : undefined;

  const { data: grid, isLoading } = useGrid(numericId);
  const { data: campusData } = useCampuses({ size: 100 });
  const createMutation = useCreateGrid();
  const updateMutation = useUpdateGrid(numericId ?? 0);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(gridSchema),
    defaultValues: { name: '', campusId: '', effectiveFrom: '' },
  });

  useEffect(() => {
    if (grid) {
      reset({
        name: grid.name,
        campusId: grid.campusId,
        effectiveFrom: grid.effectiveFrom,
      });
    }
  }, [grid, reset]);

  const onSubmit = (data) => {
    const payload = { ...data, campusId: Number(data.campusId) };
    const mutation = isEdit ? updateMutation : createMutation;
    mutation.mutate(payload, {
      onSuccess: () => navigate('/admin/scheduling-config/grids'),
      onError: (error) => {
        if (error.response?.status === 409) {
          setError('name', {
            type: 'server',
            message: error.response.data?.message ?? 'A grid with this configuration already exists.',
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
    return <LoadingSkeleton rows={4} columns={2} />;
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">
        {isEdit ? 'Edit Time-Slot Grid' : 'Create Time-Slot Grid'}
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
            {...register('campusId', { valueAsNumber: true })}
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

        {/* Effective From */}
        <div>
          <label htmlFor="effectiveFrom" className="block text-sm font-medium text-gray-700 mb-1">
            Effective From <span className="text-red-500">*</span>
          </label>
          <input
            id="effectiveFrom"
            type="date"
            {...register('effectiveFrom')}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          {errors.effectiveFrom && <p className="text-xs text-red-600 mt-1">{errors.effectiveFrom.message}</p>}
        </div>

        {/* Actions */}
        <div className="flex justify-between pt-4 border-t border-gray-100">
          <button
            type="button"
            onClick={() => navigate('/admin/scheduling-config/grids')}
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
                ? 'Update Grid'
                : 'Create Grid'}
          </button>
        </div>
      </form>
    </div>
  );
}
