import * as Dialog from '@radix-ui/react-dialog';

export function DeleteConfirmDialog({ open, entityName, onConfirm, onCancel, isLoading }) {
  return (
    <Dialog.Root open={open} onOpenChange={(isOpen) => { if (!isOpen) onCancel(); }}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/50" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 -translate-x-1/2 -translate-y-1/2 bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4 focus:outline-none">
          <Dialog.Title className="text-lg font-semibold text-gray-900 mb-2">Confirm Delete</Dialog.Title>
          <Dialog.Description className="text-sm text-gray-600 mb-6">
            Are you sure you want to delete <strong>{entityName}</strong>? This action cannot be undone.
          </Dialog.Description>
          <div className="flex justify-end gap-3">
            <button
              onClick={onCancel}
              className="px-4 py-2 text-sm text-gray-700 bg-gray-100 rounded-md hover:bg-gray-200"
            >
              Cancel
            </button>
            <button
              onClick={onConfirm}
              disabled={isLoading}
              className="px-4 py-2 text-sm text-white bg-red-600 rounded-md hover:bg-red-700 disabled:opacity-50"
            >
              {isLoading ? 'Deleting...' : 'Delete'}
            </button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
