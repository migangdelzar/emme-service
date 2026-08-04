# EMME Studio Requirements Catalog

| Field | Value |
|---|---|
| Source | `docs/vision.md`, `docs/entity_model.md` |
| Audience | Tenant owners, salon managers, staff members, artists |
| Scope | v1 Spring Modulith — salon operations app (emme-web) |
| Date | 2026-08-04 |

## Functional Requirements

### Authentication & Access

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S001 | Sign in | As a studio user, I want to authenticate through Keycloak so that I can access my tenant workspace. | High | Implemented |
| FR-S002 | Sign out | As a studio user, I want to end my authenticated session so that access from my device is revoked. | High | Implemented |
| FR-S003 | Select tenant | As a user with multiple memberships, I want to select an active tenant so that my actions apply to the intended business. | High | Implemented |
| FR-S004 | Resolve tenant from host | As a studio user, I want EMME to resolve my tenant from the approved subdomain so that I enter the correct workspace without manual selection. | High | Implemented |
| FR-S005 | View current identity | As a studio user, I want to view my profile, memberships, role, and permissions so that I understand my current access. | High | Implemented |
| FR-S006 | See permission-denied states | As a staff member, I want unauthorized actions shown as unavailable rather than broken so that I understand my access boundaries. | High | Implemented |

### Onboarding

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S007 | Complete onboarding | As a new tenant owner, I want a guided walkthrough to configure initial business details, hours, services, and calendar connections so that the salon can begin operating. | High | Implemented |

### Business Configuration

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S008 | Manage business profile | As a tenant owner, I want to configure business name, contact details, address, biography, and branding so that the salon identity is accurate. | High | Implemented |
| FR-S009 | Manage operating hours | As a tenant owner, I want to set per-day opening and closing times so that booking slots reflect actual availability. | High | Implemented |
| FR-S010 | Manage break times | As a tenant owner, I want to define break periods within each operating day so that appointments are not scheduled during staff breaks. | Medium | Implemented |
| FR-S011 | Configure booking policy | As a tenant owner, I want to set minimum notice, maximum advance, cancellation window, and slot interval so that scheduling rules match salon policy. | High | Implemented |
| FR-S012 | Manage special dates | As a tenant owner, I want to define holidays, reduced-hour days, and vacation periods so that the calendar reflects exceptions. | Medium | Implemented |
| FR-S013 | Manage notification preferences | As a tenant owner, I want to enable or disable notification channels and configure reminder timing so that communication follows salon policy. | Medium | Implemented |
| FR-S014 | Configure WhatsApp bot | As a tenant owner, I want to enable auto-reply, set welcome messages, and define keyword-response pairs so that automated customer interactions are configured. | Medium | Implemented |
| FR-S015 | Manage promotions | As a tenant owner, I want to create time-limited service promotions with discount type and value so that special offers can be run. | Medium | Implemented |
| FR-S016 | Select theme | As a studio user, I want to switch between light, dark, and system themes so that the interface matches my preference. | Low | Implemented |
| FR-S017 | Select language | As a studio user, I want to switch the interface language between Spanish and English so that I can work in my preferred language. | Medium | Implemented |

### Service Catalog

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S018 | View service catalog | As a salon manager, I want to list and filter nail services by category so that I can find current offerings. | High | Implemented |
| FR-S019 | Create service | As a salon manager, I want to create a nail service with name, duration, category, description, and price so that it can be offered to customers. | High | Implemented |
| FR-S020 | Update service | As a salon manager, I want to update a nail service's details so that the catalog remains accurate. | High | Implemented |
| FR-S021 | Retire service | As a salon manager, I want to retire a nail service without corrupting appointment history so that it cannot be selected for new bookings. | High | Implemented |
| FR-S022 | Search services | As a salon user, I want to search services by name or description so that I can quickly find specific offerings. | Medium | Implemented |
| FR-S023 | Toggle service active | As a salon manager, I want to enable or disable a service with one action so that I can quickly hide or restore offerings. | Medium | Implemented |

### Artist Management

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S024 | View artists | As a salon manager, I want to list tenant artists with their capabilities so that I can manage staff assignments. | High | Implemented |
| FR-S025 | Create artist | As a salon manager, I want to create an artist profile so that appointments can be assigned to them. | High | Implemented |
| FR-S026 | Update artist | As a salon manager, I want to update artist details so that staff information stays current. | High | Implemented |
| FR-S027 | Deactivate artist | As a salon manager, I want to deactivate an artist so that they cannot be assigned to new appointments while preserving their history. | Medium | Implemented |
| FR-S028 | Add artist capability | As a salon manager, I want to associate an artist with a service they can perform so that booking suggestions use qualified staff. | Medium | Implemented |
| FR-S029 | Remove artist capability | As a salon manager, I want to remove a service capability from an artist so that the assignment reflects actual qualifications. | Medium | Implemented |

### Customer Management

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S030 | View customers | As a staff member, I want to list and paginate tenant customers so that I can browse client records. | High | Implemented |
| FR-S031 | Search customers | As a staff member, I want to search customers by name or phone so that I can quickly find a specific client. | High | Implemented |
| FR-S032 | Filter customers | As a staff member, I want to filter customers by loyalty tier, VIP status, or activity so that I can target specific segments. | Medium | Implemented |
| FR-S033 | Create customer | As a staff member, I want to create a customer profile with name, phone, email, birthday, allergies, preferences, and VIP status so that appointments can be associated with the customer. | High | Implemented |
| FR-S034 | Update customer | As a staff member, I want to update contact details, notes, allergies, preferences, VIP status, and birthday so that service remains accurate and safe. | High | Implemented |
| FR-S035 | Retire customer | As a salon manager, I want to retire a customer profile according to retention policy so that it is unavailable for new operations without losing required history. | Medium | Implemented |
| FR-S036 | View customer history | As a staff member, I want to view a customer's visits, spending, and appointment history so that I can provide informed and personalized service. | High | Implemented |
| FR-S037 | Contact customer via WhatsApp | As a staff member, I want to open a WhatsApp conversation with a customer from their profile so that I can communicate quickly. | Medium | Implemented |

### Appointment Management

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S038 | View appointments | As a staff member, I want to view appointments by date in list, day, week, and month calendar views so that I can manage the schedule. | High | Implemented |
| FR-S039 | Search available slots | As a staff member, I want to find available time slots using salon hours, service duration, artist capability, and existing bookings so that I can choose a valid time. | High | Implemented |
| FR-S040 | Create appointment | As a staff member, I want to create an appointment for a customer with a service, artist, date, and time so that the booking is recorded. | High | Implemented |
| FR-S041 | Prevent booking collision | As a salon owner, I want concurrent requests for the same slot serialized so that double booking cannot occur. | High | Implemented |
| FR-S042 | Reschedule appointment | As a staff member, I want to move an appointment to another valid slot so that schedule changes are supported. | High | Implemented |
| FR-S043 | Cancel appointment | As a staff member, I want to cancel an eligible appointment so that the slot is released and the cancellation is recorded. | High | Implemented |
| FR-S044 | Confirm appointment | As a staff member, I want to confirm a pending appointment so that the customer's slot is reserved. | High | Implemented |
| FR-S045 | Start appointment | As a staff member, I want to mark an appointment as in progress so that the operational state reflects the service being performed. | High | Implemented |
| FR-S046 | Complete appointment | As a staff member, I want to mark an appointment as completed so that revenue is recorded and the slot is cleared. | High | Implemented |
| FR-S047 | Mark no-show | As a staff member, I want to mark an appointment as a no-show so that missed bookings are tracked. | High | Implemented |
| FR-S048 | View appointment detail | As a staff member, I want to see customer info, service, artist, time, price, notes, allergies, and status so that I can fulfill the booking with full context. | High | Implemented |
| FR-S049 | Send WhatsApp reminder | As a staff member, I want to send a WhatsApp reminder to the customer from the appointment detail so that missed appointments are reduced. | Medium | Implemented |
| FR-S050 | Generate calendar link | As a staff member, I want to generate a Google Calendar link for an appointment so that the customer can add it to their calendar. | Medium | Implemented |

### Dashboard

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S051 | View dashboard KPIs | As a salon manager, I want to see daily income, confirmed appointments, occupancy rate, and new clients so that I can assess today's performance at a glance. | High | Implemented |
| FR-S052 | View today's agenda | As a salon manager, I want to see upcoming appointments for today with client and service details so that I can prepare for the day. | High | Implemented |
| FR-S053 | View appointment detail from dashboard | As a salon manager, I want to click an appointment in the dashboard to see full details and manage its status so that I can act without navigating away. | High | Implemented |
| FR-S054 | Access quick actions | As a salon manager, I want one-click access to create appointments, add clients, and register services from the dashboard so that frequent tasks are efficient. | High | Implemented |

### Financial Analytics

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S055 | View monthly revenue | As a salon owner, I want to see total revenue for a selected month with navigation between months so that I can review period performance. | High | Implemented |
| FR-S056 | View weekly comparison | As a salon owner, I want to compare current week revenue vs previous week in a bar chart so that I can spot trends. | High | Implemented |
| FR-S057 | View revenue trend | As a salon owner, I want to see revenue over the last 6 months in an area chart so that I can identify growth patterns. | High | Implemented |
| FR-S058 | View revenue by category | As a salon owner, I want to see revenue broken down by service category in a pie chart so that I know which categories drive income. | Medium | Implemented |
| FR-S059 | View top services | As a salon owner, I want to see the highest-earning services for the month so that I can optimize the catalog. | Medium | Implemented |
| FR-S060 | View daily transactions | As a salon owner, I want to see a list of completed appointments with revenue per transaction for the month so that I can reconcile daily earnings. | High | Implemented |
| FR-S061 | Set monthly goal | As a salon owner, I want to set and track a monthly revenue goal with a progress bar so that I can measure performance against targets. | Medium | Implemented |
| FR-S062 | View advanced stats | As a salon owner, I want to see average ticket, growth rate, retention rate, busiest day, and peak hour so that I can make data-driven decisions. | Medium | Implemented |
| FR-S063 | Export finances | As a salon owner, I want to export financial summaries so that I can perform approved external analysis. | Medium | Implemented |

### Google Workspace Integration

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S064 | Connect Google account | As a salon owner, I want to authorize Google OAuth so that calendar sync and sheets export can operate. | Medium | Implemented |
| FR-S065 | Disconnect Google account | As a salon owner, I want to revoke Google OAuth access so that integration permissions are removed. | Medium | Implemented |
| FR-S066 | View Google OAuth status | As a salon owner, I want to see the connected Google account email and connection status so that I know the integration state. | Medium | Implemented |
| FR-S067 | Sync appointments to Google Calendar | As a salon owner, I want EMME appointments synced to Google Calendar automatically so that my schedule is visible in my primary calendar. | Medium | Implemented |
| FR-S068 | Trigger manual sync | As a salon owner, I want to manually trigger a calendar sync so that I can force an immediate update. | Medium | Implemented |
| FR-S069 | Export to Google Sheets | As a salon owner, I want to export appointments, clients, or full business data to Google Sheets so that I can use approved external reporting tools. | Medium | Implemented |
| FR-S070 | Re-export to spreadsheet | As a salon owner, I want to update a previously exported spreadsheet with current data so that shared reports stay fresh. | Medium | Implemented |
| FR-S071 | List spreadsheets | As a salon owner, I want to see previously exported spreadsheets so that I can re-export or reference them. | Medium | Implemented |

### Calendar Sync State

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S072 | View sync status | As a salon owner, I want to view the last known calendar sync status so that I can diagnose integration issues. | Medium | Implemented |
| FR-S073 | View calendar event links | As a salon owner, I want to see which appointments are linked to external calendar events so that I can verify sync accuracy. | Medium | Implemented |

### Subscription & Payments

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S074 | View subscription | As a tenant owner, I want to view my current subscription plan, entitlements, status, and billing period so that I understand available capabilities. | High | Implemented |
| FR-S075 | Change plan | As a tenant owner, I want to change my subscription plan so that capabilities match my business needs. | Medium | Implemented |
| FR-S076 | Initiate payment | As a tenant owner, I want to initiate a payment through the configured provider so that an eligible charge can be completed. | Medium | Implemented |
| FR-S077 | View payment | As a tenant owner, I want to view payment details and status so that I can verify transaction state. | Medium | Implemented |
| FR-S078 | List payments | As a tenant owner, I want to list tenant payments so that I can reconcile billing activity. | Medium | Implemented |
| FR-S079 | Request refund | As a tenant owner, I want to request a refund for an eligible payment so that failed or cancelled transactions can be corrected. | Medium | Implemented |

### Knowledge Management

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S080 | Upload knowledge document | As a tenant manager, I want to upload a knowledge document so that tenant-specific information can be indexed for AI responses. | High | Implemented |
| FR-S081 | View document processing status | As a tenant manager, I want to track document ingestion status and see errors so that failed sources can be corrected. | High | Implemented |
| FR-S082 | View document chunks | As a tenant manager, I want to view chunk-level retrieval content so that I can verify indexed knowledge accuracy. | Medium | Implemented |
| FR-S083 | Retire knowledge document | As a tenant manager, I want to retire a knowledge document so that outdated content is no longer retrieved. | Medium | Implemented |

### Conversation Monitoring

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S084 | View conversations | As a staff member, I want to list tenant conversations by channel and status so that I can review customer interactions. | High | Implemented |
| FR-S085 | View conversation detail | As a staff member, I want to read the full conversation history with events so that I can understand the customer's context. | High | Implemented |
| FR-S086 | Close conversation | As a staff member, I want to close a completed conversation so that it is archived. | Medium | Implemented |
| FR-S087 | View pending actions | As a staff member, I want to see booking or payment actions awaiting customer confirmation so that I can assist with stalled flows. | Medium | Implemented |

### Notifications

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S088 | View notification history | As a staff member, I want to list sent notifications with delivery status so that I can review communication activity. | Medium | Implemented |
| FR-S089 | Send appointment reminder | As a salon owner, I want scheduled appointment reminders sent automatically according to tenant settings so that missed appointments are reduced. | Medium | Implemented |

### Nail Design Catalog

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S090 | Create catalog item | As a salon manager, I want to add a nail design to the catalog with name and images so that customers can browse inspiration. | Medium | Implemented |
| FR-S091 | Browse catalog items | As a studio user, I want to browse the nail design catalog so that I can reference designs during consultations. | Medium | Implemented |
| FR-S092 | Add catalog image | As a salon manager, I want to add images to a catalog item so that designs are visually represented. | Medium | Implemented |
| FR-S093 | Delete catalog item | As a salon manager, I want to remove an outdated catalog item so that the catalog stays current. | Medium | Implemented |

### Data Management

| ID | Title | User Story | Priority | Status |
|---|---|---|---|---|
| FR-S094 | Export data backup | As a tenant owner, I want to download all business data as a JSON backup file so that I have an offline copy of my salon records. | Medium | Implemented |
| FR-S095 | Clear local cache | As a studio user, I want to purge the browser cache and reload so that stale data does not affect my view. | Low | Implemented |
| FR-S096 | Delete account | As a tenant owner, I want to request account deletion with multi-step confirmation safeguards so that irreversible destruction is intentional. | Low | Implemented |

## Non-Functional Requirements

| ID | Title | Requirement | Category | Priority | Status |
|---|---|---|---|---|---|
| NFR-S001 | API read latency | Under the agreed Locust baseline load, 95% of non-AI API reads must complete within 500 ms and 99% within 1,500 ms. | Performance | High | Open |
| NFR-S002 | API write latency | Under the agreed Locust baseline load, 95% of non-provider API writes must complete within 1,000 ms and 99% within 2,500 ms. | Performance | High | Open |
| NFR-S003 | Baseline concurrency | The production profile must sustain 100 concurrent active studio users with less than 1% server errors during a 15-minute Locust test. | Scalability | Medium | Open |
| NFR-S004 | Availability | The studio application must achieve 99.5% monthly availability excluding announced maintenance. | Availability | High | Open |
| NFR-S005 | Idempotency | Replaying the same booking request or status update 10 times must produce no more than one durable business effect. | Reliability | High | Implemented |
| NFR-S006 | Booking collision prevention | Concurrent appointment requests for the same artist and time slot must be serialized so that double booking cannot occur. | Reliability | High | Implemented |
| NFR-S007 | Accessibility | All studio frontend flows must satisfy WCAG 2.2 AA automated checks with zero critical violations. | Usability | Medium | Open |
| NFR-S008 | Browser support | The studio web app must support the latest 2 stable versions of Chrome, Firefox, Safari, and Edge. | Portability | Medium | Open |
| NFR-S009 | Offline resilience | The studio app must display cached data and graceful degradation when the backend is unreachable, without losing pending user input. | Reliability | Medium | Implemented |
| NFR-S010 | State consistency | The studio app must show loading, empty, error, and success states explicitly for every data-dependent view; no blank screens on failure. | Usability | High | Implemented |
| NFR-S011 | Session expiry | When the user's session expires, the studio app must redirect to login and preserve no sensitive data in URL or local state. | Security | High | Implemented |
| NFR-S012 | Keyboard accessibility | All interactive studio UI elements must be reachable and operable via keyboard navigation. | Usability | Medium | Open |
| NFR-S013 | Screen reader support | All studio UI elements must have accessible names, roles, and error associations for screen readers. | Usability | Medium | Open |
| NFR-S014 | Responsive layout | The studio app must be usable on viewports from 320 px to 2560 px width without horizontal scrolling or content loss. | Usability | Medium | Open |
| NFR-S015 | Frontend test coverage | Critical studio user journeys must have Playwright coverage; unit tests must cover validation, reducers, and adapter logic. | Maintainability | High | Implemented |

## Constraints

| ID | Title | Constraint | Category | Priority | Status |
|---|---|---|---|---|---|
| C-S001 | Frontend framework | The studio app must use React 19 and Vite as the bundler. | Technical | High | Implemented |
| C-S002 | Package manager | The studio workspace must use Bun as the package manager and test runner. | Technical | High | Implemented |
| C-S003 | Type safety | The studio codebase must use TypeScript with strict mode; `any` types are prohibited without a documented external-boundary reason. | Technical | High | Implemented |
| C-S004 | API consumption | Studio features must consume typed contracts through `@emme/contracts` and `@emme/api-client`; direct `fetch` calls from components are prohibited. | Technical | High | Implemented |
| C-S005 | Contract independence | `@emme/contracts` must have zero dependency on `@emme/api-client`; contracts define transport types, the client implements HTTP execution. | Technical | High | Implemented |
| C-S006 | Server state | Remote data must be managed through TanStack Query; server data must not be duplicated in client-side state stores. | Technical | High | Implemented |
| C-S007 | Client state | Cross-feature UI state must use Zustand with reducer-style actions; component-local transient state stays in React local state. | Technical | High | Implemented |
| C-S008 | Feature organization | Studio features must follow Feature-Sliced Design; each feature owns its user flows, query hooks, view models, and UI mapping. | Technical | High | Implemented |
| C-S009 | i18n | All user-visible strings must be externalized through `@emme/i18n` with typed translation keys; direct `react-i18next` imports from features are prohibited. | Technical | High | Implemented |
| C-S010 | Supported locales | The studio app must support Spanish (es-MX, default) and English (en-US); both locales must have 100% translation key coverage. | Technical | High | Implemented |
| C-S011 | Security boundary | Frontend route guards improve UX but are not security controls; all authorization decisions must be enforced on the backend. | Technical | High | Implemented |
| C-S012 | Public configuration | Browser-exposed `VITE_*` variables must contain only public configuration; private keys, tokens, and secrets must never be placed in build-time environment variables. | Security | High | Implemented |
| C-S013 | Token storage | Browser token storage must follow an approved ADR with documented threat model and controls; localStorage persistence requires explicit approval. | Security | High | Implemented |
| C-S014 | Service worker | The service worker must not cache authenticated API responses or sensitive content; `/api` routes must be excluded from SW fallback caching. | Security | High | Implemented |
| C-S015 | Frontend tests | Unit and component tests must use Vitest and Testing Library; critical user journeys must have Playwright coverage. | Technical | High | Implemented |
| C-S016 | E2E isolation | E2E tests must be deterministic, isolated, and free of committed recordings; mock and real provider lanes must be explicitly selected. | Technical | High | Implemented |
| C-S017 | Container image | The production web image must use a multi-stage build with Bun for build and Nginx for serving; runtime must run as non-root. | Technical | High | Implemented |
| C-S018 | Static delivery | The web image must serve static assets and SPA routes; `/health` must be available without exposing application data. | Technical | High | Implemented |
| C-S019 | Frontend preservation | The existing React/Vite frontend and shared TypeScript packages must be retained and adapted to the unified backend; no rewrite. | Technical | High | Implemented |
| C-S020 | Dependency direction | Features must depend on capability API contracts → shared HTTP client → browser transport; the contracts package must never import the concrete API client package. | Technical | High | Implemented |
| C-S021 | Error handling | Backend error codes must be stable enough for frontend behavior branching; human-readable messages from the backend are diagnostic context, not presentation copy. | Technical | High | Implemented |
| C-S022 | Sensitive data | Sensitive customer data must not be exposed in URLs, analytics events, browser logs, or persisted caches. | Security | High | Implemented |
| C-S023 | Duplicate mutation | Mutations must be idempotent or guarded against duplicate submission while a command is pending. | Technical | High | Implemented |
| C-S024 | Stale responses | Pending requests must be cancelled or ignored when route parameters or selected tenants change. | Technical | High | Implemented |
