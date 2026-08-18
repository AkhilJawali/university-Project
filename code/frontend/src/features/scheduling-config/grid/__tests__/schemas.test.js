import { describe, it, expect } from 'vitest';
import { gridSchema, slotSchema, workingDaysSchema } from '../schemas';

describe('gridSchema', () => {
  const validGrid = {
    name: 'Main Campus Grid 2025',
    campusId: 1,
    effectiveFrom: '2025-07-01',
  };

  it('should pass with valid data', () => {
    const result = gridSchema.safeParse(validGrid);
    expect(result.success).toBe(true);
  });

  it('should fail when name is empty', () => {
    const result = gridSchema.safeParse({ ...validGrid, name: '' });
    expect(result.success).toBe(false);
  });

  it('should fail when campusId is missing', () => {
    const result = gridSchema.safeParse({ ...validGrid, campusId: undefined });
    expect(result.success).toBe(false);
  });

  it('should fail when effectiveFrom is empty', () => {
    const result = gridSchema.safeParse({ ...validGrid, effectiveFrom: '' });
    expect(result.success).toBe(false);
  });
});

describe('slotSchema', () => {
  const validSlot = {
    slotNumber: 1,
    startTime: '08:00',
    endTime: '09:00',
    slotType: 'LECTURE',
  };

  it('should pass with valid data', () => {
    const result = slotSchema.safeParse(validSlot);
    expect(result.success).toBe(true);
  });

  it('should fail when startTime is after endTime', () => {
    const result = slotSchema.safeParse({
      ...validSlot,
      startTime: '10:00',
      endTime: '09:00',
    });
    expect(result.success).toBe(false);
    expect(result.error.issues[0].path).toContain('endTime');
  });

  it('should fail when times are equal', () => {
    const result = slotSchema.safeParse({
      ...validSlot,
      startTime: '09:00',
      endTime: '09:00',
    });
    expect(result.success).toBe(false);
  });

  it('should fail with invalid time format (missing leading zero)', () => {
    const result = slotSchema.safeParse({
      ...validSlot,
      startTime: '8:00',
    });
    expect(result.success).toBe(false);
  });

  it('should fail with invalid time format (bad minutes)', () => {
    const result = slotSchema.safeParse({
      ...validSlot,
      startTime: '08:60',
    });
    expect(result.success).toBe(false);
  });

  it('should fail when slotNumber is 0', () => {
    const result = slotSchema.safeParse({ ...validSlot, slotNumber: 0 });
    expect(result.success).toBe(false);
  });

  it('should fail when slotNumber is negative', () => {
    const result = slotSchema.safeParse({ ...validSlot, slotNumber: -1 });
    expect(result.success).toBe(false);
  });

  it('should fail with invalid slotType', () => {
    const result = slotSchema.safeParse({ ...validSlot, slotType: 'INVALID' });
    expect(result.success).toBe(false);
  });

  it('should accept all valid slot types', () => {
    for (const type of ['LECTURE', 'TUTORIAL', 'PRACTICAL', 'BREAK', 'LUNCH']) {
      const result = slotSchema.safeParse({ ...validSlot, slotType: type });
      expect(result.success).toBe(true);
    }
  });

  it('should accept 90-minute slots', () => {
    const result = slotSchema.safeParse({
      ...validSlot,
      startTime: '14:00',
      endTime: '15:30',
      slotType: 'PRACTICAL',
    });
    expect(result.success).toBe(true);
  });
});

describe('workingDaysSchema', () => {
  const validDays = {
    days: [
      { dayOfWeek: 0, label: 'Mon', isWorkingDay: true },
      { dayOfWeek: 1, label: 'Tue', isWorkingDay: true },
      { dayOfWeek: 2, label: 'Wed', isWorkingDay: true },
      { dayOfWeek: 3, label: 'Thu', isWorkingDay: true },
      { dayOfWeek: 4, label: 'Fri', isWorkingDay: true },
      { dayOfWeek: 5, label: 'Sat', isWorkingDay: false },
      { dayOfWeek: 6, label: 'Sun', isWorkingDay: false },
    ],
  };

  it('should pass with valid working days', () => {
    const result = workingDaysSchema.safeParse(validDays);
    expect(result.success).toBe(true);
  });

  it('should fail when no working day is set', () => {
    const allOff = {
      days: validDays.days.map((d) => ({ ...d, isWorkingDay: false })),
    };
    const result = workingDaysSchema.safeParse(allOff);
    expect(result.success).toBe(false);
  });

  it('should fail when fewer than 7 days are provided', () => {
    const result = workingDaysSchema.safeParse({ days: validDays.days.slice(0, 5) });
    expect(result.success).toBe(false);
  });

  it('should pass with only one working day', () => {
    const onlyMonday = {
      days: validDays.days.map((d) => ({ ...d, isWorkingDay: d.dayOfWeek === 0 })),
    };
    const result = workingDaysSchema.safeParse(onlyMonday);
    expect(result.success).toBe(true);
  });
});
