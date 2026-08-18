import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { calendarApi } from '@/api/calendarApi';
import { queryKeys } from '@/api/queryKeys';
import { useToastStore } from '@/stores/toastStore';

// --- Calendar CRUD ---

export function useCalendars(params) {
  return useQuery({
    queryKey: queryKeys.calendars.list(params),
    queryFn: () => calendarApi.getAll(params),
    staleTime: 5 * 60 * 1000,
    placeholderData: (prev) => prev,
  });
}

export function useCalendar(id) {
  return useQuery({
    queryKey: queryKeys.calendars.detail(id),
    queryFn: () => calendarApi.getById(id),
    enabled: Boolean(id),
    staleTime: 60_000,
  });
}

export function useCreateCalendar() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => calendarApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.all });
      addToast({ type: 'success', message: 'Academic calendar created successfully' });
    },
  });
}

export function useUpdateCalendar(id) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => calendarApi.update(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.detail(id) });
      addToast({ type: 'success', message: 'Academic calendar updated successfully' });
    },
  });
}

export function useDeleteCalendar() {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (id) => calendarApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.all });
      addToast({ type: 'success', message: 'Academic calendar deleted successfully' });
    },
  });
}

// --- Holidays ---

export function useHolidays(calendarId) {
  return useQuery({
    queryKey: queryKeys.calendars.holidays(calendarId),
    queryFn: () => calendarApi.getHolidays(calendarId),
    enabled: Boolean(calendarId),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateHoliday(calendarId) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => calendarApi.createHoliday(calendarId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.holidays(calendarId) });
      addToast({ type: 'success', message: 'Holiday added successfully' });
    },
  });
}

export function useDeleteHoliday(calendarId) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (holidayId) => calendarApi.deleteHoliday(calendarId, holidayId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.holidays(calendarId) });
      addToast({ type: 'success', message: 'Holiday removed successfully' });
    },
  });
}

// --- Exam Windows ---

export function useExamWindows(calendarId) {
  return useQuery({
    queryKey: queryKeys.calendars.examWindows(calendarId),
    queryFn: () => calendarApi.getExamWindows(calendarId),
    enabled: Boolean(calendarId),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateExamWindow(calendarId) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => calendarApi.createExamWindow(calendarId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.examWindows(calendarId) });
      addToast({ type: 'success', message: 'Exam window added successfully' });
    },
  });
}

export function useDeleteExamWindow(calendarId) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (windowId) => calendarApi.deleteExamWindow(calendarId, windowId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.examWindows(calendarId) });
      addToast({ type: 'success', message: 'Exam window removed successfully' });
    },
  });
}

// --- Special Periods ---

export function useSpecialPeriods(calendarId) {
  return useQuery({
    queryKey: queryKeys.calendars.specialPeriods(calendarId),
    queryFn: () => calendarApi.getSpecialPeriods(calendarId),
    enabled: Boolean(calendarId),
    staleTime: 5 * 60 * 1000,
  });
}

export function useCreateSpecialPeriod(calendarId) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (data) => calendarApi.createSpecialPeriod(calendarId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.specialPeriods(calendarId) });
      addToast({ type: 'success', message: 'Special period added successfully' });
    },
  });
}

export function useDeleteSpecialPeriod(calendarId) {
  const queryClient = useQueryClient();
  const addToast = useToastStore((s) => s.addToast);

  return useMutation({
    mutationFn: (periodId) => calendarApi.deleteSpecialPeriod(calendarId, periodId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.calendars.specialPeriods(calendarId) });
      addToast({ type: 'success', message: 'Special period removed successfully' });
    },
  });
}
