import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { gridApi } from '@/api/gridApi';
import { queryKeys } from '@/api/queryKeys';
import { useToastStore } from '@/stores/toastStore';

// --- Grid CRUD ---

export function useGrids(params) {
  return useQuery({
    queryKey: queryKeys.grids.list(params),
    queryFn: () => gridApi.getAll(params),
    staleTime: 5 * 60 * 1000,
    placeholderData: (prev) => prev,
  });
}

export function useGrid(id) {
  return useQuery({
    queryKey: queryKeys.grids.detail(id),
    queryFn: () => gridApi.getById(id),
    enabled: Boolean(id),
    staleTime: 60_000,
  });
}

export function useCreateGrid() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => gridApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.all });
      addToast({ type: 'success', message: 'Time-slot grid created successfully' });
    },
  });
}

export function useUpdateGrid(id) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => gridApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.detail(id) });
      addToast({ type: 'success', message: 'Time-slot grid updated successfully' });
    },
  });
}

export function useDeleteGrid() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (id) => gridApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.all });
      addToast({ type: 'success', message: 'Time-slot grid deleted successfully' });
    },
  });
}

export function useActivateGrid() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (id) => gridApi.activate(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.all });
      addToast({ type: 'success', message: 'Grid activated successfully' });
    },
  });
}

// --- Slots ---

export function useSlots(gridId) {
  return useQuery({
    queryKey: queryKeys.grids.slots(gridId),
    queryFn: () => gridApi.getSlots(gridId),
    enabled: Boolean(gridId),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateSlot(gridId) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => gridApi.createSlot(gridId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.slots(gridId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.detail(gridId) });
      addToast({ type: 'success', message: 'Slot added successfully' });
    },
  });
}

export function useBulkCreateSlots(gridId) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => gridApi.bulkCreateSlots(gridId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.slots(gridId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.detail(gridId) });
      addToast({ type: 'success', message: 'Slots created successfully' });
    },
  });
}

export function useDeleteSlot(gridId) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (slotId) => gridApi.deleteSlot(gridId, slotId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.slots(gridId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.detail(gridId) });
      addToast({ type: 'success', message: 'Slot removed successfully' });
    },
  });
}

// --- Working Days ---

export function useWorkingDays(gridId) {
  return useQuery({
    queryKey: queryKeys.grids.workingDays(gridId),
    queryFn: () => gridApi.getWorkingDays(gridId),
    enabled: Boolean(gridId),
    staleTime: 5 * 60 * 1000,
  });
}

export function useUpdateWorkingDays(gridId) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => gridApi.updateWorkingDays(gridId, data),
    onMutate: async (newData) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.grids.workingDays(gridId) });
      const previous = queryClient.getQueryData(queryKeys.grids.workingDays(gridId));
      queryClient.setQueryData(queryKeys.grids.workingDays(gridId), newData);
      return { previous };
    },
    onError: (_err, _data, context) => {
      queryClient.setQueryData(queryKeys.grids.workingDays(gridId), context?.previous);
      addToast({ type: 'error', message: 'Failed to update working days' });
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.grids.workingDays(gridId) });
    },
    onSuccess: () => {
      addToast({ type: 'success', message: 'Working days updated' });
    },
  });
}
