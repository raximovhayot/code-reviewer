# AI Code Reviewer for GitLab

An automated, AI-powered code review tool designed to integrate seamlessly with GitLab Merge Requests. Built with Java Spring Boot and Spring AI, this application listens for GitLab webhooks and automatically reviews code diffs using LLMs.

## Features

*   **Automated Code Review:** Instantly reviews Merge Requests upon creation or update.
*   **GitLab Integration:** Native support for GitLab Webhooks and GitLab API (`gitlab4j-api`).
*   **AI-Powered:** Utilizes `Spring AI` and Ollama for intelligent, context-aware code analysis.
*   **Direct Feedback:** Posts review comments directly onto the GitLab Merge Request timeline.

## Prerequisites

*   Java 21 or higher
*   A running instance of [Ollama](https://ollama.com/) (with your preferred model pulled)
*   GitLab Personal Access Token (with `api` scope)

## Getting Started

### 1. Configuration

Configure your application properties. You can set these in `src/main/resources/application.properties` or expose them as environment variables:

```properties
# GitLab Configuration
gitlab.url=https://gitlab.com
gitlab.personal-access-token=your_personal_access_token_here

# Ollama Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=llama3 # or your preferred model
```

### 2. Running the Application

This project uses the Maven Wrapper, so you don't need Maven installed globally.

**On Windows:**
```powershell
.\mvnw spring-boot:run
```

**On macOS/Linux:**
```bash
./mvnw spring-boot:run
```

### 3. GitLab Webhook Setup

1.  Navigate to your GitLab repository -> **Settings** -> **Webhooks**.
2.  Set the **URL** to your application's exposed endpoint: `http://<your-domain>/webhook/gitlab`
3.  Under **Triggers**, select **Merge request events**.
4.  Add the webhook.

Now, whenever a Merge Request is created or updated, GitLab will notify this application, which will fetch the diff, analyze it using the AI model, and post a review comment!

## Testing

To run the test suite:

```bash
./mvnw test
```

## Technologies Used

*   **Java 21**
*   **Spring Boot**
*   **Spring AI**
*   **Spring Modulith**
*   **GitLab4J API**
