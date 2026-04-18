This Spring Boot application uses both MVC and REST controllers. Thymeleaf templates are used for the Admin and Doctor dashboards, while REST APIs serve all other modules. The application interacts with two databases—MySQL (for patient, doctor, appointment, and admin data) and MongoDB (for prescriptions). All controllers route requests through a common service layer, which in turn delegates to the appropriate repositories. MySQL uses JPA entities while MongoDB uses document models.

1. A user opens either the Admin/Doctor dashboard pages or a REST-driven module such as appointments or patient features.
2. Admin and Doctor page requests are handled by Thymeleaf MVC controllers, while API requests are handled by REST controllers in the same Spring Boot app.
3. Both controller types delegate all business operations to the shared service layer instead of accessing databases directly.
4. The service layer applies validation and business rules, then routes persistence work to the appropriate repository based on the data domain.
5. For operational data (Admin, Doctor, Patient, Appointment), services call Spring Data JPA repositories mapped to MySQL entities.
6. For prescription data, services call the MongoDB repository, which persists and retrieves Prescription documents from MongoDB.
7. Results flow back from repositories to services, then to controllers, which return either rendered Thymeleaf pages or JSON responses to the UI.
