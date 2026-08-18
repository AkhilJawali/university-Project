import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DeleteConfirmDialog } from '../DeleteConfirmDialog';

describe('DeleteConfirmDialog', () => {
  it('should render when open is true', () => {
    render(
      <DeleteConfirmDialog
        open={true}
        entityName="Main Campus"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Confirm Delete')).toBeInTheDocument();
    expect(screen.getByText('Main Campus')).toBeInTheDocument();
  });

  it('should not render when open is false', () => {
    const { container } = render(
      <DeleteConfirmDialog
        open={false}
        entityName="Main Campus"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    expect(container.innerHTML).toBe('');
  });

  it('should call onConfirm when delete button is clicked', async () => {
    const onConfirm = vi.fn();
    const user = userEvent.setup();

    render(
      <DeleteConfirmDialog
        open={true}
        entityName="Main Campus"
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />
    );

    await user.click(screen.getByRole('button', { name: /delete/i }));
    expect(onConfirm).toHaveBeenCalledTimes(1);
  });

  it('should call onCancel when cancel button is clicked', async () => {
    const onCancel = vi.fn();
    const user = userEvent.setup();

    render(
      <DeleteConfirmDialog
        open={true}
        entityName="Main Campus"
        onConfirm={vi.fn()}
        onCancel={onCancel}
      />
    );

    await user.click(screen.getByRole('button', { name: /cancel/i }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('should show loading state when isLoading is true', () => {
    render(
      <DeleteConfirmDialog
        open={true}
        entityName="Main Campus"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
        isLoading={true}
      />
    );

    const deleteButton = screen.getByRole('button', { name: /deleting/i });
    expect(deleteButton).toBeDisabled();
    expect(deleteButton).toHaveTextContent('Deleting...');
  });

  it('should display the entity name in the confirmation message', () => {
    render(
      <DeleteConfirmDialog
        open={true}
        entityName="Engineering Department"
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />
    );

    expect(screen.getByText('Engineering Department')).toBeInTheDocument();
  });
});
