import * as Dialog from '@radix-ui/react-dialog';
import { AlertTriangle } from 'lucide-react';

/**
 * Confirmation dialog for grid activation.
 * Warns that activating will deactivate the current active grid.
 * Uses Radix Dialog for automatic focus trapping and Escape key handling.
 */
export function GridActivationDialog({ open, gridName, onConfirm, onCancel, isLoading }) {
  return (
    <Dialog.Root open={open} onOpenChange={(isOpen) => { if (!isOpen) onCancel(); }}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/50" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 -translate-x-1/2 -translate-y-1/2 bg-white rounded-lg shadow-xl max-w-md w-full mx-4 p-6 focus:outline-none">
          <div className="flex items-start gap-3">
            <div className="flex-shrink-0 w-10 h-10 bg-yellow-50 rounded-full flex items-center justify-center">
              <AlertTriangle className="w-5 h-5 text-yellow-600" />
            </div>
            <div>
              <Dialog.Title className="text-lg font-semibold text-gray-900">Activate Grid</Dialog.Title>
              <Dialog.Description className="text-sm text-gray-600 mt-1">
                Activating <strong>{gridName}</strong> will deactivate the currently active grid for this campus.
                Are you sure you want to continue?
              </Dialog.Description>
            </div>
          </div>

          <div className="flex justify-end gap-2 mt-6">
            <button
              type="button"
              onClick={onCancel}
              disabled={isLoading}
              className="px-4 py-2 text-sm text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200 disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={onConfirm}
              disabled={isLoading}
              className="px-4 py-2 text-sm text-white bg-green-600 rounded-md hover:bg-green-700 disabled:opacity-50"
            >
              {isLoading ? 'Activating...' : 'Yes, Activate'}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
