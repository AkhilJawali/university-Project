/**
 * Visual timeline of time slots as colored blocks on a horizontal bar.
 * LECTURE: blue, TUTORIAL: green, PRACTICAL: orange, BREAK: gray, LUNCH: yellow.
 * Proportional widths based on duration.
 */
export function SlotTimeline({ slots }) {
  if (!slots || slots.length === 0) return null;

  const sortedSlots = [...slots].sort((a, b) => a.slotNumber - b.slotNumber);

  // Compute total minutes in the timeline
  const totalMinutes = sortedSlots.reduce((acc, slot) => {
    const [sH, sM] = slot.startTime.split(':').map(Number);
    const [eH, eM] = slot.endTime.split(':').map(Number);
    return acc + (eH * 60 + eM) - (sH * 60 + sM);
  }, 0);

  if (totalMinutes <= 0) return null;

  const typeColors = {
    LECTURE: 'bg-blue-400 text-blue-900',
    TUTORIAL: 'bg-green-400 text-green-900',
    PRACTICAL: 'bg-orange-400 text-orange-900',
    BREAK: 'bg-gray-300 text-gray-700',
    LUNCH: 'bg-yellow-300 text-yellow-800',
  };

  return (
    <div className="bg-white border border-gray-200 rounded-lg p-4">
      <h3 className="text-sm font-medium text-gray-700 mb-3">Slot Timeline</h3>

      <div className="flex h-14 rounded-md overflow-hidden" aria-label="Time slot timeline">
        {sortedSlots.map((slot) => {
          const [sH, sM] = slot.startTime.split(':').map(Number);
          const [eH, eM] = slot.endTime.split(':').map(Number);
          const duration = (eH * 60 + eM) - (sH * 60 + sM);
          const widthPct = (duration / totalMinutes) * 100;
          const colorClass = typeColors[slot.slotType] || 'bg-gray-200 text-gray-700';

          return (
            <div
              key={slot.id || slot.slotNumber}
              className={`flex flex-col items-center justify-center text-xs px-1 border-r border-white last:border-r-0 ${colorClass}`}
              style={{ width: `${widthPct}%`, minWidth: '2rem' }}
              title={`Slot ${slot.slotNumber}: ${slot.startTime} - ${slot.endTime} (${slot.slotType}, ${duration}min)`}
            >
              <span className="font-medium">#{slot.slotNumber}</span>
              <span className="opacity-75 text-[10px]">{slot.startTime}-{slot.endTime}</span>
              <span className="opacity-75 text-[10px]">{duration}m</span>
            </div>
          );
        })}
      </div>

      {/* Legend */}
      <div className="flex items-center gap-3 mt-3 text-xs text-gray-600">
        <div className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 bg-blue-400 rounded" />
          <span>Lecture</span>
        </div>
        <div className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 bg-green-400 rounded" />
          <span>Tutorial</span>
        </div>
        <div className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 bg-orange-400 rounded" />
          <span>Practical</span>
        </div>
        <div className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 bg-gray-300 rounded" />
          <span>Break</span>
        </div>
        <div className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 bg-yellow-300 rounded" />
          <span>Lunch</span>
        </div>
      </div>
    </div>
  );
}
