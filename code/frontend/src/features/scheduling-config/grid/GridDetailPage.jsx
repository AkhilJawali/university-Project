import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { slotSchema } from './schemas';
import { useGrid, useSlots, useCreateSlot, useDeleteSlot, useActivateGrid, useWorkingDays, useUpdateWorkingDays } from './hooks';
import { useSchedulingConfigStore } from '@/stores/schedulingConfigStore';
import { SlotTimeline } from './components/SlotTimeline';
import { QuickSetupWizard } from './components/QuickSetupWizard';
import { WorkingDayToggles } from './components/WorkingDayToggles';
import { GridActivationDialog } from './components/GridActivationDialog';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';
import { ErrorState } from '@/components/ErrorState';
import { Pencil, Trash2, Plus, Zap } from 'lucide-react';

export default function GridDetailPage() {
  const { gridId } = useParams();
  const navigate = useNavigate();
  const numericId = Number(gridId);

  const { quickSetupOpen, setQuickSetupOpen } = useSchedulingConfigStore();
  const [showSlotForm, setShowSlotForm] = useState(false);
  const [activateDialogOpen, setActivateDialogOpen] = useState(false);

  const { data: grid, isLoading, isError, refetch } = useGrid(numericId);
  const { data: slotsData } = useSlots(numericId);
  const { data: workingDaysData } = useWorkingDays(numericId);

  const createSlotMutation = useCreateSlot(numericId);
  const deleteSlotMutation = useDeleteSlot(numericId);
  const activateMutation = useActivateGrid();
  const updateWorkingDaysMutation = useUpdateWorkingDays(numericId);

  const slots = slotsData?.data || slotsData || [];
  const workingDays = workingDaysData?.data || workingDaysData || [];

  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: zodResolver(slotSchema),
    defaultValues: { slotNumber: '', startTime: '', endTime: '', slotType: '' },
  });

  const onSlotSubmit = (data) => {
    createSlotMutation.mutate({ ...data, slotNumber: Number(data.slotNumber) }, {
      onSuccess: () => { reset(); setShowSlotForm(false); },
    });
  };

  const handleActivate = () => {
    activateMutation.mutate(numericId, {
      onSuccess: () => setActivateDialogOpen(false),
    });
  };

  if (isLoading) return <LoadingSkeleton rows={8} columns={4} />;
  if (isError) return <ErrorState message="Failed to load grid" onRetry={refetch} />;
  if (!grid) return null;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">{grid.name}</h1>
          <p className="text-sm text-gray-600 mt-1">
            {grid.campusName} &middot; Effective from {grid.effectiveFrom} &middot;{' '}
            {grid.active ? (
              <span className="inline-flex px-2 py-0.5 text-xs font-medium rounded-full bg-green-50 text-green-700">Active</span>
            ) : (
              <span className="inline-flex px-2 py-0.5 text-xs font-medium rounded-full bg-gray-100 text-gray-600">Inactive</span>
            )}
          </p>
        </div>
        <div className="flex items-center gap-2">
          {!grid.active && (
            <button
              onClick={() => setActivateDialogOpen(true)}
              className="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white text-sm font-medium rounded-md hover:bg-green-700"
            >
              <Zap className="w-4 h-4" />
              Activate
            </button>
          )}
          <button
            onClick={() => navigate(`/admin/scheduling-config/grids/${numericId}/edit`)}
            className="inline-flex items-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 text-sm font-medium rounded-md hover:bg-gray-200"
          >
            <Pencil className="w-4 h-4" />
            Edit
          </button>
        </div>
      </div>

      {/* Slot Timeline */}
      {slots.length > 0 && <SlotTimeline slots={slots} />}

      {/* Slots Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-medium text-gray-900">Time Slots ({slots.length})</h2>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setQuickSetupOpen(true)}
              className="inline-flex items-center gap-1 px-3 py-1.5 bg-purple-600 text-white text-sm rounded-md hover:bg-purple-700"
            >
              Quick Setup
            </button>
            <button
              onClick={() => setShowSlotForm(!showSlotForm)}
              className="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700"
            >
              <Plus className="w-3.5 h-3.5" />
              Add Slot
            </button>
          </div>
        </div>

        {/* Slot Form */}
        {showSlotForm && (
          <form onSubmit={handleSubmit(onSlotSubmit)} className="bg-gray-50 border border-gray-200 rounded-lg p-4 space-y-3">
            <div className="grid grid-cols-4 gap-3">
              <div>
                <label htmlFor="slotNumber" className="block text-xs font-medium text-gray-600 mb-1">Slot #</label>
                <input
                  id="slotNumber"
                  type="number"
                  min="1"
                  {...register('slotNumber', { valueAsNumber: true })}
                  className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm"
                />
                {errors.slotNumber && <p className="text-xs text-red-600 mt-0.5">{errors.slotNumber.message}</p>}
              </div>
              <div>
                <label htmlFor="startTime" className="block text-xs font-medium text-gray-600 mb-1">Start</label>
                <input id="startTime" type="time" {...register('startTime')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm" />
                {errors.startTime && <p className="text-xs text-red-600 mt-0.5">{errors.startTime.message}</p>}
              </div>
              <div>
                <label htmlFor="endTime" className="block text-xs font-medium text-gray-600 mb-1">End</label>
                <input id="endTime" type="time" {...register('endTime')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm" />
                {errors.endTime && <p className="text-xs text-red-600 mt-0.5">{errors.endTime.message}</p>}
              </div>
              <div>
                <label htmlFor="slotType" className="block text-xs font-medium text-gray-600 mb-1">Type</label>
                <select id="slotType" {...register('slotType')} className="w-full px-2 py-1.5 border border-gray-300 rounded text-sm">
                  <option value="">Select</option>
                  <option value="LECTURE">Lecture</option>
                  <option value="TUTORIAL">Tutorial</option>
                  <option value="PRACTICAL">Practical</option>
                  <option value="BREAK">Break</option>
                  <option value="LUNCH">Lunch</option>
                </select>
                {errors.slotType && <p className="text-xs text-red-600 mt-0.5">{errors.slotType.message}</p>}
              </div>
            </div>
            <div className="flex gap-2">
              <button type="submit" disabled={createSlotMutation.isPending} className="px-3 py-1.5 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 disabled:opacity-50">
                {createSlotMutation.isPending ? 'Adding...' : 'Add'}
              </button>
              <button type="button" onClick={() => { setShowSlotForm(false); reset(); }} className="px-3 py-1.5 bg-gray-200 text-gray-700 text-sm rounded hover:bg-gray-300">
                Cancel
              </button>
            </div>
          </form>
        )}

        {/* Slots Table */}
        {slots.length > 0 ? (
          <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">#</th>
                  <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Start</th>
                  <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">End</th>
                  <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Duration</th>
                  <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Type</th>
                  <th scope="col" className="px-4 py-2 text-left font-medium text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {slots.map((slot) => {
                  const [sH, sM] = (slot.startTime || '').split(':').map(Number);
                  const [eH, eM] = (slot.endTime || '').split(':').map(Number);
                  const duration = (eH * 60 + eM) - (sH * 60 + sM);
                  return (
                    <tr key={slot.id} className="hover:bg-gray-50">
                      <td className="px-4 py-2 text-gray-900">{slot.slotNumber}</td>
                      <td className="px-4 py-2 text-gray-600">{slot.startTime}</td>
                      <td className="px-4 py-2 text-gray-600">{slot.endTime}</td>
                      <td className="px-4 py-2 text-gray-600">{duration} min</td>
                      <td className="px-4 py-2">
                        <SlotTypeBadge type={slot.slotType} />
                      </td>
                      <td className="px-4 py-2">
                        <button
                          onClick={() => deleteSlotMutation.mutate(slot.id)}
                          className="p-1 text-gray-400 hover:text-red-600 rounded"
                          aria-label={`Delete slot ${slot.slotNumber}`}
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-sm text-gray-500 py-4 text-center">
            No slots configured yet. Use &quot;Quick Setup&quot; or &quot;Add Slot&quot; to configure.
          </p>
        )}
      </div>

      {/* Working Days */}
      <WorkingDayToggles
        workingDays={workingDays}
        onUpdate={(data) => updateWorkingDaysMutation.mutate(data)}
      />

      {/* Quick Setup Wizard */}
      <QuickSetupWizard
        open={quickSetupOpen}
        onClose={() => setQuickSetupOpen(false)}
        gridId={numericId}
      />

      {/* Activation Dialog */}
      <GridActivationDialog
        open={activateDialogOpen}
        gridName={grid.name}
        onConfirm={handleActivate}
        onCancel={() => setActivateDialogOpen(false)}
        isLoading={activateMutation.isPending}
      />
    </div>
  );
}

function SlotTypeBadge({ type }) {
  const styles = {
    LECTURE: 'bg-blue-50 text-blue-700',
    TUTORIAL: 'bg-green-50 text-green-700',
    PRACTICAL: 'bg-orange-50 text-orange-700',
    BREAK: 'bg-gray-100 text-gray-600',
    LUNCH: 'bg-yellow-50 text-yellow-700',
  };

  return (
    <span className={`inline-flex px-2 py-0.5 text-xs font-medium rounded-full ${styles[type] || 'bg-gray-100 text-gray-600'}`}>
      {type}
    </span>
  );
}
