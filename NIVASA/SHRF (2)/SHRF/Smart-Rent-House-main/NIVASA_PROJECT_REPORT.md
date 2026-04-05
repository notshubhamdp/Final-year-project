# NIVASA Project Report
## Smart Rent House - Professional System Overview

**Project Name:** NIVASA  
**Project Type:** Full-stack web application for rental property management  
**Prepared On:** April 5, 2026  
**Platform Category:** House rental and property management system  

## 1. Executive Summary
NIVASA is a web-based rental management platform designed to simplify the interaction between tenants, landlords, and administrators. The system addresses common rental market problems such as unreliable property information, weak communication between stakeholders, manual record handling, poor payment visibility, and limited trust in online listings.

The application provides a centralized platform where landlords can list and manage properties, tenants can search and book verified homes, and administrators can supervise approvals, user activity, and system quality. The project is implemented using Spring Boot, Thymeleaf, Spring Security, JPA, MySQL, and supporting services for email, payments, reminders, reviews, messaging, and visit scheduling.

## 2. Problem Statement
The residential rental process is often fragmented, slow, and difficult to trust. Tenants regularly face incomplete property information, fake or unverified listings, delayed communication, and limited transparency during booking and payment. At the same time, landlords struggle with tenant screening, listing visibility, document handling, and follow-up coordination. Traditional rental workflows are often manual, error-prone, and dependent on disconnected communication channels.

These challenges create operational inefficiency and reduce confidence for all participants in the rental ecosystem. A digital solution is required to bring listing management, verification, communication, booking, and payment tracking into a single secure environment.

## 3. Proposed Solution
NIVASA proposes an integrated rental platform that supports the full property journey from listing creation to booking confirmation and payment tracking. The system is designed around three primary actors:

- `Tenant`: searches properties, saves favorites, contacts landlords, books homes, pays deposits or rent, and tracks payment history.
- `Landlord`: uploads and manages properties, submits verification documents, responds to tenant interest, tracks bookings, and manages wallet or payout activity.
- `Admin`: reviews and verifies properties, monitors users, supervises operational flows, and supports platform governance.

The platform improves trust and efficiency by combining verified listings, secure authentication, structured dashboards, messaging, visit scheduling, email-based flows, and online payment support.

## 4. Objectives
The major objectives of the NIVASA project are:

- To provide a trusted digital platform for rental property discovery and management.
- To reduce fake or misleading listings through an approval and verification workflow.
- To simplify communication between tenants and landlords using in-platform messaging.
- To support online payment handling and improve payment transparency.
- To help landlords manage listings, tenant requests, and booking-related actions efficiently.
- To enable administrators to supervise properties, users, and verification processes.
- To automate routine reminders such as visits, rent schedules, and payment lifecycle events.

## 5. Need for the System
The project is relevant because rental housing decisions require clarity, trust, and speed. Existing rental processes frequently depend on:

- Informal communication channels
- Inconsistent documentation
- Delayed responses
- Weak verification practices
- Poor follow-up after initial contact
- Limited visibility into booking and payment status

NIVASA addresses these weaknesses by introducing a single system with defined roles, workflows, and records.

## 6. Scope of the Project
The current system covers the following scope:

- User registration, login, role selection, and authentication
- Tenant and landlord dashboards
- Property creation, property detail management, and media uploads
- Property verification and approval management
- Favorite property management
- Contact and messaging between tenant and landlord
- Booking flow with advance payment support
- Rent and payment tracking
- Visit scheduling and visit reminders
- Password reset and token-based flows
- User reviews and rating capture
- FAQ, contact, privacy policy, and terms pages
- Optional chatbot support configuration

The project is suitable for real-world extension into a campus project, prototype deployment, or production-scale enhancement.

## 7. Key Stakeholders and User Roles
### Tenant
The tenant uses the platform to browse approved properties, save favorites, contact landlords, schedule visits, book a home, and track payments or approvals.

### Landlord
The landlord uses the system to upload properties, submit documents, maintain location and media details, respond to tenants, manage bookings, and review wallet or payout information.

### Administrator
The administrator acts as the control authority of the system. This role verifies property submissions, reviews pending records, monitors banned users, and maintains the trustworthiness of the platform.

## 8. Major Functional Modules
Based on the current codebase, the platform contains the following major modules:

### 8.1 Authentication and User Management
- Registration and login flows
- Role selection
- Profile management
- Password reset support
- Token handling
- Role-based access control using Spring Security

### 8.2 Property Management
- Property listing creation and editing
- Property images and document upload
- Property detail display
- Landlord property dashboards
- Property map and location support
- Availability and verification status management

### 8.3 Tenant Experience Module
- Browse approved and available properties
- View property details
- Save favorite properties
- Contact landlords
- Book properties
- Track booked properties and payment history

### 8.4 Landlord Operations Module
- Manage listed properties
- Upload verification assets
- View booking notifications
- Approve pending booking requests
- Track earnings and wallet history

### 8.5 Payment and Billing Module
- Payment checkout and payment status tracking
- Support for advance, rent, and deposit payment categories
- Stripe integration for payment processing
- Receipt handling and payment lifecycle services
- Automated payout and refund-related scheduling logic

### 8.6 Messaging and Communication Module
- Conversation management between users
- Property-linked message threads
- Contact landlord flow
- FAQ and support contact integration
- Email support for notification-driven flows

### 8.7 Visit Scheduling and Reminder Module
- Visit scheduling management
- Reminder services for property visits
- Time-based automation using configured cron schedules

### 8.8 Review and Feedback Module
- App review submission
- Rating capture
- Home page review presentation
- Feedback visibility for system trust building

### 8.9 Admin Monitoring and Governance Module
- Admin dashboard
- Pending property review
- Student verification support
- Banned user management
- Oversight of system records

### 8.10 Chatbot and Knowledge Support Module
- Chatbot controller and service layer
- FAQ-backed support logic
- Optional OpenAI configuration in application settings

## 9. System Architecture Overview
NIVASA follows a layered architecture consistent with Spring Boot MVC design.

### 9.1 Architectural Layers
- `Presentation Layer`: Thymeleaf templates, HTML, CSS, and JavaScript
- `Controller Layer`: request handling and route management
- `Service Layer`: business rules, scheduling, email, payment, and chatbot logic
- `Repository Layer`: JPA repositories for persistence operations
- `Database Layer`: MySQL database for persistent storage

### 9.2 Simplified Architecture Flow
```text
User Interface
    ->
Spring MVC Controllers
    ->
Service Layer
    ->
JPA Repositories
    ->
MySQL Database
```

This layered approach improves maintainability, testability, and separation of responsibilities.

### 9.3 Project Architecture Diagram
The following text diagram can be directly used in a report or presentation to explain the architecture of the system:

```text
                    +--------------------+
                    |      TENANT        |
                    +--------------------+
                              |
                    +--------------------+
                    |     LANDLORD       |
                    +--------------------+
                              |
                    +--------------------+
                    |       ADMIN        |
                    +--------------------+
                              |
                              v
     +------------------------------------------------------+
     |      PRESENTATION LAYER / WEB INTERFACE              |
     |  Thymeleaf Templates + HTML + CSS + JavaScript       |
     +------------------------------------------------------+
                              |
                              v
     +------------------------------------------------------+
     |              CONTROLLER LAYER                        |
     | HomeController, PaymentController,                   |
     | MessageController, LandlordPropertyController,       |
     | AdminDashboardController, TenantController           |
     +------------------------------------------------------+
                              |
                              v
     +------------------------------------------------------+
     |                SERVICE LAYER                         |
     | PropertyService, PaymentService, MessageService,     |
     | EmailService, VisitScheduleService,                  |
     | StripeConnectService, ChatbotService                 |
     +------------------------------------------------------+
                              |
                              v
     +------------------------------------------------------+
     |              REPOSITORY / DATA ACCESS                |
     | UserRepository, PropertyRepository,                  |
     | PaymentRepository, AppReviewRepository,              |
     | MessageRepository, VisitScheduleRepository           |
     +------------------------------------------------------+
                              |
                              v
     +------------------------------------------------------+
     |                  MYSQL DATABASE                      |
     | User, Property, Payment, Message, Review, Token,    |
     | VisitSchedule, Chatbot-related tables               |
     +------------------------------------------------------+
                              |
          +-------------------+-------------------+
          |                   |                   |
          v                   v                   v
   +-------------+     +-------------+     +-------------+
   |   STRIPE    |     | SMTP EMAIL  |     | FILE/UPLOAD |
   |  PAYMENTS   |     | NOTIFICATIONS|    |   STORAGE   |
   +-------------+     +-------------+     +-------------+
```

### 9.4 Package and Project Structure
The internal structure of the project follows a standard modular Spring Boot arrangement:

```text
Smart-Rent-House-main/
|
|-- src/
|   |-- main/
|   |   |-- java/com/SRHF/SRHF/
|   |   |   |-- config/
|   |   |   |-- controller/
|   |   |   |-- entity/
|   |   |   |-- repository/
|   |   |   |-- service/
|   |   |   |-- util/
|   |   |   |-- SrhfApplication.java
|   |   |
|   |   |-- resources/
|   |       |-- static/
|   |       |   |-- css/
|   |       |   |-- js/
|   |       |   |-- images/
|   |       |
|   |       |-- templates/
|   |       |-- application.yml
|   |
|   |-- test/
|       |-- java/com/SRHF/SRHF/
|
|-- uploads/
|-- pom.xml
|-- mvnw / mvnw.cmd
|-- documentation files
```

### 9.5 Explanation of the Package Structure
- `config/`: security configuration, data initialization, exception handling, and web configuration
- `controller/`: URL mapping and request-response handling for user-facing features
- `entity/`: domain objects mapped to database tables
- `repository/`: data access interfaces using Spring Data JPA
- `service/`: business logic, workflows, scheduling, messaging, and payment support
- `util/`: helper classes such as validators, file utilities, and ID generation logic
- `templates/`: Thymeleaf HTML pages for home, dashboards, property views, payments, login, and support pages
- `static/`: CSS, JavaScript, and image assets used by the web interface
- `application.yml`: central configuration for database, mail, Stripe, cron jobs, and server settings

### 9.6 Request Processing Flow
The request flow of the application can be explained as follows:

1. The user interacts with the interface through a browser.
2. The request is received by the appropriate Spring MVC controller.
3. The controller validates the request and delegates processing to the service layer.
4. The service layer applies business rules and interacts with the repository layer.
5. The repository layer performs database operations using JPA.
6. The result is returned to the controller.
7. The controller passes data to a Thymeleaf template or redirects the user to the next workflow step.

This flow supports clarity, modularity, and easy maintenance.

## 10. Technology Stack
The project is built with the following technologies:

- `Java 17`
- `Spring Boot 4.0.0`
- `Spring Web`
- `Spring Security`
- `Spring Data JPA`
- `Thymeleaf`
- `MySQL`
- `Spring Mail`
- `Stripe Java SDK`
- `PDFBox` and `iText` for document and PDF-related operations
- `Maven` for build and dependency management

## 11. Core Entities in the Data Model
The current codebase includes the following core entities:

- `User`
- `Property`
- `Payment`
- `Message`
- `VisitSchedule`
- `Token`
- `AppReview`
- `ChatbotConversation`
- `ChatbotFaq`
- `ChatbotMessage`

These entities represent the major business objects required for rental listing, booking, payment, messaging, review, and support workflows.

## 12. Important Services Implemented
The service layer includes several specialized operational services:

- `PropertyService`
- `PaymentService`
- `PaymentReceiptService`
- `PaymentLifecycleSchedulerService`
- `StripeConnectService`
- `MessageService`
- `VisitScheduleService`
- `VisitReminderSchedulerService`
- `RentReminderSchedulerService`
- `EmailService`
- `TokenService`
- `ChatbotService`
- `CustomUserDetailsService`

This indicates that the project is not limited to static CRUD operations and already contains workflow-oriented automation.

## 13. Functional Requirements Summary
The system functionally supports:

- Registration and login
- Role-based redirection
- Property upload and management
- Property search and detail viewing
- Favorite handling
- Direct communication between stakeholders
- Booking and payment processing
- Landlord wallet visibility
- Review capture and display
- Visit scheduling
- Email-based token and reminder support
- Administrative approval workflows

## 14. Non-Functional Requirements Summary
The application also reflects several non-functional concerns:

- `Security`: authentication, authorization, CSRF protection, and controlled access by role
- `Usability`: dashboard-based flows and structured templates for different user journeys
- `Maintainability`: layered architecture with controller, service, repository, and entity separation
- `Scalability`: modular code organization suitable for future API or UI expansion
- `Reliability`: reminder scheduling, payment lifecycle handling, and persistent database storage
- `Performance`: server-side rendering suitable for business workflows and moderate application scale

## 15. Security and Trust Features
Security is a major requirement in a rental platform because the application handles user identities, property records, messages, and financial transactions. The current implementation includes:

- Spring Security-based authentication
- Role-controlled dashboards and operations
- Protected property workflows
- Token-driven account and password operations
- Stripe integration for secure payment handling
- Admin moderation mechanisms
- Property verification logic

These features contribute directly to the trust model of the application.

## 16. Scheduling and Automation Support
The configuration and service layer show that the project supports automation through scheduled jobs. These include:

- Rent reminder scheduling
- Visit reminder scheduling
- Payment late-fee evaluation
- Payout scheduling
- Booking refund-related scheduling

Automation reduces manual follow-up effort and improves user experience.

## 17. Advantages of the System
The NIVASA project offers several important advantages from both user and technical perspectives:

- It centralizes property listing, booking, communication, and payment tracking in one platform.
- It improves trust through verification-oriented workflows and role-based access control.
- It reduces manual dependency by automating reminders, payment lifecycle checks, and scheduled operations.
- It provides separate dashboards and features for tenants, landlords, and administrators.
- It uses a modular layered architecture, making the project easier to maintain and extend.
- It supports secure authentication and controlled access to sensitive actions.
- It integrates important business features such as messaging, reviews, visit scheduling, and payment support.
- It is built on widely used enterprise technologies, making it suitable for academic and real-world learning purposes.

## 18. Disadvantages or Challenges
Like most monolithic web applications, the system also has certain disadvantages and operational challenges:

- The application depends heavily on server-side rendering, which may feel less dynamic than a modern SPA-based frontend.
- The platform requires careful configuration of database, mail, and payment services before full deployment.
- Because many features are integrated into one application, complexity increases as the project grows.
- External integrations such as Stripe and SMTP introduce dependency on third-party services.
- Local file handling for uploads can become harder to manage at larger scale compared to cloud storage solutions.
- Admin verification improves trust but can also create operational workload if listing volume grows significantly.

## 19. Limitations in the Current Version
Although the project is comprehensive, the current version may still be improved in the following areas:

- Wrapper-based build execution is incomplete because the `.mvn` wrapper metadata is missing
- Some configurations still contain placeholder credentials for email, Stripe, and OpenAI
- Production hardening and deployment documentation can be expanded
- Search and recommendation features can be made more advanced
- Reporting and analytics dashboards can be made more detailed
- Automated test coverage appears limited from the current test surface
- The current architecture is primarily monolithic, so future high-scale distribution would need additional redesign
- Monitoring, observability, and production-grade logging can be strengthened further

## 20. Future Enhancements
The following enhancements can strengthen the platform further:

- Advanced filtering and recommendation engine for property discovery
- Real-time notifications using WebSocket or push services
- AI-assisted rental support and smarter chatbot workflows
- Better analytics for landlords and administrators
- Multi-city insights and demand heatmaps
- Digital agreement generation and e-signature workflow
- Mobile app integration
- Audit logging and stronger monitoring for production deployment

## 21. Conclusion
NIVASA is a professionally structured rental management system that goes beyond simple listing display. It combines property management, role-based access, payments, messaging, visit scheduling, reviews, support, and admin verification into one cohesive platform. The project directly addresses a real-world problem in the rental ecosystem by improving trust, transparency, and process efficiency.

From a software engineering perspective, the project demonstrates strong modularity, practical business value, and a suitable foundation for future academic presentation or real-world product expansion. It is a credible full-stack system that can be presented as a complete software solution for modern rental housing management.

## 22. Suggested Academic Report Title
If you want to use this for a college or internship submission, a suitable title would be:

**"NIVASA: A Secure Web-Based Rental House Management and Property Verification System"**
