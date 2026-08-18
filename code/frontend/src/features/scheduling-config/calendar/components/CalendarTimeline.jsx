/**
 * Visual timeline component for an academic calendar.
 * Displays holidays as red dots, exam windows as orange bands,
 * and special periods as blue bands on a horizontal bar.
 */
export function CalendarTimeline({ startDate, endDate, holidays, examWindows, specialPeriods }) {
  const start = new Date(startDate).getTime();
  const end = new Date(endDate).getTime();
  const totalRange = end - start;

  if (totalRange <= 0) return null;

  const getLeftPercent = (dateStr) => {
    const d = new Date(dateStr).getTime();
    return Math.max(0, Math.min(100, ((d - start) / totalRange) * 100));
  };

  const getWidthPercent = (fromStr, toStr) => {
    const from = new Date(fromStr).getTime();
    const to = new Date(toStr).getTime();
    const leftPct = Math.max(0, ((from - start) / totalRange) * 100);
    const rightPct = Math.min(100, ((to - start) / totalRange) * 100);
    return Math.max(0, rightPct - leftPct);
  };

  return (
    <div className="bg-white border border-gray-200 rounded-lg p-4">
      <h3 className="text-sm font-medium text-gray-700 mb-3">Calendar Timeline</h3>

      {/* Timeline bar */}
      <div className="relative h-12 bg-gray-100 rounded-md overflow-hidden" aria-label="Calendar timeline visualization">
        {/* Special Periods (blue bands) */}
        {specialPeriods.map((sp) => (
          <div
            key={sp.id}
            className="absolute top-0 h-full bg-blue-200 opacity-60"
            style={{
              left: `${getLeftPercent(sp.startDate)}%`,
              width: `${getWidthPercent(sp.startDate, sp.endDate)}%`,
            }}
            title={`${sp.name} (${sp.periodType})`}
          />
        ))}

        {/* Exam Windows (orange bands) */}
        {examWindows.map((ew) => (
          <div
            key={ew.id}
            className="absolute top-0 h-full bg-orange-200 opacity-60"
            style={{
              left: `${getLeftPercent(ew.startDate)}%`,
              width: `${getWidthPercent(ew.startDate, ew.endDate)}%`,
            }}
            title={`${ew.name} (${ew.examType})`}
          />
        ))}

        {/* Holidays (red dots) */}
        {holidays.map((h) => (
          <div
            key={h.id}
            className="absolute top-1/2 -translate-y-1/2 w-3 h-3 bg-red-500 rounded-full -ml-1.5"
            style={{ left: `${getLeftPercent(h.date)}%` }}
            title={`${h.name} (${h.date})`}
          />
        ))}
      </div>

      {/* Date labels */}
      <div className="flex justify-between mt-1 text-xs text-gray-500">
        <span>{startDate}</span>
        <span>{endDate}</span>
      </div>

      {/* Legend */}
      <div className="flex items-center gap-4 mt-3 text-xs text-gray-600">
        <div className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 bg-red-500 rounded-full" />
          <span>Holidays</span>
        </div>
        <div className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 bg-orange-200 rounded" />
          <span>Exam Windows</span>
        </div>
        <div className="flex items-center gap-1">
          <span className="inline-block w-3 h-3 bg-blue-200 rounded" />
          <span>Special Periods</span>
        </div>
      </div>
    </div>
  );
}
