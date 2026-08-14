package com.utms.masterdata.campus;

import com.utms.common.exception.EntityNotFoundException;
import com.utms.masterdata.batch.Batch;
import com.utms.masterdata.batch.BatchRepository;
import com.utms.masterdata.department.Department;
import com.utms.masterdata.department.DepartmentRepository;
import com.utms.masterdata.program.Program;
import com.utms.masterdata.program.ProgramRepository;
import com.utms.masterdata.section.Section;
import com.utms.masterdata.section.SectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HierarchyService {

    private final CampusRepository campusRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final BatchRepository batchRepository;
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getHierarchyTree(Long campusId) {
        Campus campus = campusRepository.findByIdAndDeletedAtIsNull(campusId)
                .orElseThrow(() -> new EntityNotFoundException("Campus", campusId));

        List<Department> departments = departmentRepository.findAllByCampusIdAndDeletedAtIsNull(campusId);

        List<Map<String, Object>> departmentNodes = departments.stream()
                .map(this::buildDepartmentNode)
                .collect(Collectors.toList());

        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("id", campus.getId());
        tree.put("name", campus.getName());
        tree.put("code", campus.getCode());
        tree.put("departments", departmentNodes);

        return tree;
    }

    private Map<String, Object> buildDepartmentNode(Department department) {
        List<Program> programs = programRepository.findAllByDepartmentIdAndDeletedAtIsNull(department.getId());

        List<Map<String, Object>> programNodes = programs.stream()
                .map(this::buildProgramNode)
                .collect(Collectors.toList());

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", department.getId());
        node.put("name", department.getName());
        node.put("code", department.getCode());
        node.put("programs", programNodes);
        return node;
    }

    private Map<String, Object> buildProgramNode(Program program) {
        List<Batch> batches = batchRepository.findAllByProgramIdAndDeletedAtIsNull(program.getId());

        List<Map<String, Object>> batchNodes = batches.stream()
                .map(this::buildBatchNode)
                .collect(Collectors.toList());

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", program.getId());
        node.put("name", program.getName());
        node.put("code", program.getCode());
        node.put("batches", batchNodes);
        return node;
    }

    private Map<String, Object> buildBatchNode(Batch batch) {
        List<Section> sections = sectionRepository.findAllByBatchIdAndDeletedAtIsNull(batch.getId());

        List<Map<String, Object>> sectionNodes = sections.stream()
                .map(s -> {
                    Map<String, Object> sNode = new LinkedHashMap<>();
                    sNode.put("id", s.getId());
                    sNode.put("name", s.getName());
                    sNode.put("strength", s.getStrength());
                    return sNode;
                })
                .collect(Collectors.toList());

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", batch.getId());
        node.put("name", batch.getName());
        node.put("academicYear", batch.getAcademicYear());
        node.put("sections", sectionNodes);
        return node;
    }
}
