import { z } from 'zod';

export const programSchema = z.object({
  name: z
    .string()
    .min(1, 'Program name is required')
    .max(200, 'Program name must be 200 characters or fewer')
    .trim(),
  code: z
    .string()
    .min(2, 'Code must be at least 2 characters')
    .max(20, 'Code must be 20 characters or fewer')
    .regex(
      /^[A-Z0-9-]+$/,
      'Code must contain only uppercase letters, numbers, and hyphens'
    )
    .transform((val) => val.toUpperCase()),
  departmentId: z.coerce.number().min(1, 'Department is required'),
  durationYears: z.coerce
    .number()
    .min(1, 'Duration must be at least 1 year')
    .max(8, 'Duration must be 8 years or fewer'),
  totalSemesters: z.coerce
    .number()
    .min(1, 'Must have at least 1 semester')
    .max(16, 'Must be 16 semesters or fewer'),
  degreeType: z.enum(['UG', 'PG', 'PHD', 'DIPLOMA'], {
    errorMap: () => ({ message: 'Please select a degree type' }),
  }),
});
