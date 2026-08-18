import { useState } from 'react';
import { ChevronRight, ChevronDown } from 'lucide-react';

export function TreeNode({ label, icon, children, onClick, level, defaultExpanded = false }) {
  const [expanded, setExpanded] = useState(defaultExpanded);
  const hasChildren = Boolean(children);

  return (
    <div className="select-none">
      <div
        className="flex items-center gap-1.5 py-1.5 px-2 rounded-md hover:bg-gray-100 cursor-pointer group"
        style={{ paddingLeft: `${level * 20 + 8}px` }}
      >
        {/* Expand/collapse toggle */}
        {hasChildren ? (
          <button
            onClick={(e) => { e.stopPropagation(); setExpanded(!expanded); }}
            className="p-0.5 text-gray-400 hover:text-gray-600 rounded"
            aria-label={expanded ? 'Collapse' : 'Expand'}
          >
            {expanded ? (
              <ChevronDown className="w-4 h-4" />
            ) : (
              <ChevronRight className="w-4 h-4" />
            )}
          </button>
        ) : (
          <span className="w-5" />
        )}

        {/* Icon */}
        <span className="text-gray-500 flex-shrink-0">{icon}</span>

        {/* Label */}
        <span
          onClick={onClick}
          className="text-sm text-gray-800 group-hover:text-blue-600 truncate"
          role="button"
          tabIndex={0}
          onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') onClick?.(); }}
        >
          {label}
        </span>
      </div>

      {/* Children */}
      {hasChildren && expanded && (
        <div role="group">{children}</div>
      )}
    </div>
  );
}
