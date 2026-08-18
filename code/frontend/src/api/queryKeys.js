export const queryKeys = {
  campuses: {
    all: ['campuses'],
    list: (params) => ['campuses', 'list', params],
    detail: (id) => ['campuses', 'detail', id],
    hierarchy: (id) => ['campuses', 'hierarchy', id],
  },
  departments: {
    all: ['departments'],
    list: (params) => ['departments', 'list', params],
    detail: (id) => ['departments', 'detail', id],
  },
  programs: {
    all: ['programs'],
    list: (params) => ['programs', 'list', params],
    detail: (id) => ['programs', 'detail', id],
  },
  batches: {
    all: ['batches'],
    list: (params) => ['batches', 'list', params],
    detail: (id) => ['batches', 'detail', id],
  },
  sections: {
    all: ['sections'],
    list: (params) => ['sections', 'list', params],
    detail: (id) => ['sections', 'detail', id],
  },
  calendars: {
    all: ['calendars'],
    list: (params) => ['calendars', 'list', params],
    detail: (id) => ['calendars', 'detail', id],
    holidays: (calendarId) => ['calendars', 'holidays', calendarId],
    examWindows: (calendarId) => ['calendars', 'examWindows', calendarId],
    specialPeriods: (calendarId) => ['calendars', 'specialPeriods', calendarId],
  },
  grids: {
    all: ['grids'],
    list: (params) => ['grids', 'list', params],
    detail: (id) => ['grids', 'detail', id],
    slots: (gridId) => ['grids', 'slots', gridId],
    workingDays: (gridId) => ['grids', 'workingDays', gridId],
  },
};
