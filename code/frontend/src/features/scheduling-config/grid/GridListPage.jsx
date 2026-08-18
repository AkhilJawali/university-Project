import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useGrids, useDeleteGrid, useActivateGrid } from './hooks';
import { useCampuses } from '@/features/master-data/campus/hooks';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';
import { ErrorState } from '@/components/ErrorState';
import { DeleteConfirmDialog } from '@/components/DeleteConfirmDialog';
import { ConflictErrorDialog } from '@/components/ConflictErrorDialog';
import { GridActivationDialog } from './components/GridActivationDialog';
import { Plus, Pencil, Trash2, Eye, Zap } from 'lucide-react';

export default function GridListPage() {
  const navigate = useNavigate();
  const [campusFilter, setCampusFilter] = useState('');
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [activateTarget, setActivateTarget] = useState(null);
  const [conflictMessage, setConflictMessage] = useState(null);

  const { data: campusData } = useCampuses({ size: 100 });

  const { data, isLoading, isError, refetch } = useGrids({
    campusId: campusFilter || undefined,
    page,
    size,
  });

  const deleteMutation = useDeleteGrid();
  const activateMutation = useActivateGrid();

  const confirmDelete = () => {
    if (!deleteTarget) return;
    deleteMutation.mutate(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
      onError: (error) => {
        setDeleteTarget(null);
        if (error.response?.status === 409) {
          setConflictMessage(
            error.response.data?.message ?? 'Cannot delete: grid has active references.'
          );
        }
      },
    });
  };

  const confirmActivate = () => {
    if (!activateTarget) return;
    activateMutation.mutate(activateTarget.id, {
      onSuccess: () => setActivateTarget(null),
    });
  };

  if (isError) {
    return <ErrorState message="Failed to load time-slot grids" onRetry={refetch} />;
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">Time-Slot Grids</h1>
        <button
          onClick={() => navigate('new')}
          className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700"
        >
          <Plus className="w-4 h-4" />
          Create Grid
        </button>
      </div>

      {/* Filter */}
      <div>
        <select
          value={campusFilter}
          onChange={(e) => {
            setCampusFilter(e.target.value);
            setPage(0);
          }}
          className="w-full max-w-sm px-4 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          aria-label="Filter by campus"
        >
          <option value="">All Campuses</option>
          {campusData?.data?.map((campus) => (
            <option key={campus.id} value={campus.id}>
              {campus.name}
            </option>
          ))}
        </select>
      </div>

      {/* Table */}
      {isLoading ? (
        <LoadingSkeleton rows={10} columns={6} />
      ) : (
        <>
          <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Name</th>
                  <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Campus</th>
                  <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Effective From</th>
                  <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Status</th>
                  <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Slots</th>
                  <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data?.data && data.data.length > 0 ? (
                  data.data.map((grid) => (
                    <tr key={grid.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 font-medium text-gray-900">{grid.name}</td>
                      <td className="px-4 py-3 text-gray-600">{grid.campusName || '—'}</td>
                      <td className="px-4 py-3 text-gray-600">{grid.effectiveFrom}</td>
                      <td className="px-4 py-3">
                        {grid.active ? (
                          <span className="inline-flex px-2 py-0.5 text-xs font-medium rounded-full bg-green-50 text-green-700">
                            Active
                          </span>
                        ) : (
                          <span className="inline-flex px-2 py-0.5 text-xs font-medium rounded-full bg-gray-100 text-gray-600">
                            Inactive
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-gray-600">{grid.slotCount ?? '—'}</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => navigate(`${grid.id}`)}
                            className="p-1.5 text-gray-400 hover:text-blue-600 rounded"
                            aria-label={`View ${grid.name}`}
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => navigate(`${grid.id}/edit`)}
                            className="p-1.5 text-gray-400 hover:text-blue-600 rounded"
                            aria-label={`Edit ${grid.name}`}
                          >
                            <Pencil className="w-4 h-4" />
                          </button>
                          {!grid.active && (
                            <button
                              onClick={() => setActivateTarget(grid)}
                              className="p-1.5 text-gray-400 hover:text-green-600 rounded"
                              aria-label={`Activate ${grid.name}`}
                            >
                              <Zap className="w-4 h-4" />
                            </button>
                          )}
                          <button
                            onClick={() => setDeleteTarget(grid)}
                            className="p-1.5 text-gray-400 hover:text-red-600 rounded"
                            aria-label={`Delete ${grid.name}`}
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={6} className="px-4 py-12 text-center text-gray-500">
                      No time-slot grids found. Click &quot;Create Grid&quot; to create one.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {data?.meta && (
            <div className="flex items-center justify-between text-sm text-gray-600">
              <span>
                Showing {page * size + 1}–{Math.min((page + 1) * size, data.meta.totalElements)} of{' '}
                {data.meta.totalElements}
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-3 py-1 border border-gray-300 rounded disabled:opacity-50"
                >
                  Previous
                </button>
                <button
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= data.meta.totalPages - 1}
                  className="px-3 py-1 border border-gray-300 rounded disabled:opacity-50"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Delete Confirmation */}
      <DeleteConfirmDialog
        open={Boolean(deleteTarget)}
        entityName={deleteTarget?.name ?? ''}
        onConfirm={confirmDelete}
        onCancel={() => setDeleteTarget(null)}
        isLoading={deleteMutation.isPending}
      />

      {/* Activate Confirmation */}
      <GridActivationDialog
        open={Boolean(activateTarget)}
        gridName={activateTarget?.name ?? ''}
        onConfirm={confirmActivate}
        onCancel={() => setActivateTarget(null)}
        isLoading={activateMutation.isPending}
      />

      {/* Conflict Error */}
      <ConflictErrorDialog
        open={Boolean(conflictMessage)}
        message={conflictMessage ?? ''}
        onAcknowledge={() => setConflictMessage(null)}
      />
    </div>
  );
}
