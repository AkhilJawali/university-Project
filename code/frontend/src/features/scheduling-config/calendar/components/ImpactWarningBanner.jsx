import { useState } from 'react';
import { AlertTriangle, ChevronDown, ChevronUp, X } from 'lucide-react';

/**
 * Yellow warning banner shown when holiday creation impacts existing sessions.
 * Displays a count with an expandable list of affected sessions.
 */
export function ImpactWarningBanner({ sessions, onDismiss }) {
  const [expanded, setExpanded] = useState(false);

  if (!sessions || sessions.length === 0) return null;

  return (
    <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4" role="alert">
      <div className="flex items-start justify-between">
        <div className="flex items-start gap-2">
          <AlertTriangle className="w-5 h-5 text-yellow-600 mt-0.5 flex-shrink-0" />
          <div>
            <p className="text-sm font-medium text-yellow-800">
              {sessions.length} session{sessions.length > 1 ? 's' : ''} impacted by this holiday
            </p>
            <p className="text-xs text-yellow-700 mt-0.5">
              These sessions fall on the new holiday date and may need rescheduling.
            </p>
          </div>
        </div>
        <div className="flex items-center gap-1">
          <button
            onClick={() => setExpanded(!expanded)}
            className="p-1 text-yellow-600 hover:text-yellow-800 rounded"
            aria-label={expanded ? 'Collapse session list' : 'Expand session list'}
          >
            {expanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
          <button
            onClick={onDismiss}
            className="p-1 text-yellow-600 hover:text-yellow-800 rounded"
            aria-label="Dismiss warning"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      {expanded && (
        <ul className="mt-3 ml-7 space-y-1 text-xs text-yellow-800">
          {sessions.map((session, idx) => (
            <li key={session.id || idx} className="flex items-center gap-2">
              <span className="inline-block w-1.5 h-1.5 bg-yellow-500 rounded-full" />
              <span>
                {session.courseName || session.courseCode || 'Session'}{' '}
                {session.time && `at ${session.time}`}{' '}
                {session.room && `in ${session.room}`}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
