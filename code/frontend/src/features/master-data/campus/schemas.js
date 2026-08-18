import { z } from 'zod';

export const campusSchema = z.object({
  name: z
    .string()
    .min(1, 'Campus name is required')
    .max(200, 'Campus name must be 200 characters or fewer')
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
  address: z.string().max(500).optional().default(''),
  city: z.string().max(100).optional().default(''),
  state: z.string().max(100).optional().default(''),
  timezone: z.string().min(1, 'Timezone is required'),
});
