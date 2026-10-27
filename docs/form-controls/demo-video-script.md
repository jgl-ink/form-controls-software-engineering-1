# Demo Video Script (about 3 minutes 30 seconds)

Record the screen of a Windows 11 computer with MySQL running and `sql/seed.sql` loaded.
Use the sample accounts only. Every name and number in the sample data is fictional.
Keep MySQL Workbench open in a second window to show that the data was saved.

| Time | On screen | What to say |
| --- | --- | --- |
| 0:00 to 0:15 | Login form. Click Log In with both fields empty. | "This is CRM-PAMS for the Cubao branch of CADDSolutions. Every input has a label, and empty required fields are blocked with a message under the field and a summary at the top." |
| 0:15 to 0:25 | Log in as `rvillanueva` / `Demo2026pass`. Main window opens. | "The Operations Manager sees all four forms. The menu depends on the role." |
| 0:25 to 1:10 | New Order. Type `J` as the name, `0917123` as the contact number, `juan@mail` as the email, set the deadline to yesterday, then press Enter. | "Our first major form records an order once. Wrong formats, a past deadline, and missing selections are all reported, and focus moves to the first problem. Nothing is saved." |
| | Fix the fields: Juan Dela Cruz, 09171112222, project type, description, deadline, urgency High. Click Browse Reference File and pick a file. Press Enter. | "Contact numbers accept 09 or +639 mobiles and 02 landlines. For the reference file we store only the name and the network location. Saving creates the customer, the order, and its history in one transaction." |
| | Show the "Order saved" message, then the new row in MySQL Workbench (`SELECT * FROM orders ORDER BY order_id DESC LIMIT 1;`). | "Here is the new order in the database with the status New." |
| 1:10 to 1:30 | New Order again with Arch. Lea Navarro, 09171234501, and the description "Two-storey residential floor plan revision". Save. | "If the same customer already has an open order with the same description, the system warns before saving a duplicate." |
| 1:30 to 2:15 | Register Employee. Type `Ma1`, a short password, and a different confirmation. Submit. | "The second major form creates accounts. It checks names, username rules, password length, and that both passwords match." |
| | Fill it correctly: Maria Santos, Production Staff, Drafter, `msantos`, `Drafts2026`. Turn the Account status switch on and off with Space. Register. | "Position is only enabled for Production Staff. The password is saved as a PBKDF2 hash, never as plain text." Show the `users` row in Workbench. |
| 2:15 to 2:50 | Search Orders. Type `plan`, press Enter. Then pick status Completed, tick Filter by date received, set the end date before the start date, and search. | "The search form builds one parameterized query. It also blocks a date range that ends before it starts." Select a row and press Enter to open the order. |
| 2:50 to 3:25 | Production Monitoring. Show all active orders, then move the warning period slider to 7 with the arrow keys and choose Approaching Deadline. Then untick all stages and apply. | "Each active order is marked Passed Deadline, Approaching Deadline, or On Schedule, as text and not only by color. At least one stage must be ticked." |
| 3:25 to 3:35 | Press Tab through one form without the mouse. | "Every form works with the keyboard: Tab order follows the screen, Alt plus the underlined letter jumps to a field, Enter submits, and Escape cancels or resets." |
