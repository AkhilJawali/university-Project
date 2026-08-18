import * as Dialog from '@radix-ui/react-dialog';

export function ConflictErrorDialog({ open, message, onAcknowledge }) {
  return (
    <Dialog.Root open={open} onOpenChange={(isOpen) => { if (!isOpen) onAcknowledge(); }}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/50" />
        <Dialog.Content className="fixed left-1/2 top-1/2 z-50 -translate-x-1/2 -translate-y-1/2 bg-white rounded-lg shadow-lg p-6 max-w-md w-full mx-4 focus:outline-none">
          <Dialog.Title className="text-lg font-semibold text-red-700 mb-2">Cannot Delete</Dialog.Title>
          <Dialog.Description className="text-sm text-gray-600 mb-6">{message}</Dialog.Description>
          <button
            onClick={onAcknowledge}
            className="px-4 py-2 text-sm bg-gray-100 rounded-md hover:bg-gray-200"
          >
            Acknowledge
          </button>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
