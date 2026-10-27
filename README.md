# CRM-PAMS: Form Controls Activity

Integrated Customer Relationship Management and Production Assigning and Monitoring System
for the Cubao branch of CADDSolutions Incorporated. Java SE 17 desktop application (Swing)
with a MySQL 8.0 database, built in Apache NetBeans as a Maven project.

This folder holds the four forms required by the Form Controls activity:

| Form | Type | Module and IPO | Users |
| --- | --- | --- | --- |
| New Order | Major | Customer Order Intake, IPO 5.1 to 5.4 | Operations Manager, Officer-in-Charge |
| Register New Employee | Major | Registration, IPO 11.2 | Operations Manager |
| Search Orders | Supporting | Search, IPO 12.1 to 12.3 | Operations Manager, Officer-in-Charge |
| Production Monitoring (Monitoring Filter) | Supporting | Production Status Monitoring, IPO 9.1 to 9.4 | Operations Manager, Officer-in-Charge |

The Login form (IPO 1.1) uses the same validation and layout but is not one of the four graded forms.

## Setting up on Windows 11

1. Install JDK 17, MySQL 8.0, MySQL Workbench, and Apache NetBeans.
2. In MySQL Workbench, open and run `sql/schema.sql`, then `sql/seed.sql`.
   The seed data is fictional. Every sample account uses the password `Demo2026pass`.
3. Create the restricted application account at the end of `schema.sql`
   (remove the `--` in front of the two lines and set your own password).
4. Copy `db.properties.example` to `db.properties` in the project folder and enter that password.
   `db.properties` is ignored by Git, so the password is never committed.
5. In NetBeans choose File, Open Project, and select this folder. NetBeans reads `pom.xml`
   and downloads the MySQL driver and JUnit 6 the first time.
6. Run the project (F6). Log in as `rvillanueva` (Operations Manager) or `tbautista` (Officer-in-Charge).

## Running the tests

Right-click the project and choose Test (Alt+F6). The tests do not need MySQL: they use an
in-memory copy of the tables (`src/test/java/crmpams/testsupport`).

## Taking the screenshots again

Right-click `src/test/java/crmpams/tools/FormScreenshotTool.java` and choose Run File.
It writes the forms in its normal and error states, to `docs/form-controls/screenshots/`.

## Where things are

| Folder | Contents |
| --- | --- |
| `src/main/java/crmpams/ui/forms` | The five forms |
| `src/main/java/crmpams/ui/components` | Shared form parts: labels, helper text, error display, tab order, placeholders, switch, radio group, date input |
| `src/main/java/crmpams/validation` | Every validation rule (`FieldRules`), used by both the forms and the services |
| `src/main/java/crmpams/service` | Business rules and role checks |
| `src/main/java/crmpams/dao` | Database access with JDBC prepared statements |
| `sql` | Database schema and fictional sample data |
| `docs/form-controls` | Activity documentation, screenshots, demo script, team tasks |
