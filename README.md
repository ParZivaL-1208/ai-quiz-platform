# AI-Enhanced Quiz Platform

An intelligent, full-stack educational platform built with Spring Boot and Python. This application features a robust role-based access control system (Admin, Teacher, Student) and integrates a dedicated AI microservice to automatically classify quiz questions and generate personalized, data-driven student feedback.

> **⚠️ CRITICAL ARCHITECTURE NOTE**
> 
> This is the primary interface of a decoupled, two-part system. 
> 
> **The full application requires both repositories running simultaneously:**
> 1. **This Spring Boot Application:** Handles the UI, database operations, security, and user analytics (Runs on port 8080).
> 2. **The Python AI Microservice:** Handles machine learning, PDF parsing, and NLP text classification (Runs on port 5000). You must clone and run the AI microservice from this repository: https://github.com/ParZivaL-1208/ai-flask-quiz.git
>
> If the Python microservice is offline, PDF uploads and automated question classification features will throw `Connection refused` errors.

## 🏗 Architecture Overview

The system is designed using a microservice-inspired architecture:
1. **Primary Backend (Java/Spring Boot):** Handles user authentication (including 2FA), role authorization, relational data persistence, and the presentation layer via Thymeleaf.
2. **AI & Processing Engine (Python/Flask):** A decoupled microservice responsible for ingesting PDF course materials, extracting text, and maintaining a TF-IDF vectorization model. It exposes REST endpoints for the Java backend to classify text and rebuild the NLP model dynamically.

## ✨ Key Features

* **Intelligent Document Ingestion:** Teachers can upload PDF chapters. The Python microservice automatically extracts the text using `pdfplumber` and continuously re-trains a `scikit-learn` NLP model.
* **Automated Question Classification:** When teachers create quiz questions, the Java backend queries the Python API to calculate cosine similarity against the ingested PDFs, automatically categorizing questions into the correct chapter repository.
* **Granular Performance Analytics:** Calculates student mastery on a per-chapter basis. If a student falls below a configurable mastery threshold (default 70%), the system generates targeted feedback directing them to review specific PDF materials.
* **Comprehensive Role Management:** * **Admin:** Oversees system access and user database.
  * **Teacher:** Manages question banks, creates quizzes, reviews retake requests, and monitors student analytics.
  * **Student:** Takes randomized quizzes, reviews detailed performance metrics, and submits retake workflows.
* **Security:** Implements Spring Security with bcrypt password hashing and an email-based One-Time Password (OTP) Two-Factor Authentication system.

## 💻 Tech Stack

**Core Backend:**
* Java 17
* Spring Boot 3.5.x (Web, Security, Data JPA, Mail)
* Hibernate / MySQL
* Maven

**AI & Microservice (Python):**
* Flask (REST API)
* `scikit-learn` (TF-IDF Vectorizer, Cosine Similarity)
* `pdfplumber` (Document Extraction)
* `numpy`

**Frontend:**
* Thymeleaf (Server-side templating)
* Bootstrap 5
* HTML5 / CSS3 / Vanilla JS

## 🚀 Local Setup & Installation

Because this project utilizes two separate servers, both must be running simultaneously for full functionality.

### Prerequisites
* Java 17+
* Python 3.9+
* MySQL Server (running on default port 3306)

### 1. Database Configuration
Create a local MySQL database named `quizzdb`. Update the credentials in `src/main/resources/application.properties` if necessary:
```properties
spring.datasource.username=root
spring.datasource.password=your_password
