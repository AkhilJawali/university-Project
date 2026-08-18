import { useState, useRef, useCallback } from 'react';

/**
 * 7 toggle buttons (Mon-Sun) for working day configuration.
 * Click auto-saves via onUpdate with 500ms debounce to batch rapid clicks.
 * The last remaining ON toggle is disabled.
 */
const DAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

export function WorkingDayToggles({ workingDays, onUpdate }) {
  // Normalize working days data — handle both array and object shapes
  const initialDays = Array.isArray(workingDays) && workingDays.length === 7
    ? workingDays
    : DAY_LABELS.map((label, idx) => ({
        dayOfWeek: idx,
        label,
        isWorkingDay: idx < 5, // Default Mon-Fri
      }));

  const [days, setDays] = useState(initialDays);
  const pendingRef = useRef(null);
  const timeoutRef = useRef(null);

  const handleToggle = useCallback((dayOfWeek) => {
    const updated = days.map((d) =>
      d.dayOfWeek === dayOfWeek ? { ...d, isWorkingDay: !d.isWorkingDay } : d
    );

    // Ensure at least one working day remains
    if (!updated.some((d) => d.isWorkingDay)) return;

    setDays(updated); // Immediate UI feedback

    // Debounce the mutation — batch rapid clicks into one API call
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    pendingRef.current = updated;
    timeoutRef.current = setTimeout(() => {
      onUpdate({ days: pendingRef.current });
      pendingRef.current = null;
    }, 500);
  }, [days, onUpdate]);

  const activeCount = days.filter((d) => d.isWorkingDay).length;

  return (
    <div className="bg-white border border-gray-200 rounded-lg p-4">
      <h3 className="text-sm font-medium text-gray-700 mb-3">Working Days</h3>
      <div className="flex items-center gap-2" role="group" aria-label="Working day toggles">
        {days.map((day) => {
          const isLastActive = day.isWorkingDay && activeCount === 1;
          return (
            <button
              key={day.dayOfWeek}
              type="button"
              onClick={() => handleToggle(day.dayOfWeek)}
              disabled={isLastActive}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                day.isWorkingDay
                  ? 'bg-blue-600 text-white hover:bg-blue-700'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              } ${isLastActive ? 'opacity-50 cursor-not-allowed' : ''}`}
              aria-label={`${day.label || DAY_LABELS[day.dayOfWeek]}: ${day.isWorkingDay ? 'working day (click to disable)' : 'off day (click to enable)'}`}
              aria-pressed={day.isWorkingDay}
            >
              {day.label || DAY_LABELS[day.dayOfWeek]}
            </button>
          );
        })}
      </div>
      <p className="text-xs text-gray-500 mt-2">
        Click to toggle. At least one working day must remain active.
      </p>
    </div>
  );
}
