import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCampuses, useCampusHierarchy } from '../campus/hooks';
import { TreeNode } from './TreeNode';
import { LoadingSkeleton } from '@/components/LoadingSkeleton';
import { ErrorState } from '@/components/ErrorState';
import { Building2, GraduationCap, Users, BookOpen, Layers } from 'lucide-react';

export default function HierarchyTreePage() {
  const navigate = useNavigate();
  const [selectedCampusId, setSelectedCampusId] = useState(undefined);

  const { data: campuses, isLoading: campusesLoading, isError: campusesError, refetch } = useCampuses({
    page: 0,
    size: 100,
  });

  const { data: hierarchy, isLoading: hierarchyLoading } = useCampusHierarchy(selectedCampusId);

  if (campusesError) {
    return <ErrorState message="Failed to load campus data" onRetry={refetch} />;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-gray-900">Campus Hierarchy</h1>

      {/* Campus selector */}
      <div>
        <label htmlFor="campus-select" className="block text-sm font-medium text-gray-700 mb-1">
          Select Campus
        </label>
        <select
          id="campus-select"
          value={selectedCampusId ?? ''}
          onChange={(e) => setSelectedCampusId(e.target.value ? Number(e.target.value) : undefined)}
          className="w-full max-w-sm px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">Choose a campus...</option>
          {campuses?.data?.map((campus) => (
            <option key={campus.id} value={campus.id}>
              {campus.name} ({campus.code})
            </option>
          ))}
        </select>
      </div>

      {/* Loading states */}
      {campusesLoading && <LoadingSkeleton rows={5} columns={1} />}
      {selectedCampusId && hierarchyLoading && <LoadingSkeleton rows={8} columns={1} />}

      {/* Hierarchy tree */}
      {hierarchy?.data && (
        <div className="bg-white border border-gray-200 rounded-lg p-4">
          <TreeNode
            label={`${hierarchy.data.name} (${hierarchy.data.code})`}
            icon={<Building2 className="w-4 h-4" />}
            level={0}
            defaultExpanded
            onClick={() => navigate(`/admin/master-data/campuses/${hierarchy.data.id}`)}
          >
            {hierarchy.data.departments.map((dept) => (
              <TreeNode
                key={dept.id}
                label={`${dept.name} (${dept.code})`}
                icon={<Layers className="w-4 h-4" />}
                level={1}
                onClick={() => navigate(`/admin/master-data/campuses/${hierarchy.data.id}/departments/${dept.id}`)}
              >
                {dept.programs.map((program) => (
                  <TreeNode
                    key={program.id}
                    label={`${program.name} (${program.code})`}
                    icon={<GraduationCap className="w-4 h-4" />}
                    level={2}
                    onClick={() => navigate(`/admin/master-data/departments/${dept.id}/programs/${program.id}`)}
                  >
                    {program.batches.map((batch) => (
                      <TreeNode
                        key={batch.id}
                        label={`${batch.name} — ${batch.academicYear}`}
                        icon={<Users className="w-4 h-4" />}
                        level={3}
                        onClick={() => navigate(`/admin/master-data/programs/${program.id}/batches/${batch.id}`)}
                      >
                        {batch.sections.map((section) => (
                          <TreeNode
                            key={section.id}
                            label={`${section.name} (Str: ${section.strength})`}
                            icon={<BookOpen className="w-4 h-4" />}
                            level={4}
                            onClick={() => navigate(`/admin/master-data/batches/${batch.id}/sections/${section.id}`)}
                          />
                        ))}
                      </TreeNode>
                    ))}
                  </TreeNode>
                ))}
              </TreeNode>
            ))}
          </TreeNode>
        </div>
      )}

      {/* No selection state */}
      {!selectedCampusId && !campusesLoading && (
        <div className="bg-white border border-gray-200 rounded-lg p-12 text-center text-gray-500">
          <Building2 className="w-12 h-12 mx-auto mb-4 text-gray-300" />
          <p className="text-sm">Select a campus above to view its hierarchy tree.</p>
        </div>
      )}
    </div>
  );
}
