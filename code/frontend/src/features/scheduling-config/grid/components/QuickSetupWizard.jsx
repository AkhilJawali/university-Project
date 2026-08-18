import { useState, useMemo } from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import { useBulkCreateSlots } from '../hooks';
import { SlotTimeline } from './SlotTimeline';
import { X } from 'lucide-react';

/**
 * 3-step wizard for quickly generating a time slot grid.
 * Step 1: Define lecture parameters (start time, duration, count)
 * Step 2: Configure breaks (break after slot, lunch after slot)
 * Step 3: Preview and confirm (bulk create)
 */
export function QuickSetupWizard({ open, onClose, gridId }) {
  const [step, setStep] = useState(1);
  const [config, setConfig] = useState({
    startTime: '08:00',
    lectureDuration: 60,
    lectureCount: 6,
    breakAfterSlot: 3,
    breakDuration: 15,
    lunchAfterSlot: 4,
    lunchDuration: 45,
  });

  const bulkCreateMutation = useBulkCreateSlots(gridId);

  const generatedSlots = useMemo(() => generateSlots(config), [config]);

  const handleConfirm = () => {
    bulkCreateMutation.mutate({ slots: generatedSlots }, {
      onSuccess: () => {
        setStep(1);
        setConfig({
          startTime: '08:00',
          lectureDuration: 60,
          lectureCount: 6,
          breakAfterSlot: 3,
          breakDuration: 15,
          lunchAfterSlot: 4,
          lunchDuration: 45,
        });
        onClose();
      },
    });
  };

  return (
    <Dialog.Root open={open} onOpenChange={(isOpen) => { if (!isOpen) onClose(); }}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/50" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 -translate-x-1/2 -translate-y-1/2 bg-white rounded-lg shadow-xl max-w-lg w-full mx-4 p-6 space-y-4 focus:outline-none">
          <div className="flex items-center justify-between">
            <Dialog.Title className="text-lg font-semibold text-gray-900">
              Quick Setup — Step {step} of 3
            </Dialog.Title>
            <Dialog.Close asChild>
              <button className="p-1 text-gray-400 hover:text-gray-600 rounded" aria-label="Close wizard">
                <X className="w-5 h-5" />
              </button>
            </Dialog.Close>
          </div>
          <Dialog.Description className="sr-only">
            A 3-step wizard to quickly generate time slots for the grid.
          </Dialog.Description>

        {/* Step 1: Lecture Parameters */}
        {step === 1 && (
          <div className="space-y-3">
            <p className="text-sm text-gray-600">Configure the basic lecture schedule.</p>
            <div className="grid grid-cols-3 gap-3">
              <div>
                <label htmlFor="qs-start" className="block text-xs font-medium text-gray-600 mb-1">Start Time</label>
                <input
                  id="qs-start"
                  type="time"
                  value={config.startTime}
                  onChange={(e) => setConfig({ ...config, startTime: e.target.value })}
                  className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
                />
              </div>
              <div>
                <label htmlFor="qs-duration" className="block text-xs font-medium text-gray-600 mb-1">Duration (min)</label>
                <input
                  id="qs-duration"
                  type="number"
                  min="30"
                  max="180"
                  step="15"
                  value={config.lectureDuration}
                  onChange={(e) => setConfig({ ...config, lectureDuration: Number(e.target.value) })}
                  className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
                />
              </div>
              <div>
                <label htmlFor="qs-count" className="block text-xs font-medium text-gray-600 mb-1">Lecture Count</label>
                <input
                  id="qs-count"
                  type="number"
                  min="1"
                  max="12"
                  value={config.lectureCount}
                  onChange={(e) => setConfig({ ...config, lectureCount: Number(e.target.value) })}
                  className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
                />
              </div>
            </div>
          </div>
        )}

        {/* Step 2: Breaks */}
        {step === 2 && (
          <div className="space-y-3">
            <p className="text-sm text-gray-600">Configure break and lunch timing.</p>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label htmlFor="qs-break-after" className="block text-xs font-medium text-gray-600 mb-1">Break after lecture #</label>
                <input
                  id="qs-break-after"
                  type="number"
                  min="1"
                  max={config.lectureCount}
                  value={config.breakAfterSlot}
                  onChange={(e) => setConfig({ ...config, breakAfterSlot: Number(e.target.value) })}
                  className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
                />
              </div>
              <div>
                <label htmlFor="qs-break-dur" className="block text-xs font-medium text-gray-600 mb-1">Break Duration (min)</label>
                <input
                  id="qs-break-dur"
                  type="number"
                  min="5"
                  max="30"
                  step="5"
                  value={config.breakDuration}
                  onChange={(e) => setConfig({ ...config, breakDuration: Number(e.target.value) })}
                  className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
                />
              </div>
              <div>
                <label htmlFor="qs-lunch-after" className="block text-xs font-medium text-gray-600 mb-1">Lunch after lecture #</label>
                <input
                  id="qs-lunch-after"
                  type="number"
                  min="1"
                  max={config.lectureCount}
                  value={config.lunchAfterSlot}
                  onChange={(e) => setConfig({ ...config, lunchAfterSlot: Number(e.target.value) })}
                  className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
                />
              </div>
              <div>
                <label htmlFor="qs-lunch-dur" className="block text-xs font-medium text-gray-600 mb-1">Lunch Duration (min)</label>
                <input
                  id="qs-lunch-dur"
                  type="number"
                  min="15"
                  max="90"
                  step="15"
                  value={config.lunchDuration}
                  onChange={(e) => setConfig({ ...config, lunchDuration: Number(e.target.value) })}
                  className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
                />
              </div>
            </div>
          </div>
        )}

        {/* Step 3: Preview */}
        {step === 3 && (
          <div className="space-y-3">
            <p className="text-sm text-gray-600">Preview the generated slots before confirming.</p>
            <SlotTimeline slots={generatedSlots.map((s, i) => ({ ...s, id: `preview-${i}` }))} />
            <div className="bg-gray-50 rounded p-3 text-xs text-gray-600 max-h-40 overflow-y-auto">
              {generatedSlots.map((s) => (
                <div key={s.slotNumber} className="flex gap-3 py-0.5">
                  <span className="font-medium w-8">#{s.slotNumber}</span>
                  <span>{s.startTime} - {s.endTime}</span>
                  <span className="text-gray-500">{s.slotType}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Navigation */}
        <div className="flex justify-between pt-3 border-t border-gray-100">
          <button
            type="button"
            onClick={() => step > 1 ? setStep(step - 1) : onClose()}
            className="px-4 py-2 text-sm text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200"
          >
            {step === 1 ? 'Cancel' : 'Back'}
          </button>
          {step < 3 ? (
            <button
              type="button"
              onClick={() => setStep(step + 1)}
              className="px-4 py-2 text-sm text-white bg-blue-600 rounded-md hover:bg-blue-700"
            >
              Next
            </button>
          ) : (
            <button
              type="button"
              onClick={handleConfirm}
              disabled={bulkCreateMutation.isPending}
              className="px-4 py-2 text-sm text-white bg-green-600 rounded-md hover:bg-green-700 disabled:opacity-50"
            >
              {bulkCreateMutation.isPending ? 'Creating...' : 'Confirm & Create'}
            </button>
          )}
        </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}

function generateSlots(config) {
  const slots = [];
  let currentTime = timeToMinutes(config.startTime);
  let slotNumber = 1;
  let lecturesPlaced = 0;

  // Build insertion plan: sorted list of (afterLecture, type, duration)
  const insertions = [];
  if (config.breakDuration > 0 && config.breakAfterSlot <= config.lectureCount) {
    insertions.push({ afterLecture: config.breakAfterSlot, type: 'BREAK', duration: config.breakDuration });
  }
  if (config.lunchDuration > 0 && config.lunchAfterSlot <= config.lectureCount) {
    insertions.push({ afterLecture: config.lunchAfterSlot, type: 'LUNCH', duration: config.lunchDuration });
  }
  // Sort by afterLecture ascending; if same, break before lunch
  insertions.sort((a, b) => a.afterLecture - b.afterLecture || (a.type === 'BREAK' ? -1 : 1));

  while (lecturesPlaced < config.lectureCount) {
    // Place lecture
    const lectureEnd = currentTime + config.lectureDuration;
    slots.push({
      slotNumber: slotNumber++,
      startTime: minutesToTime(currentTime),
      endTime: minutesToTime(lectureEnd),
      slotType: 'LECTURE',
    });
    currentTime = lectureEnd;
    lecturesPlaced++;

    // Check if any insertions fire after this lecture
    for (const ins of insertions) {
      if (ins.afterLecture === lecturesPlaced && lecturesPlaced < config.lectureCount) {
        const end = currentTime + ins.duration;
        slots.push({
          slotNumber: slotNumber++,
          startTime: minutesToTime(currentTime),
          endTime: minutesToTime(end),
          slotType: ins.type,
        });
        currentTime = end;
      }
    }
  }

  return slots;
}

function timeToMinutes(time) {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

function minutesToTime(minutes) {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}
