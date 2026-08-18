import { describe, it, expect } from 'vitest';
import { campusSchema } from '../schemas';

describe('campusSchema', () => {
  it('should pass with valid data', () => {
    const validData = {
      name: 'Main Campus',
      code: 'MC-01',
      address: '123 University St',
      city: 'Bengaluru',
      state: 'Karnataka',
      timezone: 'Asia/Kolkata',
    };

    const result = campusSchema.safeParse(validData);
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.name).toBe('Main Campus');
      expect(result.data.code).toBe('MC-01');
    }
  });

  it('should fail when name is missing', () => {
    const invalidData = {
      name: '',
      code: 'MC-01',
      timezone: 'Asia/Kolkata',
    };

    const result = campusSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      const nameError = result.error.issues.find((i) => i.path[0] === 'name');
      expect(nameError).toBeDefined();
      expect(nameError?.message).toBe('Campus name is required');
    }
  });

  it('should fail with invalid code format (lowercase)', () => {
    const invalidData = {
      name: 'Test Campus',
      code: 'invalid',
      timezone: 'Asia/Kolkata',
    };

    const result = campusSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      const codeError = result.error.issues.find((i) => i.path[0] === 'code');
      expect(codeError).toBeDefined();
      expect(codeError?.message).toContain('uppercase');
    }
  });

  it('should fail when code is too short', () => {
    const invalidData = {
      name: 'Test Campus',
      code: 'A',
      timezone: 'Asia/Kolkata',
    };

    const result = campusSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      const codeError = result.error.issues.find((i) => i.path[0] === 'code');
      expect(codeError).toBeDefined();
      expect(codeError?.message).toContain('at least 2 characters');
    }
  });

  it('should transform code to uppercase', () => {
    const data = {
      name: 'Test Campus',
      code: 'AB-01',
      timezone: 'Asia/Kolkata',
    };

    const result = campusSchema.safeParse(data);
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.code).toBe('AB-01');
    }
  });

  it('should fail when timezone is empty', () => {
    const invalidData = {
      name: 'Test Campus',
      code: 'MC-01',
      timezone: '',
    };

    const result = campusSchema.safeParse(invalidData);
    expect(result.success).toBe(false);
    if (!result.success) {
      const tzError = result.error.issues.find((i) => i.path[0] === 'timezone');
      expect(tzError).toBeDefined();
    }
  });

  it('should set optional fields to defaults', () => {
    const minimalData = {
      name: 'Test Campus',
      code: 'TC-01',
      timezone: 'Asia/Kolkata',
    };

    const result = campusSchema.safeParse(minimalData);
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.address).toBe('');
      expect(result.data.city).toBe('');
      expect(result.data.state).toBe('');
    }
  });
});
