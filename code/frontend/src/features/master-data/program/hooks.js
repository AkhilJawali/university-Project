import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { programApi } from '@/api/programApi';
import { queryKeys } from '@/api/queryKeys';
import { useToastStore } from '@/stores/toastStore';

export function usePrograms(params) {
  return useQuery({
    queryKey: queryKeys.programs.list(params),
    queryFn: () => programApi.getAll(params),
    staleTime: 5 * 60 * 1000,
    placeholderData: (prev) => prev,
  });
}

export function useProgram(id) {
  return useQuery({
    queryKey: queryKeys.programs.detail(id),
    queryFn: () => programApi.getById(id),
    enabled: Boolean(id),
    staleTime: 60_000,
  });
}

export function useCreateProgram() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => programApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.programs.all });
      addToast({ type: 'success', message: 'Program created successfully' });
    },
  });
}

export function useUpdateProgram(id) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => programApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.programs.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.programs.detail(id) });
      addToast({ type: 'success', message: 'Program updated successfully' });
    },
  });
}

export function useDeleteProgram() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (id) => programApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.programs.all });
      addToast({ type: 'success', message: 'Program deleted successfully' });
    },
  });
}
