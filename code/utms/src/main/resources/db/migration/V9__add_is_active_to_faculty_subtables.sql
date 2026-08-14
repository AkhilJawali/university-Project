-- V9: Add missing is_active column to faculty sub-tables, workload_configs, and resource_blocks
-- Fix: BaseEntity requires is_active on all entity tables

ALTER TABLE utms.faculty_availability_windows ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE utms.faculty_competencies ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE utms.faculty_campus_associations ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE utms.workload_configs ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE utms.resource_blocks ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
