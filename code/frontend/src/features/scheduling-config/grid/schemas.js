import { z } from 'zod';

export const gridSchema = z.object({
  name: z
    .string()
    .min(1, 'Grid name is required')
    .max(200, 'Grid name must be 200 characters or fewer')
    .trim(),
  campusId: z
    .coerce.number({ required_error: 'Campus is required' })
    .min(1, 'Please select a campus'),
  effectiveFrom: z.string().min(1, 'Effective from date is required'),
});

export const slotSchema = z
  .object({
    slotNumber: z
      .number({ required_error: 'Slot number is required' })
      .int()
      .min(1, 'Slot number must be at least 1'),
    startTime: z
      .string()
      .min(1, 'Start time is required')
      .regex(/^([01]\d|2[0-3]):[0-5]\d$/, 'Start time must be in HH:mm format'),
    endTime: z
      .string()
      .min(1, 'End time is required')
      .regex(/^([01]\d|2[0-3]):[0-5]\d$/, 'End time must be in HH:mm format'),
    slotType: z.enum(['LECTURE', 'TUTORIAL', 'PRACTICAL', 'BREAK', 'LUNCH'], {
      required_error: 'Slot type is required',
    }),
  })
  .refine(
    (data) => {
      const [startH, startM] = data.startTime.split(':').map(Number);
      const [endH, endM] = data.endTime.split(':').map(Number);
      return startH * 60 + startM < endH * 60 + endM;
    },
    {
      message: 'End time must be after start time',
      path: ['endTime'],
    }
  );

export const workingDaysSchema = z.object({
  days: z
    .array(
      z.object({
        dayOfWeek: z.number().int().min(0).max(6),
        label: z.string(),
        isWorkingDay: z.boolean(),
      })
    )
    .length(7, 'Must include all 7 days')
    .refine(
      (days) => days.some((d) => d.isWorkingDay),
      { message: 'At least one working day is required' }
    ),
});
