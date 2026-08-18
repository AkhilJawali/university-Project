import { describe, it, expect } from 'vitest';
import { calendarSchema, holidaySchema, examWindowSchema, specialPeriodSchema } from '../schemas';

describe('calendarSchema', () => {
  const validData = {
    name: 'Odd Semester 2025-2026',
    campusId: 1,
    academicYear: '2025-2026',
    semesterType: 'ODD',
    startDate: '2025-07-01',
    endDate: '2025-12-15',
  };

  it('should pass with valid data', () => {
    const result = calendarSchema.safeParse(validData);
    expect(result.success).toBe(true);
  });

  it('should fail when name is missing', () => {
    const result = calendarSchema.safeParse({ ...validData, name: '' });
    expect(result.success).toBe(false);
    expect(result.error.issues[0].path).toContain('name');
  });

  it('should fail when campusId is missing', () => {
    const result = calendarSchema.safeParse({ ...validData, campusId: undefined });
    expect(result.success).toBe(false);
  });

  it('should fail when academicYear has wrong format', () => {
    const result = calendarSchema.safeParse({ ...validData, academicYear: '2025' });
    expect(result.success).toBe(false);
    expect(result.error.issues[0].path).toContain('academicYear');
  });

  it('should fail when academicYear uses slash instead of dash', () => {
    const result = calendarSchema.safeParse({ ...validData, academicYear: '2025/2026' });
    expect(result.success).toBe(false);
  });

  it('should fail when startDate is after endDate', () => {
    const result = calendarSchema.safeParse({
      ...validData,
      startDate: '2025-12-15',
      endDate: '2025-07-01',
    });
    expect(result.success).toBe(false);
    expect(result.error.issues[0].path).toContain('endDate');
  });

  it('should fail when semesterType is invalid', () => {
    const result = calendarSchema.safeParse({ ...validData, semesterType: 'WINTER' });
    expect(result.success).toBe(false);
  });

  it('should accept all valid semester types', () => {
    for (const type of ['ODD', 'EVEN', 'SUMMER']) {
      const result = calendarSchema.safeParse({ ...validData, semesterType: type });
      expect(result.success).toBe(true);
    }
  });
});

describe('holidaySchema', () => {
  const validHoliday = {
    name: 'Independence Day',
    date: '2025-08-15',
    dayType: 'NATIONAL',
    isRecurring: true,
  };

  it('should pass with valid data', () => {
    const result = holidaySchema.safeParse(validHoliday);
    expect(result.success).toBe(true);
  });

  it('should fail when name is empty', () => {
    const result = holidaySchema.safeParse({ ...validHoliday, name: '' });
    expect(result.success).toBe(false);
  });

  it('should fail with invalid dayType', () => {
    const result = holidaySchema.safeParse({ ...validHoliday, dayType: 'INVALID' });
    expect(result.success).toBe(false);
  });

  it('should default isRecurring to false', () => {
    const { isRecurring, ...rest } = validHoliday;
    const result = holidaySchema.safeParse(rest);
    expect(result.success).toBe(true);
    expect(result.data.isRecurring).toBe(false);
  });
});

describe('examWindowSchema', () => {
  const validExamWindow = {
    name: 'Midterm Exams',
    examType: 'MIDTERM',
    startDate: '2025-09-15',
    endDate: '2025-09-25',
  };

  it('should pass with valid data', () => {
    const result = examWindowSchema.safeParse(validExamWindow);
    expect(result.success).toBe(true);
  });

  it('should fail when startDate is after endDate', () => {
    const result = examWindowSchema.safeParse({
      ...validExamWindow,
      startDate: '2025-09-25',
      endDate: '2025-09-15',
    });
    expect(result.success).toBe(false);
  });

  it('should fail with invalid examType', () => {
    const result = examWindowSchema.safeParse({ ...validExamWindow, examType: 'FINAL' });
    expect(result.success).toBe(false);
  });
});

describe('specialPeriodSchema', () => {
  const validPeriod = {
    name: 'Orientation Week',
    periodType: 'ORIENTATION',
    startDate: '2025-07-01',
    endDate: '2025-07-07',
  };

  it('should pass with valid data', () => {
    const result = specialPeriodSchema.safeParse(validPeriod);
    expect(result.success).toBe(true);
  });

  it('should fail when startDate is after endDate', () => {
    const result = specialPeriodSchema.safeParse({
      ...validPeriod,
      startDate: '2025-07-07',
      endDate: '2025-07-01',
    });
    expect(result.success).toBe(false);
  });

  it('should fail with invalid periodType', () => {
    const result = specialPeriodSchema.safeParse({ ...validPeriod, periodType: 'UNKNOWN' });
    expect(result.success).toBe(false);
  });
});
