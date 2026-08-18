import { z } from 'zod';

export const calendarSchema = z
  .object({
    name: z
      .string()
      .min(1, 'Calendar name is required')
      .max(200, 'Calendar name must be 200 characters or fewer')
      .trim(),
    campusId: z
      .coerce.number({ required_error: 'Campus is required' })
      .min(1, 'Please select a campus'),
    academicYear: z
      .string()
      .min(1, 'Academic year is required')
      .regex(
        /^\d{4}-\d{4}$/,
        'Academic year must be in YYYY-YYYY format (e.g., 2025-2026)'
      ),
    semesterType: z.enum(['ODD', 'EVEN', 'SUMMER'], {
      required_error: 'Semester type is required',
    }),
    startDate: z.string().min(1, 'Start date is required'),
    endDate: z.string().min(1, 'End date is required'),
  })
  .refine((data) => new Date(data.startDate) < new Date(data.endDate), {
    message: 'Start date must be before end date',
    path: ['endDate'],
  });

export const holidaySchema = z.object({
  name: z
    .string()
    .min(1, 'Holiday name is required')
    .max(200, 'Holiday name must be 200 characters or fewer')
    .trim(),
  date: z.string().min(1, 'Date is required'),
  dayType: z.enum(['NATIONAL', 'STATE', 'INSTITUTIONAL', 'RESTRICTED'], {
    required_error: 'Day type is required',
  }),
  isRecurring: z.boolean().default(false),
});

export const examWindowSchema = z
  .object({
    name: z
      .string()
      .min(1, 'Exam window name is required')
      .max(200, 'Name must be 200 characters or fewer')
      .trim(),
    examType: z.enum(['MIDTERM', 'ENDTERM', 'SUPPLEMENTARY', 'PRACTICAL'], {
      required_error: 'Exam type is required',
    }),
    startDate: z.string().min(1, 'Start date is required'),
    endDate: z.string().min(1, 'End date is required'),
  })
  .refine((data) => new Date(data.startDate) < new Date(data.endDate), {
    message: 'Start date must be before end date',
    path: ['endDate'],
  });

export const specialPeriodSchema = z
  .object({
    name: z
      .string()
      .min(1, 'Period name is required')
      .max(200, 'Name must be 200 characters or fewer')
      .trim(),
    periodType: z.enum(['ORIENTATION', 'REGISTRATION', 'BREAK', 'WORKSHOP', 'OTHER'], {
      required_error: 'Period type is required',
    }),
    startDate: z.string().min(1, 'Start date is required'),
    endDate: z.string().min(1, 'End date is required'),
  })
  .refine((data) => new Date(data.startDate) < new Date(data.endDate), {
    message: 'Start date must be before end date',
    path: ['endDate'],
  });
