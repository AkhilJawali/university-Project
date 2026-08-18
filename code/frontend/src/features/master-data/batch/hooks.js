import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { batchApi } from '@/api/batchApi';
import { queryKeys } from '@/api/queryKeys';
import { useToastStore } from '@/stores/toastStore';

export function useBatches(params) {
  return useQuery({
    queryKey: queryKeys.batches.list(params),
    queryFn: () => batchApi.getAll(params),
    staleTime: 5 * 60 * 1000,
    placeholderData: (prev) => prev,
  });
}

export function useBatch(id) {
  return useQuery({
    queryKey: queryKeys.batches.detail(id),
    queryFn: () => batchApi.getById(id),
    enabled: Boolean(id),
    staleTime: 60_000,
  });
}

export function useCreateBatch() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => batchApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.batches.all });
      addToast({ type: 'success', message: 'Batch created successfully' });
    },
  });
}

export function useUpdateBatch(id) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => batchApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.batches.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.batches.detail(id) });
      addToast({ type: 'success', message: 'Batch updated successfully' });
    },
  });
}

export function useDeleteBatch() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (id) => batchApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.batches.all });
      addToast({ type: 'success', message: 'Batch deleted successfully' });
    },
  });
}
