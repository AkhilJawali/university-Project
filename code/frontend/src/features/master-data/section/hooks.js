import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sectionApi } from '@/api/sectionApi';
import { queryKeys } from '@/api/queryKeys';
import { useToastStore } from '@/stores/toastStore';

export function useSections(params) {
  return useQuery({
    queryKey: queryKeys.sections.list(params),
    queryFn: () => sectionApi.getAll(params),
    staleTime: 5 * 60 * 1000,
    placeholderData: (prev) => prev,
  });
}

export function useSection(id) {
  return useQuery({
    queryKey: queryKeys.sections.detail(id),
    queryFn: () => sectionApi.getById(id),
    enabled: Boolean(id),
    staleTime: 60_000,
  });
}

export function useCreateSection() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => sectionApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.sections.all });
      addToast({ type: 'success', message: 'Section created successfully' });
    },
  });
}

export function useUpdateSection(id) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => sectionApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.sections.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.sections.detail(id) });
      addToast({ type: 'success', message: 'Section updated successfully' });
    },
  });
}

export function useDeleteSection() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (id) => sectionApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.sections.all });
      addToast({ type: 'success', message: 'Section deleted successfully' });
    },
  });
}
