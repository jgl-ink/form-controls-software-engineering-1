# Team Tasks and Git Workflow

The activity requires commits from every group member. This draft was prepared outside the
repository, so no commits were made for anyone. Each member should add their own part from
their own GitHub account, so the history shows real authorship.

The split below is a suggestion. Change the names as the group agrees.

| Member | Branch | Files to add and commit |
| --- | --- | --- |
| Lizardo, John Gideon M. | `feature/form-validation-core` | `pom.xml`, `.gitignore`, `db.properties.example`, `sql/`, `src/main/java/crmpams/model`, `validation`, `security`, `db`, `dao`, `ui/components`, `ui/UiTheme.java`, `ui/Dialogs.java`, `ui/UiTasks.java`, tests in `validation`, `security`, `db`, `model`, `testsupport` |
| Mendoza, Andrea Maxine C. | `feature/new-order-form` | `service/OrderIntakeService.java`, `SaveOrderResult.java`, `OrderFormData.java`, `ui/forms/NewOrderForm.java`, `OrderIntakeServiceTest.java` |
| Nool, Neil Jethro M. | `feature/register-employee-form` | `service/RegistrationService.java`, `SaveResult.java`, `AuthService.java`, `LoginResult.java`, `ui/forms/RegisterEmployeeForm.java`, `ui/forms/LoginForm.java`, `RegistrationServiceTest.java`, `AuthServiceTest.java` |
| Uy, Lenard Niño L. | `feature/search-orders-form` | `service/SearchService.java`, `SearchOptions.java`, `SearchResult.java`, `ui/forms/SearchOrdersForm.java` |
| Valdez, Ira Nicole L. | `feature/monitoring-filter-form` | `service/MonitoringService.java`, `MonitoringOptions.java`, `MonitoringResult.java`, `ui/forms/MonitoringFilterForm.java`, `SearchAndMonitoringServiceTest.java` |

After the four form branches are merged, one member adds `App.java`, `AppContext.java`,
`ui/MainWindow.java`, `FormBehaviorTest.java`, `FormDriver.java`, `FormScreenshotTool.java`,
`README.md`, and `docs/form-controls/` on a `feature/form-controls-docs` branch.

## Order of merging

1. `feature/form-validation-core` first, because every form depends on it.
2. The New Order and Register Employee branches in any order.
3. Search Orders before Production Monitoring, because the monitoring form reuses the table
   set-up of the search form.
4. `feature/form-controls-docs` last.

## Commands for each member (PowerShell)

```powershell
git checkout main
git pull
git checkout -b feature/new-order-form          # use your own branch name
# copy your files from the draft into the same paths in the repository
git add src/main/java/crmpams/service/OrderIntakeService.java src/main/java/crmpams/ui/forms/NewOrderForm.java
git commit -m "Add New Order form with validation (Customer Order Intake, IPO 5.1 to 5.4)"
git push -u origin feature/new-order-form
gh pr create --fill
```

Before committing, check that `git config user.name` and `git config user.email` show your
own name and the email of your GitHub account.

## Review checklist for each pull request

- The project builds and every test passes in NetBeans (Alt+F6).
- The form opens from the main window and Tab moves through the fields from top to bottom.
- Submitting empty or wrong values shows the error summary and saves nothing.
- Submitting correct values saves the record in MySQL (check it in MySQL Workbench).
