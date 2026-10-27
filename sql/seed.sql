-- CRM-PAMS sample data for development, testing, screenshots, and the demo video.
-- EVERY NAME, NUMBER, AND ORDER BELOW IS FICTIONAL. Do not load real customer data here.
-- Run after schema.sql. Dates are relative to the day you run the script, so the
-- monitoring view always shows a mix of passed, approaching, and on-schedule deadlines.
--
-- All sample accounts use the password  Demo2026pass  (PBKDF2-HMAC-SHA256, 600,000 rounds).
-- Change or deactivate these accounts before the system is used with real data.

USE crm_pams;

INSERT INTO project_types (project_type_id, type_name) VALUES
  (1, '3D Model'),
  (2, 'Architectural Floor Plan'),
  (3, 'Blueprint Printing'),
  (4, 'Large Format Scanning'),
  (5, 'Site Development Plan'),
  (6, 'Structural Layout'),
  (7, 'Thesis Board Printing');

INSERT INTO urgency_levels (urgency_level_id, level_name, urgency_rank) VALUES
  (1, 'Low', 1),
  (2, 'Normal', 2),
  (3, 'High', 3);

INSERT INTO workflow_stages (stage_id, stage_name, stage_order) VALUES
  (1, 'New', 1),
  (2, 'Assigned', 2),
  (3, 'In Progress', 3),
  (4, 'For Review', 4),
  (5, 'For Customer Confirmation', 5),
  (6, 'Released', 6),
  (7, 'Completed', 7);

INSERT INTO employees (employee_id, first_name, last_name, position) VALUES
  (1, 'Ramon',   'Villanueva',   'Operations Manager'),
  (2, 'Teresa',  'Bautista',     'Officer-in-Charge'),
  (3, 'Paolo',   'Reyes',        'Designer'),
  (4, 'Jasmine', 'Cruz',         'Drafter'),
  (5, 'Mark',    'Delos Santos', 'Modeler'),
  (6, 'Ana',     'Lim',          'Drafter');

INSERT INTO users (user_id, employee_id, username, password_hash, role, is_active, created_by) VALUES
  (1, 1, 'rvillanueva',  'pbkdf2_sha256$600000$eRXGKm4B/yiJwQYnT07j8w==$0gp+rEJiGtN6Zww/jTmKYMB7x2BvQg/2C/95Ueu5j5c=', 'OPERATIONS_MANAGER', 1, NULL),
  (2, 2, 'tbautista',    'pbkdf2_sha256$600000$2LQ0f8CqhYz8v/zTQ6ke3g==$NZTs/TdxtSNCqxvFj4ocZIIIGlPFmqaZUfHgdnr/6B8=', 'OFFICER_IN_CHARGE',  1, 1),
  (3, 3, 'preyes',       'pbkdf2_sha256$600000$yOAtpgvRkX05Autbn7c53w==$FHkvfnM5IM7UJRUOHaO3XSho/O17zqhWNV5kiKESNTg=', 'PRODUCTION_STAFF',   1, 1),
  (4, 4, 'jcruz',        'pbkdf2_sha256$600000$3qpZw6tYh5U4bdrMq7d+PQ==$hefOQiplrWBNZEpgMa9rRwgygp7/rbj+5Go6WlXYzXU=', 'PRODUCTION_STAFF',   1, 1),
  (5, 5, 'mdelossantos', 'pbkdf2_sha256$600000$nZ8OwUeLE8TNg1vXMQ+bAQ==$OMVXtkKvJ3lUNOC8xme3hWtI/YQAdrh5axrCuKllrvE=', 'PRODUCTION_STAFF',   1, 1),
  (6, 6, 'alim',         'pbkdf2_sha256$600000$RfUaGgGxbBnOJqEWDi9xfQ==$gus6U+e/K/62bcfUsoNgmN+oFchejTQ7mW6vEIo7XwA=', 'PRODUCTION_STAFF',   0, 1);

INSERT INTO customers (customer_id, customer_name, contact_number, email) VALUES
  (1, 'Arch. Lea Navarro',          '09171234501', 'lea.navarro@example.com'),
  (2, 'Solid Rock Builders Inc.',   '0281234502',  NULL),
  (3, 'Engr. Dennis Tan',           '09181234503', NULL),
  (4, 'Bayview Construction Corp.', '09191234504', 'projects@example.com'),
  (5, 'Kristine Ocampo',            '09201234505', NULL);

INSERT INTO orders (order_id, customer_id, project_description, deadline, urgency_level_id, delivery_address,
                    stage_id, date_received, encoded_by) VALUES
  (1001, 1, 'Two-storey residential floor plan revision',            DATE_ADD(CURDATE(), INTERVAL 5 DAY),  3, NULL,                                       3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY),  '10:05:00'), 2),
  (1002, 2, 'Structural layout for three-storey commercial building', DATE_SUB(CURDATE(), INTERVAL 1 DAY),  3, '45 Sample Street, Barangay Example, Quezon City', 2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),  '14:20:00'), 2),
  (1003, 3, 'Blueprint printing of approved site development plan',  DATE_ADD(CURDATE(), INTERVAL 2 DAY),  2, NULL,                                       1, TIMESTAMP(CURDATE(), '08:40:00'), 2),
  (1004, 4, '3D model of warehouse extension',                       DATE_ADD(CURDATE(), INTERVAL 12 DAY), 2, NULL,                                       4, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 12 DAY), '09:00:00'), 2),
  (1005, 5, 'Thesis board printing, 4 plates',                       DATE_ADD(CURDATE(), INTERVAL 1 DAY),  3, NULL,                                       5, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY),  '16:45:00'), 2),
  (1006, 1, 'Kitchen renovation layout',                             DATE_SUB(CURDATE(), INTERVAL 10 DAY), 2, NULL,                                       7, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 22 DAY), '11:30:00'), 2),
  (1007, 2, 'As-built plans, 6 sheets',                              DATE_ADD(CURDATE(), INTERVAL 3 DAY),  1, '45 Sample Street, Barangay Example, Quezon City', 6, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 15 DAY), '13:10:00'), 2),
  (1008, 3, 'Site development plan revision 2',                      DATE_ADD(CURDATE(), INTERVAL 8 DAY),  1, NULL,                                       3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),  '15:00:00'), 2);

-- Order lines. Order 1001 has two project types: the floor plan drawing and blueprint copies of it.
INSERT INTO order_items (order_id, line_no, project_type_id, media, sheet_size, sheet_count, set_count) VALUES
  (1001, 1, 2, 'Bond paper',        'A1',         2, 1),
  (1001, 2, 3, 'Blueprint paper',   'A1',         2, 3),
  (1002, 1, 6, 'Bond paper',        '20 x 30 in', 5, 2),
  (1003, 1, 3, 'Blueprint paper',   'A1',         8, 3),
  (1004, 1, 1, 'Digital file only', 'A1',         1, 1),
  (1005, 1, 7, 'Photo paper',       'A1',         4, 1),
  (1006, 1, 2, 'Bond paper',        'A2',         1, 2),
  (1007, 1, 3, 'Blueprint paper',   'A1',         6, 2),
  (1008, 1, 5, 'Vellum',            '20 x 30 in', 3, 1);

INSERT INTO assignments (order_id, employee_id, assigned_by, date_assigned, is_current) VALUES
  (1001, 3, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY),  '11:00:00'), 1),
  (1002, 4, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY),  '15:00:00'), 1),
  (1004, 5, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 12 DAY), '10:00:00'), 1),
  (1005, 3, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY),  '17:00:00'), 1),
  (1006, 4, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 22 DAY), '12:00:00'), 1),
  (1007, 5, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 15 DAY), '14:00:00'), 1),
  (1008, 4, 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY),  '16:00:00'), 1);

INSERT INTO status_history (order_id, stage_id, changed_by, changed_at, field_changed, old_value, new_value)
SELECT o.order_id, o.stage_id, 2, DATE_ADD(o.date_received, INTERVAL 1 HOUR), 'Status', NULL, ws.stage_name
FROM orders o JOIN workflow_stages ws ON ws.stage_id = o.stage_id;
