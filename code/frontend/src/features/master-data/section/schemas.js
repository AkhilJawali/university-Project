import { z } from 'zod';

export const sectionSchema = z.object({
  name: z
    .string()
    .min(1, 'Section name is required')
    .max(200, 'Section name must be 200 characters or fewer')
    .trim(),
  batchId: z.coerce.number().min(1, 'Batch is required'),
  strength: z.coerce
    .number()
    .min(1, 'Strength must be at least 1')
    .max(10000, 'Strength must be 10,000 or fewer'),
});
