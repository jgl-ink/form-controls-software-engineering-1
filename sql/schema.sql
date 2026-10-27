-- CRM-PAMS database schema (MySQL 8.0)
-- Integrated Customer Relationship Management and Production Assigning and Monitoring System
-- CADDSolutions Incorporated, Cubao branch
--
-- The tables below were rebuilt from the written descriptions in Chapters 1 to 3
-- (module descriptions, flowchart and IPO descriptions, system architecture).
-- Compare every table and column with Figure 89 (Entity Relationship Diagram) and adjust
-- this file if the ERD uses different names or extra fields.
--
-- order_items follows the company's order form (System Revised Order Form): one order (J.O.)
-- can have several lines, each with its own project type, media, size, number of sheets, and
-- number of sets. Prices are not stored, because the system does not compute prices
-- (Chapter 1, Delimitations). This adds a twelfth table to the eleven in the ERD.

CREATE DATABASE IF NOT EXISTS crm_pams
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_as_ci;   -- case-insensitive, accent-sensitive (Pena and Peña stay different)

USE crm_pams;

-- ---------------------------------------------------------------------------
-- Reference data (managed in the Maintenance module, 13.3)
-- ---------------------------------------------------------------------------

CREATE TABLE project_types (
  project_type_id INT AUTO_INCREMENT PRIMARY KEY,
  type_name       VARCHAR(60)  NOT NULL UNIQUE,
  is_active       TINYINT(1)   NOT NULL DEFAULT 1
);

CREATE TABLE urgency_levels (
  urgency_level_id INT AUTO_INCREMENT PRIMARY KEY,
  level_name       VARCHAR(30) NOT NULL UNIQUE,
  urgency_rank     INT         NOT NULL,          -- higher rank = more urgent (Chapter 3, Algorithm Implementation)
  is_active        TINYINT(1)  NOT NULL DEFAULT 1
);

CREATE TABLE workflow_stages (
  stage_id    INT AUTO_INCREMENT PRIMARY KEY,
  stage_name  VARCHAR(40) NOT NULL UNIQUE,
  stage_order INT         NOT NULL UNIQUE,         -- New = 1 ... Completed = 7
  is_active   TINYINT(1)  NOT NULL DEFAULT 1
);

-- ---------------------------------------------------------------------------
-- People and accounts (Registration module, 11.0; Security module, 1.0)
-- ---------------------------------------------------------------------------

CREATE TABLE employees (
  employee_id    INT AUTO_INCREMENT PRIMARY KEY,
  first_name     VARCHAR(50)  NOT NULL,
  last_name      VARCHAR(50)  NOT NULL,
  position       VARCHAR(40)  NOT NULL,            -- Designer, Drafter, Modeler, Operations Manager, Officer-in-Charge
  contact_number VARCHAR(13)  NULL,
  email          VARCHAR(100) NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
  user_id       INT AUTO_INCREMENT PRIMARY KEY,
  employee_id   INT          NOT NULL UNIQUE,
  username      VARCHAR(30)  NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,             -- pbkdf2_sha256$600000$<salt>$<hash>
  role          ENUM('OPERATIONS_MANAGER', 'OFFICER_IN_CHARGE', 'PRODUCTION_STAFF') NOT NULL,
  is_active     TINYINT(1)   NOT NULL DEFAULT 1,   -- deactivation keeps the record (11.3)
  created_by    INT          NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_employee   FOREIGN KEY (employee_id) REFERENCES employees (employee_id),
  CONSTRAINT fk_users_created_by FOREIGN KEY (created_by)  REFERENCES users (user_id)
);

-- ---------------------------------------------------------------------------
-- Customers and orders (Customer Order Intake 5.0, Centralized Order Logging 6.0)
-- ---------------------------------------------------------------------------

CREATE TABLE customers (
  customer_id    INT AUTO_INCREMENT PRIMARY KEY,
  customer_name  VARCHAR(100) NOT NULL,
  contact_number VARCHAR(13)  NOT NULL,            -- stored as 09XXXXXXXXX or 02XXXXXXXX
  email          VARCHAR(100) NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_customer_name_contact UNIQUE (customer_name, contact_number)
);

CREATE TABLE orders (
  order_id             INT AUTO_INCREMENT PRIMARY KEY,  -- the J.O. number
  customer_id          INT          NOT NULL,
  project_description  VARCHAR(500) NOT NULL,
  deadline             DATE         NOT NULL,
  urgency_level_id     INT          NOT NULL,
  delivery_address     VARCHAR(255) NULL,               -- blank when the customer picks up the order
  special_instructions VARCHAR(500) NULL,
  stage_id             INT          NOT NULL,
  date_received        DATETIME     NOT NULL,       -- set by the system when the order is saved
  encoded_by           INT          NOT NULL,
  updated_at           DATETIME     NULL,
  CONSTRAINT fk_orders_customer     FOREIGN KEY (customer_id)      REFERENCES customers (customer_id),
  CONSTRAINT fk_orders_urgency      FOREIGN KEY (urgency_level_id) REFERENCES urgency_levels (urgency_level_id),
  CONSTRAINT fk_orders_stage        FOREIGN KEY (stage_id)         REFERENCES workflow_stages (stage_id),
  CONSTRAINT fk_orders_encoded_by   FOREIGN KEY (encoded_by)       REFERENCES users (user_id),
  INDEX idx_orders_stage (stage_id),
  INDEX idx_orders_deadline (deadline),
  INDEX idx_orders_date_received (date_received)
);

-- Order lines (columns of the company's order form). Total no. of sheets = sheet_count x set_count,
-- computed when shown instead of stored, so it can never disagree with the two counts.
CREATE TABLE order_items (
  order_item_id   INT AUTO_INCREMENT PRIMARY KEY,
  order_id        INT         NOT NULL,
  line_no         INT         NOT NULL,                 -- 1, 2, 3 ... in the order they were entered
  project_type_id INT         NOT NULL,
  media           VARCHAR(40) NOT NULL,                 -- for example Bond paper, Blueprint paper, Vellum
  sheet_size      VARCHAR(30) NOT NULL,                 -- for example A1, 20 x 30 in
  sheet_count     INT         NOT NULL,                 -- NO. OF SHEET
  set_count       INT         NOT NULL,                 -- NO. OF SET
  CONSTRAINT fk_order_items_order        FOREIGN KEY (order_id)        REFERENCES orders (order_id),
  CONSTRAINT fk_order_items_project_type FOREIGN KEY (project_type_id) REFERENCES project_types (project_type_id),
  CONSTRAINT uq_order_items_line UNIQUE (order_id, line_no),
  CONSTRAINT ck_order_items_sheets CHECK (sheet_count BETWEEN 1 AND 999),
  CONSTRAINT ck_order_items_sets   CHECK (set_count BETWEEN 1 AND 99),
  INDEX idx_order_items_type (project_type_id)
);

-- Reference file attachment: only the file name and its network location are stored.
-- The system never copies, opens, or previews the file (Chapter 1, Delimitations).
CREATE TABLE reference_files (
  file_id       INT AUTO_INCREMENT PRIMARY KEY,
  order_id      INT          NOT NULL,
  file_name     VARCHAR(255) NOT NULL,
  file_location VARCHAR(500) NOT NULL,
  added_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_reference_files_order FOREIGN KEY (order_id) REFERENCES orders (order_id)
);

-- ---------------------------------------------------------------------------
-- Task Assignment (7.0)
-- ---------------------------------------------------------------------------

CREATE TABLE assignments (
  assignment_id INT AUTO_INCREMENT PRIMARY KEY,
  order_id      INT        NOT NULL,
  employee_id   INT        NOT NULL,
  assigned_by   INT        NOT NULL,
  date_assigned DATETIME   NOT NULL,
  is_current    TINYINT(1) NOT NULL DEFAULT 1,       -- a reassignment sets the old row to 0
  CONSTRAINT fk_assignments_order       FOREIGN KEY (order_id)    REFERENCES orders (order_id),
  CONSTRAINT fk_assignments_employee    FOREIGN KEY (employee_id) REFERENCES employees (employee_id),
  CONSTRAINT fk_assignments_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (user_id),
  INDEX idx_assignments_current (order_id, is_current)
);

-- ---------------------------------------------------------------------------
-- History and audit (6.2 View Order Change History, activity log)
-- ---------------------------------------------------------------------------

CREATE TABLE status_history (
  history_id    INT AUTO_INCREMENT PRIMARY KEY,
  order_id      INT          NOT NULL,
  stage_id      INT          NULL,
  changed_by    INT          NOT NULL,
  changed_at    DATETIME     NOT NULL,
  field_changed VARCHAR(60)  NOT NULL,
  old_value     VARCHAR(500) NULL,
  new_value     VARCHAR(500) NULL,
  CONSTRAINT fk_status_history_order FOREIGN KEY (order_id)   REFERENCES orders (order_id),
  CONSTRAINT fk_status_history_stage FOREIGN KEY (stage_id)   REFERENCES workflow_stages (stage_id),
  CONSTRAINT fk_status_history_user  FOREIGN KEY (changed_by) REFERENCES users (user_id),
  INDEX idx_status_history_order (order_id, changed_at)
);

CREATE TABLE activity_log (
  log_id    INT AUTO_INCREMENT PRIMARY KEY,
  user_id   INT          NULL,                     -- NULL for a failed login with an unknown username
  action    VARCHAR(60)  NOT NULL,
  details   VARCHAR(255) NULL,
  logged_at DATETIME     NOT NULL,
  CONSTRAINT fk_activity_log_user FOREIGN KEY (user_id) REFERENCES users (user_id),
  INDEX idx_activity_log_time (logged_at)
);

-- ---------------------------------------------------------------------------
-- Restricted application account (Chapter 3, System Architecture).
-- Change the password before running. The account can read and change data only.
-- ---------------------------------------------------------------------------
-- CREATE USER 'crm_app'@'%' IDENTIFIED BY 'change-this-password';
-- GRANT SELECT, INSERT, UPDATE ON crm_pams.* TO 'crm_app'@'%';
