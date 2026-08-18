import { z } from 'zod';

export const batchSchema = z.object({
  name: z
    .string()
    .min(1, 'Batch name is required')
    .max(200, 'Batch name must be 200 characters or fewer')
    .trim(),
  programId: z.coerce.number().min(1, 'Program is required'),
  academicYear: z
    .string()
    .min(1, 'Academic year is required')
    .max(20, 'Academic year must be 20 characters or fewer')
    .trim(),
  semesterNumber: z.coerce
    .number()
    .min(1, 'Semester must be at least 1')
    .max(16, 'Semester must be 16 or fewer'),
  strength: z.coerce
    .number()
    .min(1, 'Strength must be at least 1')
    .max(10000, 'Strength must be 10,000 or fewer'),
});
