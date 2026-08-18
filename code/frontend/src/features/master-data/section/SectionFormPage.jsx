import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { sectionSchema } from './schemas';
import { useSection, useCreateSection, useUpdateSection } from './hooks';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';

export default function SectionFormPage() {
  const navigate = useNavigate();
  const { batchId, sectionId } = useParams();
  const isEdit = Boolean(sectionId);
  const numericId = sectionId ? Number(sectionId) : undefined;
  const numericBatchId = batchId ? Number(batchId) : 0;

  const { data: section, isLoading } = useSection(numericId);
  const createMutation = useCreateSection();
  const updateMutation = useUpdateSection(numericId ?? 0);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(sectionSchema),
    defaultValues: {
      name: '',
      batchId: numericBatchId,
      strength: 30,
    },
  });

  useEffect(() => {
    if (section) {
      reset({
        name: section.name,
        batchId: section.batchId,
        strength: section.strength,
      });
    }
  }, [section, reset]);

  const onSubmit = (data) => {
    const mutation = isEdit ? updateMutation : createMutation;
    mutation.mutate(data, {
      onSuccess: () => navigate(-1),
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
    return <LoadingSkeleton rows={3} columns={2} />;
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">
        {isEdit ? 'Edit Section' : 'Create Section'}
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

        {/* Batch ID (hidden - from route) */}
        <input type="hidden" {...register('batchId')} />

        {/* Strength */}
        <div>
          <label htmlFor="strength" className="block text-sm font-medium text-gray-700 mb-1">
            Strength <span className="text-red-500">*</span>
          </label>
          <input
            id="strength"
            type="number"
            min={1}
            max={10000}
            {...register('strength')}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          {errors.strength && <p className="text-xs text-red-600 mt-1">{errors.strength.message}</p>}
        </div>

        {/* Actions */}
        <div className="flex justify-between pt-4 border-t border-gray-100">
          <button
            type="button"
            onClick={() => navigate(-1)}
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
              : isEdit ? 'Update Section' : 'Create Section'}
          </button>
        </div>
      </form>
    </div>
  );
}
