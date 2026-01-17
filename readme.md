# 🏠 Chores Management System

![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.1-brightgreen) ![Docker](https://img.shields.io/badge/Docker-Enabled-blue) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-336791) ![Deployment](https://img.shields.io/badge/Deployed_on-Render-black)

A Spring Boot middleware that automates household chore management via the **WhatsApp Business API**. This application handles turn rotation, enforces permission checks, and manages task reminders using a conversational interface backed by a **Supabase** (PostgreSQL) database.

It serves as an impartial "bot" that governs the family's cleaning schedule, ensuring fairness and accountability while reducing interpersonal conflict.

---

### 🤖 What the App Does

The application functions as a centralized command center for family chores, handling everything from scheduling to conflict resolution directly through WhatsApp.

* **Automated Turn Rotation**
    Every morning at 8:00 AM, the `SchedulerService` automatically rotates the turn to the next person in line (seeded as Ashraf, Ahmed, Omar, and Mohamed) and sends a template notification to alert them of their duty.

* **Smart Reminders & Nagging**
    A "friendly reminder" is scheduled for the evening (8:00 PM). This message uses a conversational tone to prompt the assigned member to finish their tasks before the day ends.

* **Interactive "Snitch" System**
    Family members can file formal complaints via a "Complain" button. The bot enters a listening mode to capture photo evidence, identifies the "guilty" member, and forwards the photo to them with a "🚨 COMPLAINT FILED 🚨" caption.

* **Permission-Based Skip Requests**
    * **Auto-Approval:** "Parent" and "Admin" roles are automatically approved for skips, triggering an immediate rotation.
    * **Admin Approval:** "Child" roles must wait for approval. The bot sends an interactive message to the Admin with "Approve" and "Deny" buttons.

* **Admin Control Panel**
    The Admin has exclusive access to a control panel via the "Admin" command, allowing them to "Force Rotate" the turn or "Resend Alerts" if notifications are missed.

---

### ❤️ Benefits for the Family

* **Eliminates "I Forgot" Excuses:** Automated WhatsApp notifications ensure there is no ambiguity about whose turn it is.
* **Reduces Interpersonal Conflict:** The bot acts as the "bad guy" for nagging and complaints, depersonalizing enforcement.
* **Ensures Fairness:** Rigid, database-backed rotation tracks exactly who is next, ensuring no one gets lost in the shuffle if a turn is skipped.
* **Accountability with Evidence:** Photo evidence resolves arguments by providing visual proof of the mess to the responsible party.

---

### ⚡ Interesting Techniques

* **Webhook Event Processing** 📡
    The [WebhookController](src/main/java/com/family/chores/controller/WebhookController.java) manually traverses complex JSON payloads using Jackson's `JsonNode`. This avoids rigid DTO binding, allowing flexible handling of diverse message types like text, images, and [interactive buttons](https://developers.facebook.com/docs/whatsapp/guides/interactive-messages).

* **In-Memory State Management** 🧠
    A `ConcurrentHashMap` tracks transient user states (e.g., `COMPLAINING` mode waiting for photo evidence). This creates a lightweight session mechanism for ephemeral contexts without the overhead of persistent storage.

* **Cron-Based Scheduling** ⏲️
    The [SchedulerService](src/main/java/com/family/chores/service/SchedulerService.java) leverages Spring's `@Scheduled` annotation to trigger daily logic. It automatically rotates the chore assignee and dispatches template notifications via the WhatsApp API.

* **Dynamic Response Construction** 💬
    The [WhatsAppService](src/main/java/com/family/chores/service/WhatsAppService.java) dynamically builds raw JSON payloads for interactive messages, allowing the buttons (e.g., "Approve" vs "Deny") to change based on the user's role (Admin vs Standard).

---

### 🛠️ Technology Stack

| Technology | Purpose | Documentation |
| :--- | :--- | :--- |
| **Spring Boot 4.0.1** | Core framework for MVC, DI, and Data | [Spring Docs](https://spring.io/projects/spring-boot) |
| **Supabase** | Managed PostgreSQL Database | [Supabase Docs](https://supabase.com/) |
| **Render** | Cloud Hosting & Deployment | [Render Docs](https://render.com/) |
| **Docker** | Multi-stage containerization | [Docker Docs](https://docs.docker.com/) |
| **Maven Wrapper** | Build tool version management | [Maven Wrapper](https://maven.apache.org/wrapper/) |

---

### 📂 Project Structure

```text
/
├── src/
│   ├── main/
│   │   ├── java/com/family/chores/
│   │   │   ├── controller/      # REST Endpoints (Webhooks, Testing)
│   │   │   ├── model/           # JPA Entities (FamilyMember)
│   │   │   ├── repository/      # Data Access Interfaces
│   │   │   └── service/         # Business Logic (Rotation, Scheduling)
│   │   └── resources/           # Application Properties
│   └── test/                    # Integration Tests
├── Dockerfile                   # Multi-stage build definition
├── pom.xml                      # Dependencies
└── mvnw                         # Maven Wrapper