# Plan: Multi-Agent Application Operations POC

## 1. Goal

Build an automated Application Operations solution where a controlled
error in a Spring Boot application is detected by Dynatrace, enriched by
Azure OpenAI GPT-4o-mini, and converted into a ServiceNow incident by
the Application Operations Agent (AOA).

### Target flow

``` text
Spring Boot Application
        |
        | Error occurs
        v
Dynatrace OneAgent
        |
        | Problem detected
        v
Dynatrace Problem Notification Webhook
        |
        | POST /api/agent/process-incident
        v
Application Operations Agent (AOA)
        |
        +--> DynatraceService
        |       +--> Dynatrace REST API v2
        |
        +--> IncidentEnrichmentService
        |       +--> Azure OpenAI GPT-4o-mini
        |
        +--> ServiceNowService
                +--> ServiceNow Table REST API
                        |
                        v
                ServiceNow Incident
```

The entire pipeline should run automatically with no human in the loop.

------------------------------------------------------------------------

## 2. Architecture

### Components

  -----------------------------------------------------------------------
  Component                           Responsibility
  ----------------------------------- -----------------------------------
  Spring Boot application             Generates controlled application
                                      errors

  Dynatrace OneAgent                  Captures metrics, logs and traces

  Dynatrace Problem Detection         Detects application problems

  Dynatrace Webhook                   Sends problem notification to AOA

  Application Operations Agent        Orchestrates the incident workflow

  DynatraceService                    Retrieves problem details and logs

  Azure OpenAI GPT-4o-mini            Converts observability data into
                                      structured incident content

  IncidentEnrichmentService           Calls the LLM and parses the
                                      structured response

  ServiceNowService                   Checks for duplicates and creates
                                      incidents

  ServiceNow PDI                      Stores the resulting ITSM incident
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 3. End-to-End Sequence

``` text
Spring Boot App
      |
      | error captured
      v
Dynatrace OneAgent
      |
      | problem detected
      v
Dynatrace Webhook
      |
      | POST /api/agent/process-incident
      v
AOA
      |
      +--> GET /api/v2/problems/{id}
      |
      +--> GET /api/v2/logs/search
      |
      +--> Azure OpenAI GPT-4o-mini
      |       |
      |       +--> structured incident JSON
      |
      +--> ServiceNow duplicate check
      |
      +--> POST /api/now/table/incident
      |
      v
ServiceNow
      |
      | incident number
      v
AOA application logs
```

------------------------------------------------------------------------

## 4. LLM Recommendation

### Recommended model

**Azure OpenAI GPT-4o-mini**

### Why

-   Fast
-   Low cost
-   Reliable structured JSON output
-   Suitable for an operations-analysis POC
-   Keeps the model deployment inside the Azure environment
-   The LLM has a focused responsibility rather than owning the whole
    orchestration

### LLM responsibility

The LLM should transform:

``` text
Dynatrace problem JSON
+
Relevant application error logs
```

into:

``` json
{
  "short_description": "...",
  "description": "...",
  "urgency": 2,
  "impact": 2,
  "category": "application",
  "recommended_actions": [
    "...",
    "...",
    "..."
  ]
}
```

Use structured JSON output rather than free-form text.

------------------------------------------------------------------------

# 5. Prerequisites

## 5.1 Azure OpenAI

1.  Open the Azure portal.
2.  Create an Azure OpenAI resource.
3.  Deploy the `gpt-4o-mini` model.
4.  Use deployment name `gpt-4o-mini`.
5.  Copy the endpoint and API key.
6.  Configure:

``` text
AZURE_OPENAI_ENDPOINT
AZURE_OPENAI_API_KEY
```

Do not commit secrets to Git.

------------------------------------------------------------------------

## 5.2 Dynatrace

Use a Dynatrace trial for the POC if appropriate.

Required setup:

1.  Create/sign in to Dynatrace.
2.  Install Dynatrace OneAgent on the Spring Boot host/container
    environment.
3.  Enable Log Monitoring.
4.  Confirm the Spring Boot process is visible in Dynatrace.
5.  Create a Dynatrace API token with required scopes such as:

``` text
problems.read
logs.read
metrics.read
```

6.  Configure:

``` text
DT_API_URL
DT_API_TOKEN
```

7.  Configure a Dynatrace Problem Notification webhook.
8.  Configure the webhook URL:

``` text
http://<host>:8080/api/agent/process-incident
```

For Azure deployment, replace the local URL with the public Azure
Container App URL.

9.  Configure the shared webhook authentication header:

``` text
X-Dynatrace-Problem-Authentication
```

10. Store the shared secret as:

``` text
DT_WEBHOOK_AUTH_TOKEN
```

------------------------------------------------------------------------

## 5.3 ServiceNow PDI

1.  Activate/create a ServiceNow Personal Developer Instance.
2.  Note the instance URL.
3.  Create a service account or use an appropriate admin account for the
    POC.
4.  Enable REST API access.
5.  Optionally create a sample knowledge article:

``` text
Spring Boot Application Rollback Procedure
```

6.  Configure:

``` text
SN_INSTANCE_URL
SN_USERNAME
SN_PASSWORD
```

Do not commit credentials to Git.

------------------------------------------------------------------------

# 6. Implementation Phases

## Phase 1 --- Spring AI + Azure OpenAI Wiring

### Objective

Integrate Spring AI with Azure OpenAI GPT-4o-mini.

### Changes

Update `pom.xml` with the Spring AI BOM and Azure OpenAI starter.

Expected dependency direction:

``` xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-azure-openai-spring-boot-starter</artifactId>
</dependency>
```

Configure Azure OpenAI in `application.yml`.

The target model/deployment is:

``` text
gpt-4o-mini
```

The response should be structured JSON.

### Verification

Call the LLM with a simple test prompt and confirm a valid JSON
response.

------------------------------------------------------------------------

# Phase 2 --- DynatraceService

Create:

``` text
service/DynatraceService.java
```

### Responsibilities

``` text
getProblemById(problemId)
getRecentErrorLogs(affectedEntityId)
```

### Dynatrace APIs

Problem:

``` text
GET /api/v2/problems/{id}
```

Logs:

``` text
GET /api/v2/logs/search
```

### Verification

Use a real Dynatrace problem ID and confirm that:

-   Problem details are returned.
-   Relevant logs are returned.
-   Authentication works.
-   Errors are handled cleanly.

------------------------------------------------------------------------

# Phase 3 --- ServiceNowService

Create:

``` text
service/ServiceNowService.java
```

### Responsibilities

``` text
createIncident(ServiceNowIncidentRequest request)
findExistingIncident(String correlationId)
```

### Create incident

``` text
POST /api/now/table/incident
```

### Duplicate check

Use the incident table with:

``` text
sysparm_query=correlation_id={problemId}
```

The Dynatrace problem ID should be used as the ServiceNow
`correlation_id`.

### Authentication

Use Basic Authentication for the POC:

``` text
SN_USERNAME
SN_PASSWORD
```

### Response

Map the ServiceNow response into:

``` text
ServiceNowIncidentResponse
```

with fields such as:

``` text
sysId
incidentNumber
```

### Verification

Call `createIncident()` directly and confirm that an incident is created
in ServiceNow.

Then verify that calling the duplicate-check method with the same
correlation ID finds the existing incident.

------------------------------------------------------------------------

# Phase 4 --- IncidentEnrichmentService

Create:

``` text
service/IncidentEnrichmentService.java
```

### Main operation

``` text
enrich(DynatraceProblemDetail problem, List<String> logs)
```

Returns:

``` text
EnrichedIncident
```

### LLM system prompt

The model should behave as an:

``` text
IT operations analyst
```

and analyze the Dynatrace problem and application logs.

The expected fields are:

``` text
short_description
description
urgency
impact
category
recommended_actions
```

### Example expected reasoning output

``` text
Application performance degraded: 6-second database query latency detected in tieto-poc-app.

Dynatrace identified elevated SLOW_DATABASE_QUERY error rate.

Affected service: OrderService.

Recommended actions:
1. Check database connection pool utilization.
2. Review recent deployments.
3. Inspect PostgreSQL slow query logs.
```

### Log handling

Do not send unlimited application logs to the LLM.

Pass the problem details and a bounded set of relevant error log lines
to keep the request efficient and predictable.

### Verification

Mock a `DynatraceProblemDetail` and representative logs.

Verify that GPT-4o-mini returns valid, parseable `EnrichedIncident`
JSON.

------------------------------------------------------------------------

# Phase 5 --- AOA Orchestrator + Webhook

## 5.1 ApplicationOperationsAgent

Create:

``` text
service/ApplicationOperationsAgent.java
```

### Pipeline

1.  Check the Dynatrace event status.
2.  Skip `RESOLVED` events for the initial POC.
3.  Retrieve the Dynatrace problem.
4.  Retrieve recent error logs.
5.  Enrich the problem using Azure OpenAI.
6.  Check ServiceNow for an existing incident.
7.  Skip incident creation if a duplicate already exists.
8.  Map the enriched fields into a ServiceNow incident request.
9.  Set:

``` text
correlation_id = problemId
```

10. Create the ServiceNow incident.
11. Write an audit log for `INCIDENT_CREATED`.
12. Log the created incident number.

### Expected log

``` text
[AOA] Incident {incidentNumber} created for DT problem {problemId}
```

------------------------------------------------------------------------

## 5.2 IncidentWebhookController

Create:

``` text
controller/IncidentWebhookController.java
```

### Endpoint

``` text
POST /api/agent/process-incident
```

### Responsibilities

1.  Receive the Dynatrace webhook payload.
2.  Validate:

``` text
X-Dynatrace-Problem-Authentication
```

against:

``` text
DT_WEBHOOK_AUTH_TOKEN
```

3.  Delegate to:

``` text
ApplicationOperationsAgent.processIncident(event)
```

4.  Return HTTP 200 for successfully handled webhook requests.
5.  Handle failures in a way that allows Dynatrace retries when
    appropriate.

------------------------------------------------------------------------

# 7. DTOs

Create:

``` text
dto/
├── DynatraceProblemEvent.java
├── DynatraceProblemDetail.java
├── EnrichedIncident.java
├── ServiceNowIncidentRequest.java
└── ServiceNowIncidentResponse.java
```

## DynatraceProblemEvent

Represents the webhook event received from Dynatrace.

Important information includes:

``` text
problemId
status
affectedEntity
event metadata
```

## DynatraceProblemDetail

Represents the detailed Dynatrace problem retrieved from the REST API.

## EnrichedIncident

Represents the structured response returned by Azure OpenAI.

Expected fields:

``` text
shortDescription
description
urgency
impact
category
recommendedActions
```

## ServiceNowIncidentRequest

Contains the fields required to create the ServiceNow incident.

Important fields:

``` text
short_description
description
urgency
impact
category
correlation_id
```

## ServiceNowIncidentResponse

Contains the ServiceNow response fields needed by AOA:

``` text
sysId
incidentNumber
```

------------------------------------------------------------------------

# 8. Configuration

Update:

``` text
src/main/resources/application.yml
```

with configuration for:

-   Azure OpenAI
-   Dynatrace
-   ServiceNow
-   AOA webhook authentication

Use environment variables for secrets.

Example:

``` yaml
azure:
  openai:
    endpoint: ${AZURE_OPENAI_ENDPOINT}
    api-key: ${AZURE_OPENAI_API_KEY}

dynatrace:
  api-url: ${DT_API_URL}
  api-token: ${DT_API_TOKEN}
  webhook-auth-token: ${DT_WEBHOOK_AUTH_TOKEN}

servicenow:
  instance-url: ${SN_INSTANCE_URL}
  username: ${SN_USERNAME}
  password: ${SN_PASSWORD}
```

Use the exact property structure required by the Spring AI version used
by the project.

------------------------------------------------------------------------

# 9. Docker Compose

Update:

``` text
docker-compose.yml
```

with environment-variable placeholders for:

``` text
AZURE_OPENAI_ENDPOINT
AZURE_OPENAI_API_KEY
DT_API_URL
DT_API_TOKEN
DT_WEBHOOK_AUTH_TOKEN
SN_INSTANCE_URL
SN_USERNAME
SN_PASSWORD
```

Secrets should be provided through a local `.env` file or environment
configuration and should not be committed.

------------------------------------------------------------------------

# 10. End-to-End Test

## Start application

``` bash
docker compose up
```

Confirm that the Spring Boot application is healthy.

## Trigger controlled error

Use an existing error simulator endpoint, for example:

``` bash
curl -X POST http://localhost:8080/api/errors/slow-query
```

Repeat the request several times within approximately two minutes if
required to trigger Dynatrace problem detection.

Alternative:

``` bash
curl -X POST http://localhost:8080/api/errors/null-pointer
```

## Verify Dynatrace

Confirm:

1.  OneAgent captures the application activity.
2.  Logs are visible.
3.  A Dynatrace problem is opened.
4.  The problem contains the expected affected service/entity.

## Verify webhook

Confirm that Dynatrace invokes:

``` text
POST /api/agent/process-incident
```

## Verify AOA

Check Spring Boot logs for:

``` text
[AOA] Incident ... created for DT problem ...
```

## Verify ServiceNow

Open ServiceNow and confirm:

-   Incident exists.
-   `correlation_id` contains the Dynatrace problem ID.
-   Description is enriched by the LLM.
-   Urgency/impact/category are populated.
-   Recommended actions are included.

------------------------------------------------------------------------

# 11. Expected Incident Example

``` text
Short description:
Application performance degraded - slow database queries

Description:
Application performance degraded: 6-second database query latency
detected in tieto-poc-app.

Dynatrace identified elevated SLOW_DATABASE_QUERY error rate
starting at 14:32 UTC.

Affected service:
OrderService.

Recommended actions:
1. Check database connection pool utilization.
2. Review recent deployments.
3. Inspect PostgreSQL slow query logs.
```

------------------------------------------------------------------------

# 12. Duplicate Incident Prevention

The POC should prevent duplicate ServiceNow incidents for the same
Dynatrace problem.

Use:

``` text
Dynatrace problemId
        |
        v
ServiceNow correlation_id
```

Before creating an incident:

``` text
findExistingIncident(problemId)
```

If an incident already exists:

``` text
Do not create another incident.
```

If no incident exists:

``` text
Create new incident.
```

------------------------------------------------------------------------

# 13. Error Handling

The AOA should handle failures from:

-   Invalid Dynatrace webhook authentication
-   Dynatrace API unavailable
-   Dynatrace problem not found
-   Dynatrace logs unavailable
-   Azure OpenAI unavailable
-   Invalid LLM JSON
-   ServiceNow unavailable
-   ServiceNow authentication failure
-   Duplicate incident detected

Do not expose credentials or sensitive API responses in logs.

------------------------------------------------------------------------

# 14. Testing Strategy

## Unit tests

Test:

``` text
DynatraceService
ServiceNowService
IncidentEnrichmentService
ApplicationOperationsAgent
IncidentWebhookController
```

Mock external API calls.

## Integration tests

Verify:

``` text
Spring Boot
    |
    +--> Dynatrace integration
    |
    +--> Azure OpenAI integration
    |
    +--> ServiceNow integration
```

Only enable real external integrations in an appropriate POC/integration
environment.

## End-to-end test

Validate:

``` text
Controlled Error
    ↓
Dynatrace Problem
    ↓
Webhook
    ↓
AOA
    ↓
Dynatrace API
    ↓
Azure OpenAI
    ↓
ServiceNow
```

------------------------------------------------------------------------

# 15. Azure Deployment

## Infrastructure

Provision:

``` text
Azure Container Registry
Azure Container Apps Environment
Azure Container App
```

## Build image

Example:

``` bash
docker build -t <acr>.azurecr.io/tieto-poc:v1 .
```

## Push image

``` bash
docker push <acr>.azurecr.io/tieto-poc:v1
```

## Deploy

Deploy the image to Azure Container Apps.

Configure all secrets as Container Apps secrets/environment variables.

Required values:

``` text
AZURE_OPENAI_ENDPOINT
AZURE_OPENAI_API_KEY
DT_API_URL
DT_API_TOKEN
DT_WEBHOOK_AUTH_TOKEN
SN_INSTANCE_URL
SN_USERNAME
SN_PASSWORD
```

## Dynatrace webhook

Update the webhook URL to the public Azure Container App endpoint:

``` text
https://<container-app-host>/api/agent/process-incident
```

## Final Azure test

Repeat the end-to-end test against the Azure-hosted application.

------------------------------------------------------------------------

# 16. Project Structure

Target structure:

``` text
src/main/java/com/tieto/poc/ai_servicenow/

├── config/
│   └── AzureOpenAIConfig.java
│
├── controller/
│   └── IncidentWebhookController.java
│
├── service/
│   ├── ApplicationOperationsAgent.java
│   ├── DynatraceService.java
│   ├── ServiceNowService.java
│   └── IncidentEnrichmentService.java
│
└── dto/
    ├── DynatraceProblemEvent.java
    ├── DynatraceProblemDetail.java
    ├── EnrichedIncident.java
    ├── ServiceNowIncidentRequest.java
    └── ServiceNowIncidentResponse.java
```

Existing application components such as the order controller, order
service, error simulator, RabbitMQ producer/consumer, audit logging and
database remain part of the sample application and can continue to
generate realistic operational failures.

------------------------------------------------------------------------

# 17. New/Modified Files Summary

## New files

``` text
src/main/java/com/tieto/poc/ai_servicenow/config/AzureOpenAIConfig.java

src/main/java/com/tieto/poc/ai_servicenow/controller/IncidentWebhookController.java

src/main/java/com/tieto/poc/ai_servicenow/service/ApplicationOperationsAgent.java
src/main/java/com/tieto/poc/ai_servicenow/service/DynatraceService.java
src/main/java/com/tieto/poc/ai_servicenow/service/ServiceNowService.java
src/main/java/com/tieto/poc/ai_servicenow/service/IncidentEnrichmentService.java

src/main/java/com/tieto/poc/ai_servicenow/dto/DynatraceProblemEvent.java
src/main/java/com/tieto/poc/ai_servicenow/dto/DynatraceProblemDetail.java
src/main/java/com/tieto/poc/ai_servicenow/dto/EnrichedIncident.java
src/main/java/com/tieto/poc/ai_servicenow/dto/ServiceNowIncidentRequest.java
src/main/java/com/tieto/poc/ai_servicenow/dto/ServiceNowIncidentResponse.java
```

## Modified files

``` text
pom.xml
src/main/resources/application.yml
docker-compose.yml
```

------------------------------------------------------------------------

# 18. Scope Boundaries

  -----------------------------------------------------------------------
  In Scope                            Out of Scope
  ----------------------------------- -----------------------------------
  Automated application error         Conversational AOA/chat UI
  detection                           

  Dynatrace observability             Automatic ServiceNow closure on
                                      RESOLVED

  Dynatrace REST API v2               Azure AD managed identity for the
                                      initial POC

  LLM-based incident enrichment       Dynatrace MCP integration

  Azure OpenAI GPT-4o-mini            ServiceNow NowAssist / GenAI
                                      features

  ServiceNow Table REST API           Full autonomous remediation

  Duplicate detection using           Production-grade multi-region
  correlation ID                      architecture

  Azure Container Apps deployment     Full enterprise security hardening
  -----------------------------------------------------------------------

------------------------------------------------------------------------

# 19. Definition of Done

The POC is considered successful when all of the following work:

-   [ ] Spring Boot application runs successfully.
-   [ ] Controlled application error can be triggered.
-   [ ] Dynatrace OneAgent captures the application.
-   [ ] Dynatrace detects the resulting problem.
-   [ ] Dynatrace sends the webhook to AOA.
-   [ ] AOA validates the webhook authentication.
-   [ ] AOA retrieves the Dynatrace problem.
-   [ ] AOA retrieves relevant logs.
-   [ ] Azure OpenAI GPT-4o-mini returns valid structured JSON.
-   [ ] AOA converts the LLM output into a ServiceNow request.
-   [ ] Duplicate ServiceNow incident detection works.
-   [ ] ServiceNow incident is created automatically.
-   [ ] Incident description contains meaningful LLM enrichment.
-   [ ] Recommended actions are included.
-   [ ] AOA logs the created incident number.
-   [ ] The complete flow works locally.
-   [ ] The complete flow works after Azure Container Apps deployment.

------------------------------------------------------------------------

# 20. Final POC Flow

``` text
┌──────────────────────┐
│   Spring Boot App    │
│  Controlled Error    │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Dynatrace OneAgent   │
│ Metrics / Logs /     │
│ Traces               │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Dynatrace Problem    │
│ Detection            │
└──────────┬───────────┘
           │ Webhook
           ▼
┌──────────────────────────────┐
│ Application Operations Agent │
│            AOA               │
└──────────┬───────────────────┘
           │
     ┌─────┼──────────────┐
     │     │              │
     ▼     ▼              ▼
 Dynatrace  Azure OpenAI  ServiceNow
   API       GPT-4o-mini     API
     │          │             │
     │          │             │
     └─────┬────┘             │
           │                  │
           ▼                  │
   Enriched Incident ─────────┘
           │
           ▼
┌──────────────────────┐
│ ServiceNow Incident  │
│ INCxxxxxxxx          │
└──────────────────────┘
```

**Result:** an application failure is automatically converted into an
observability-enriched, LLM-generated ServiceNow incident without manual
intervention.
