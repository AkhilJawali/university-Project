import { create } from 'zustand';

export const useSchedulingConfigStore = create((set) => ({
  activeCalendarTab: 'holidays',
  setActiveCalendarTab: (tab) => set({ activeCalendarTab: tab }),
  quickSetupOpen: false,
  setQuickSetupOpen: (open) => set({ quickSetupOpen: open }),
}));
