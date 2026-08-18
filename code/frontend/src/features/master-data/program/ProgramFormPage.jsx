import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { programSchema } from './schemas';
import { useProgram, useCreateProgram, useUpdateProgram } from './hooks';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';

export default function ProgramFormPage() {
  const navigate = useNavigate();
  const { departmentId, programId } = useParams();
  const isEdit = Boolean(programId);
  const numericId = programId ? Number(programId) : undefined;
  const numericDepartmentId = departmentId ? Number(departmentId) : 0;

  const { data: program, isLoading } = useProgram(numericId);
  const createMutation = useCreateProgram();
  const updateMutation = useUpdateProgram(numericId ?? 0);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(programSchema),
    defaultValues: {
      name: '',
      code: '',
      departmentId: numericDepartmentId,
      durationYears: 4,
      totalSemesters: 8,
      degreeType: 'UG',
    },
  });

  useEffect(() => {
    if (program) {
      reset({
        name: program.name,
        code: program.code,
        departmentId: program.departmentId,
        durationYears: program.durationYears,
        totalSemesters: program.totalSemesters,
        degreeType: program.degreeType,
      });
    }
  }, [program, reset]);

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
    return <LoadingSkeleton rows={6} columns={2} />;
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">
        {isEdit ? 'Edit Program' : 'Create Program'}
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

        {/* Department ID (hidden - from route) */}
        <input type="hidden" {...register('departmentId')} />

        {/* Duration Years & Total Semesters */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="durationYears" className="block text-sm font-medium text-gray-700 mb-1">
              Duration Years <span className="text-red-500">*</span>
            </label>
            <input
              id="durationYears"
              type="number"
              min={1}
              max={8}
              {...register('durationYears')}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.durationYears && <p className="text-xs text-red-600 mt-1">{errors.durationYears.message}</p>}
          </div>
          <div>
            <label htmlFor="totalSemesters" className="block text-sm font-medium text-gray-700 mb-1">
              Total Semesters <span className="text-red-500">*</span>
            </label>
            <input
              id="totalSemesters"
              type="number"
              min={1}
              max={16}
              {...register('totalSemesters')}
              className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {errors.totalSemesters && <p className="text-xs text-red-600 mt-1">{errors.totalSemesters.message}</p>}
          </div>
        </div>

        {/* Degree Type */}
        <div>
          <label htmlFor="degreeType" className="block text-sm font-medium text-gray-700 mb-1">
            Degree Type <span className="text-red-500">*</span>
          </label>
          <select
            id="degreeType"
            {...register('degreeType')}
            className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="UG">Undergraduate (UG)</option>
            <option value="PG">Postgraduate (PG)</option>
            <option value="PHD">Doctorate (PhD)</option>
            <option value="DIPLOMA">Diploma</option>
          </select>
          {errors.degreeType && <p className="text-xs text-red-600 mt-1">{errors.degreeType.message}</p>}
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
              : isEdit ? 'Update Program' : 'Create Program'}
          </button>
        </div>
      </form>
    </div>
  );
}
