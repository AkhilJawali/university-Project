-- Seed data for local development
-- Run manually: copy contents and execute in pgAdmin or psql

INSERT INTO utms.campuses (name, code, address, city, state, timezone, is_active, created_by, updated_by) VALUES
('Main Campus', 'MAIN', '123 University Road', 'Belgaum', 'Karnataka', 'Asia/Kolkata', true, 'admin', 'admin'),
('North Campus', 'NORTH', '456 North Avenue', 'Hubli', 'Karnataka', 'Asia/Kolkata', true, 'admin', 'admin'),
('South Campus', 'SOUTH', '789 South Street', 'Dharwad', 'Karnataka', 'Asia/Kolkata', true, 'admin', 'admin'),
('Tech Park Campus', 'TECH', '101 Innovation Drive', 'Bangalore', 'Karnataka', 'Asia/Kolkata', true, 'admin', 'admin'),
('Medical Campus', 'MED', '202 Health Lane', 'Belgaum', 'Karnataka', 'Asia/Kolkata', true, 'admin', 'admin');

INSERT INTO utms.departments (name, code, campus_id, is_active, created_by, updated_by) VALUES
('Computer Science & Engineering', 'CSE', (SELECT id FROM utms.campuses WHERE code = 'MAIN'), true, 'admin', 'admin'),
('Electronics & Communication', 'ECE', (SELECT id FROM utms.campuses WHERE code = 'MAIN'), true, 'admin', 'admin'),
('Mechanical Engineering', 'MECH', (SELECT id FROM utms.campuses WHERE code = 'MAIN'), true, 'admin', 'admin'),
('Civil Engineering', 'CIVIL', (SELECT id FROM utms.campuses WHERE code = 'MAIN'), true, 'admin', 'admin'),
('Information Science', 'ISE', (SELECT id FROM utms.campuses WHERE code = 'NORTH'), true, 'admin', 'admin'),
('Electrical Engineering', 'EEE', (SELECT id FROM utms.campuses WHERE code = 'NORTH'), true, 'admin', 'admin');

INSERT INTO utms.programs (name, code, department_id, duration_years, total_semesters, degree_type, is_active, created_by, updated_by) VALUES
('B.Tech Computer Science', 'BTCS', (SELECT id FROM utms.departments WHERE code = 'CSE'), 4, 8, 'UG', true, 'admin', 'admin'),
('M.Tech Computer Science', 'MTCS', (SELECT id FROM utms.departments WHERE code = 'CSE'), 2, 4, 'PG', true, 'admin', 'admin'),
('B.Tech Electronics', 'BTEC', (SELECT id FROM utms.departments WHERE code = 'ECE'), 4, 8, 'UG', true, 'admin', 'admin'),
('B.Tech Mechanical', 'BTME', (SELECT id FROM utms.departments WHERE code = 'MECH'), 4, 8, 'UG', true, 'admin', 'admin'),
('B.Tech Civil', 'BTCE', (SELECT id FROM utms.departments WHERE code = 'CIVIL'), 4, 8, 'UG', true, 'admin', 'admin');

INSERT INTO utms.batches (name, program_id, academic_year, semester_number, strength, is_active, created_by, updated_by) VALUES
('2024-28 Batch', (SELECT id FROM utms.programs WHERE code = 'BTCS'), '2024-2028', 1, 120, true, 'admin', 'admin'),
('2023-27 Batch', (SELECT id FROM utms.programs WHERE code = 'BTCS'), '2023-2027', 3, 120, true, 'admin', 'admin'),
('2024-28 Batch', (SELECT id FROM utms.programs WHERE code = 'MTCS'), '2024-2028', 1, 60, true, 'admin', 'admin'),
('2024-28 Batch', (SELECT id FROM utms.programs WHERE code = 'BTEC'), '2024-2028', 1, 90, true, 'admin', 'admin');

INSERT INTO utms.sections (name, batch_id, strength, is_active, created_by, updated_by) VALUES
('Section A', (SELECT id FROM utms.batches WHERE name = '2024-28 Batch' AND program_id = (SELECT id FROM utms.programs WHERE code = 'BTCS')), 60, true, 'admin', 'admin'),
('Section B', (SELECT id FROM utms.batches WHERE name = '2024-28 Batch' AND program_id = (SELECT id FROM utms.programs WHERE code = 'BTCS')), 60, true, 'admin', 'admin'),
('Section A', (SELECT id FROM utms.batches WHERE name = '2023-27 Batch' AND program_id = (SELECT id FROM utms.programs WHERE code = 'BTCS')), 60, true, 'admin', 'admin'),
('Section B', (SELECT id FROM utms.batches WHERE name = '2023-27 Batch' AND program_id = (SELECT id FROM utms.programs WHERE code = 'BTCS')), 60, true, 'admin', 'admin'),
('Section A', (SELECT id FROM utms.batches WHERE name = '2024-28 Batch' AND program_id = (SELECT id FROM utms.programs WHERE code = 'MTCS')), 30, true, 'admin', 'admin'),
('Section B', (SELECT id FROM utms.batches WHERE name = '2024-28 Batch' AND program_id = (SELECT id FROM utms.programs WHERE code = 'MTCS')), 30, true, 'admin', 'admin');
