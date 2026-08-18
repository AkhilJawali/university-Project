import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { campusApi } from '@/api/campusApi';
import { queryKeys } from '@/api/queryKeys';
import { useToastStore } from '@/stores/toastStore';

export function useCampuses(params) {
  return useQuery({
    queryKey: queryKeys.campuses.list(params),
    queryFn: () => campusApi.getAll(params),
    staleTime: 5 * 60 * 1000,
    placeholderData: (prev) => prev,
  });
}

export function useCampus(id) {
  return useQuery({
    queryKey: queryKeys.campuses.detail(id),
    queryFn: () => campusApi.getById(id),
    enabled: Boolean(id),
    staleTime: 60_000,
  });
}

export function useCreateCampus() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => campusApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.campuses.all });
      addToast({ type: 'success', message: 'Campus created successfully' });
    },
  });
}

export function useUpdateCampus(id) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => campusApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.campuses.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.campuses.detail(id) });
      addToast({ type: 'success', message: 'Campus updated successfully' });
    },
  });
}

export function useDeleteCampus() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (id) => campusApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.campuses.all });
      addToast({ type: 'success', message: 'Campus deleted successfully' });
    },
  });
}

export function useCampusHierarchy(campusId) {
  return useQuery({
    queryKey: queryKeys.campuses.hierarchy(campusId),
    queryFn: () => campusApi.getHierarchy(campusId),
    enabled: Boolean(campusId),
    staleTime: 60_000,
  });
}
