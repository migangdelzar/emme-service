# EMME Client Requirements Catalog

| Field | Value |
|---|---|
| Source | `docs/vision.md`, `docs/entity_model.md` |
| Audience | End customers (WhatsApp, web chat, web frontend) |
| Scope | v1 Spring Modulith |
| Date | 2026-08-04 |

## Functional Requirements

### Customer Authentication

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-C001 | Customer sign in | As a customer, I want to authenticate with my phone number so that I can access my booking history and profile. | High | Implemented |
| FR-C002 | Update customer profile | As a customer, I want to update my name, contact details, and preferences so that my profile stays accurate. | High | Implemented |

### Channel Interaction

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-C003 | Receive WhatsApp message | As a customer, I want to send a message through the salon's Meta WhatsApp channel so that I can interact with EMME. | High | Implemented |
| FR-C004 | Use web chat | As a customer, I want to use web chat with the same core capabilities as WhatsApp so that I can choose my preferred channel. | High | Implemented |
| FR-C005 | Verify WhatsApp webhook | As a system operator, I want Meta webhook verification and signatures validated so that only authentic callbacks are processed. | High | Implemented |
| FR-C006 | Normalize channel messages | As a conversation system, I want channel-specific payloads normalized into one tenant-scoped message contract so that orchestration is channel independent. | High | Implemented |
| FR-C007 | Deduplicate inbound message | As a salon owner, I want retried webhooks deduplicated so that one customer message cannot trigger duplicate work. | High | Implemented |

### Multimodal Input

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-C008 | Transcribe voice input | As a customer, I want supported voice notes transcribed so that I can communicate without typing. | Medium | Implemented |
| FR-C009 | Analyze image input | As a customer, I want nail-design images converted into structured style features so that EMME can recommend and estimate relevant services. | High | Implemented |
| FR-C010 | Combine multimodal input | As a customer, I want text instructions to refine accompanying voice or image content so that my complete intent is respected. | High | Implemented |

### AI-Powered Guidance

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-C011 | Detect conversation intent | As a customer, I want EMME to identify one or more intents from my message so that relevant tools can answer my request. | High | Implemented |
| FR-C012 | Recommend services | As a customer, I want tenant-catalog service recommendations grounded in my request so that I can select an appropriate service. | High | Implemented |
| FR-C013 | Estimate price | As a customer, I want a price estimate derived from the tenant's structured catalog so that the estimate reflects current salon pricing. | High | Implemented |
| FR-C014 | Answer policy question | As a customer, I want answers grounded in the tenant's approved knowledge base so that salon policies are represented accurately. | High | Implemented |
| FR-C015 | Retrieve tenant knowledge | As an AI system, I want hybrid vector and keyword retrieval filtered by tenant before ranking so that responses use only authorized knowledge. | High | Implemented |
| FR-C016 | Handle AI provider failure | As a customer, I want an actionable fallback response when an AI provider fails so that unsafe or fabricated actions are not performed. | High | Implemented |

### Conversational Booking

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-C017 | Draft conversational booking | As a customer, I want EMME to assemble a booking draft from conversation context so that I do not repeat known details. | High | Implemented |
| FR-C018 | Propose booking action | As a customer, I want EMME to present a proposed booking action with service, date, time, and price so that I can review before committing. | High | Implemented |
| FR-C019 | Confirm consequential action | As a customer, I want EMME to request explicit confirmation before booking, cancellation, or payment actions so that unintended changes are prevented. | High | Implemented |
| FR-C020 | Reject proposed action | As a customer, I want to reject a proposed booking or payment action so that it is not executed. | High | Implemented |
| FR-C021 | Resume pending confirmation | As a customer, I want a pending confirmation to survive application restart until its expiry so that a recoverable interruption does not lose my booking flow. | High | Implemented |
| FR-C022 | Cancel expired confirmation | As a salon owner, I want unconfirmed actions to expire without changing business state so that stale requests cannot execute. | High | Implemented |

### Appointment Booking (Customer Side)

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-C023 | Search available slots | As a customer, I want to find available appointment slots using salon hours, service duration, and existing bookings so that I can choose a valid time. | High | Implemented |
| FR-C024 | Create appointment | As a customer, I want to create an appointment for myself with a valid service, artist, and slot so that the booking is recorded. | High | Implemented |
| FR-C025 | Reschedule appointment | As a customer, I want to move my appointment to another valid slot so that schedule changes are supported. | High | Implemented |
| FR-C026 | Cancel appointment | As a customer, I want to cancel my eligible appointment so that the slot is released. | High | Implemented |

### Conversation History & Context

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-C027 | Store conversation history | As a customer, I want my conversation history retained so that I can resume past interactions without repeating context. | High | Implemented |
| FR-C028 | Summarize conversation | As a conversation system, I want to maintain an expiring summary of long conversations so that AI context remains efficient. | Medium | Implemented |
| FR-C029 | View conversation events | As a customer, I want to see the sequence of messages and actions in my conversation so that I understand what was discussed. | High | Implemented |

### Notification Delivery

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-C030 | Receive WhatsApp response | As a customer, I want replies delivered through the salon's direct Meta WhatsApp integration so that the conversation completes in the originating channel. | High | Implemented |
| FR-C031 | Receive appointment reminder | As a customer, I want to receive appointment reminders through WhatsApp so that I do not miss my scheduled service. | Medium | Implemented |

### Calendar Sync (Customer Side)

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-C032 | Sync appointment to calendar | As a customer, I want my confirmed appointment synced to my personal calendar so that it appears alongside my other events. | Medium | Implemented |
| FR-C033 | Unsync appointment from calendar | As a customer, I want to remove a calendar-synced appointment from my personal calendar so that cancelled bookings are reflected. | Medium | Implemented |

### Catalog Discovery

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-C034 | Browse service catalog | As a customer, I want to view available salon services with descriptions and prices so that I can choose what to book. | High | Implemented |
| FR-C035 | Match nail design | As a customer, I want to submit a nail design image and find matching catalog services so that I can request the look I want. | Medium | Implemented |
| FR-C036 | Browse nail designs | As a customer, I want to browse the salon's nail design catalog so that I can explore inspiration before booking. | Medium | Implemented |

## Non-Functional Requirements

| ID | Title | Requirement | Category | Priority | Status |
|---|---|---|---|---|---|
| NFR-C001 | Conversation latency | Excluding external channel delivery, 95% of text-only AI responses must complete within 10 seconds and 95% of single-image responses within 20 seconds. | Performance | Medium | Open |
| NFR-C002 | Channel authentication | 100% of incoming WhatsApp webhooks must pass signature verification before message processing begins. | Security | High | Implemented |
| NFR-C003 | Message idempotency | Replaying the same WhatsApp webhook or channel message 10 times must produce no more than one durable business effect. | Reliability | High | Implemented |
| NFR-C004 | Tenant-scoped retrieval | 100% of RAG queries must filter knowledge by the authorized tenant before ranking; cross-tenant knowledge leakage is prohibited. | Security | High | Implemented |
| NFR-C005 | AI source of truth compliance | 100% of price estimates and availability answers must be derived from structured module APIs backed by SQL, never from RAG output alone. | Security | High | Implemented |
| NFR-C006 | Pending action expiry | 100% of unconfirmed booking, cancellation, or payment actions must expire without changing business state within 30 minutes. | Reliability | High | Implemented |
| NFR-C007 | Booking collision prevention | Concurrent booking requests for the same slot must be serialized so that double booking cannot occur. | Reliability | High | Implemented |
| NFR-C008 | Response safety | AI-generated responses must not fabricate prices, deadlines, or availability beyond the structured tenant catalog and booking policy. | Security | High | Implemented |
| NFR-C009 | Accessibility | Customer-facing web chat and booking flows must satisfy WCAG 2.2 AA automated checks with zero critical violations. | Usability | Medium | Open |
| NFR-C010 | Browser support | Customer-facing web interfaces must support the latest 2 stable versions of Chrome, Firefox, Safari, and Edge. | Portability | Medium | Open |
| NFR-C011 | Mobile support | The retained Capacitor application must complete 100% of its critical smoke flow on the current and previous major iOS and Android versions supported by Capacitor. | Portability | Medium | Open |
| NFR-C012 | Customer notification delivery | WhatsApp message delivery must be attempted within 30 seconds of the notification request for time-sensitive reminders. | Performance | Medium | Open |

## Constraints

| ID | Title | Constraint | Category | Priority | Status |
|---|---|---|---|---|---|
| C-C001 | WhatsApp integration | Production WhatsApp must integrate directly with Meta WhatsApp Cloud API; no third-party WhatsApp gateway is allowed. | Technical | High | Implemented |
| C-C002 | AI framework | AI orchestration, model access, tools, and vector-store integration must use Spring AI 2.0 inside the Modulith. | Technical | High | Implemented |
| C-C003 | Local AI | Ollama must support local development models and the approved production embedding model. | Technical | Medium | Implemented |
| C-C004 | Channel normalization | WhatsApp and web chat must use the same business conversation capabilities after channel normalization; channel-specific logic must be isolated to adapter layers. | Technical | High | Implemented |
| C-C005 | Customer authentication | Customer login must use phone-number-based token authentication through the Keycloak-backed identity module. | Technical | High | Implemented |
| C-C006 | AI source of truth | Prices and availability must be obtained through structured module APIs backed by SQL, never from RAG output. | Business | High | Implemented |
| C-C007 | Conversation storage | Conversation history and pending actions must be tenant-scoped and stored in PostgreSQL; no separate conversation database is required in v1. | Technical | High | Implemented |
| C-C008 | Multimodal processing | Voice transcription, image analysis, and text intent detection must use Spring AI with provider-agnostic adapter interfaces. | Technical | High | Implemented |
| C-C009 | Graph projection | Apache AGE must be a tenant-isolated, derived, disposable, and rebuildable read model for the nail design catalog matching; it must not be an authoritative data store. | Technical | High | Implemented |
| C-C010 | Projection mechanism | pgvector and Apache AGE projections must be populated after commit through durable Spring Modulith events with idempotent projectors and retries; Debezium must not be required by v1. | Technical | High | Implemented |
| C-C011 | Customer-facing web | Customer-facing web interfaces must be delivered through the same React/Vite application as the admin interface, with authenticated routes for customer-specific views. | Technical | High | Implemented |
