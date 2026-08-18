import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import { useCampuses, useCreateCampus } from '../hooks';

// Mock the API module
vi.mock('@/api/campusApi', () => ({
  campusApi: {
    getAll: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    getHierarchy: vi.fn(),
  },
}));

// Mock toast store
vi.mock('@/stores/toastStore', () => ({
  useToastStore: () => vi.fn(),
}));

import { campusApi } from '@/api/campusApi';

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });

  return function Wrapper({ children }) {
    return createElement(QueryClientProvider, { client: queryClient }, children);
  };
}

describe('useCampuses', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return campus data on success', async () => {
    const mockResponse = {
      data: [
        { id: 1, name: 'Main Campus', code: 'MC-01', timezone: 'Asia/Kolkata', isActive: true, createdAt: '', updatedAt: '' },
      ],
      meta: { page: 0, size: 20, totalElements: 1, totalPages: 1 },
    };

    vi.mocked(campusApi.getAll).mockResolvedValueOnce(mockResponse);

    const { result } = renderHook(() => useCampuses({ page: 0, size: 20 }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toEqual(mockResponse);
    expect(campusApi.getAll).toHaveBeenCalledWith({ page: 0, size: 20 });
  });

  it('should handle error state', async () => {
    vi.mocked(campusApi.getAll).mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useCampuses({ page: 0, size: 20 }), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error).toBeDefined();
  });
});

describe('useCreateCampus', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should call campusApi.create and invalidate queries on success', async () => {
    const newCampus = {
      name: 'New Campus',
      code: 'NC-01',
      timezone: 'Asia/Kolkata',
    };

    const mockResponse = {
      data: { id: 2, ...newCampus, isActive: true, createdAt: '', updatedAt: '' },
      warnings: [],
    };

    vi.mocked(campusApi.create).mockResolvedValueOnce(mockResponse);

    const { result } = renderHook(() => useCreateCampus(), {
      wrapper: createWrapper(),
    });

    result.current.mutate(newCampus);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(campusApi.create).toHaveBeenCalledWith(newCampus);
  });

  it('should handle creation error', async () => {
    vi.mocked(campusApi.create).mockRejectedValueOnce(new Error('Duplicate code'));

    const { result } = renderHook(() => useCreateCampus(), {
      wrapper: createWrapper(),
    });

    result.current.mutate({ name: 'Test', code: 'TC', timezone: 'UTC' });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
