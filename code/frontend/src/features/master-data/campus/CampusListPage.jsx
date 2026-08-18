import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCampuses, useDeleteCampus } from './hooks';
import { useDebounce } from '@/hooks/useDebounce';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';
import { ErrorState } from '@/components/ErrorState';
import { DeleteConfirmDialog } from '@/components/DeleteConfirmDialog';
import { ConflictErrorDialog } from '@/components/ConflictErrorDialog';
import { Plus, Pencil, Trash2, Eye } from 'lucide-react';

export default function CampusListPage() {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [conflictMessage, setConflictMessage] = useState(null);

  const debouncedSearch = useDebounce(search, 300);
  const { data, isLoading, isError, refetch } = useCampuses({
    search: debouncedSearch || undefined,
    page,
    size,
  });

  const deleteMutation = useDeleteCampus();

  const confirmDelete = () => {
    if (!deleteTarget) return;
    deleteMutation.mutate(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
      onError: (error) => {
        setDeleteTarget(null);
        if (error.response?.status === 409) {
          setConflictMessage(error.response.data?.message ?? 'Cannot delete: has active children.');
        }
      },
    });
  };

  if (isError) {
    return <ErrorState message="Failed to load campuses" onRetry={refetch} />;
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">Campuses</h1>
        <button
          onClick={() => navigate('new')}
          className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700"
        >
          <Plus className="w-4 h-4" />
          Add Campus
        </button>
      </div>

      {/* Search */}
      <div>
        <input
          type="text"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0); }}
          placeholder="Search by name or code..."
          className="w-full max-w-sm px-4 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
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
                  <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Code</th>
                  <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">City</th>
                  <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">State</th>
                  <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Timezone</th>
                  <th scope="col" className="px-4 py-3 text-left font-medium text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data?.data && data.data.length > 0 ? (
                  data.data.map((campus) => (
                    <tr key={campus.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 font-medium text-gray-900">{campus.name}</td>
                      <td className="px-4 py-3 text-gray-600">{campus.code}</td>
                      <td className="px-4 py-3 text-gray-600">{campus.city || '—'}</td>
                      <td className="px-4 py-3 text-gray-600">{campus.state || '—'}</td>
                      <td className="px-4 py-3 text-gray-600">{campus.timezone}</td>
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => navigate(`${campus.id}/departments`)}
                            className="p-1.5 text-gray-400 hover:text-blue-600 rounded"
                            aria-label={`View departments of ${campus.name}`}
                          >
                            <Eye className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => navigate(`${campus.id}`)}
                            className="p-1.5 text-gray-400 hover:text-blue-600 rounded"
                            aria-label={`Edit ${campus.name}`}
                          >
                            <Pencil className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => setDeleteTarget(campus)}
                            className="p-1.5 text-gray-400 hover:text-red-600 rounded"
                            aria-label={`Delete ${campus.name}`}
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
                      No campuses found. Click &quot;Add Campus&quot; to create one.
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
                Showing {page * size + 1}–{Math.min((page + 1) * size, data.meta.totalElements)} of {data.meta.totalElements}
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

      {/* Conflict Error */}
      <ConflictErrorDialog
        open={Boolean(conflictMessage)}
        message={conflictMessage ?? ''}
        onAcknowledge={() => setConflictMessage(null)}
      />
    </div>
  );
}
