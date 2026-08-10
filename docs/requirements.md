# EMME — Unified Requirements Catalog

Aggregated from per-app requirements. See `docs/admin/requirements.md`, `docs/studio/requirements.md`, and `docs/client/requirements.md` for detailed user stories and NFRs.

## Functional Requirements Summary

| App | Count | Range |
|---|---|---|
| Admin | 22 | FR-A001 – FR-A022 |
| Studio | 96 | FR-S001 – FR-S096 |
| Client | 37 | FR-C001 – FR-C037 |
| **Total** | **155** | |

### Admin (FR-A)

| ID | Title | UC |
|---|---|---|
| FR-A001 | Create tenant | UC-002 |
| FR-A002 | View tenant | UC-002 |
| FR-A003 | List tenants | UC-002 |
| FR-A004 | Update tenant | UC-002 |
| FR-A005 | Suspend tenant | UC-002 |
| FR-A006 | Reactivate tenant | UC-002 |
| FR-A007 | Stage tenant deletion | UC-002 |
| FR-A008 | View tenant health | UC-002 |
| FR-A009 | Request tenant provisioning | UC-002 |
| FR-A010 | View provisioning status | UC-002 |
| FR-A011 | Manage feature flags | UC-019 |
| FR-A012 | Override tenant features | UC-019 |
| FR-A013 | View tenant effective features | UC-019 |
| FR-A014 | View memberships | UC-020 |
| FR-A015 | Assign membership | UC-020 |
| FR-A016 | Revoke membership | UC-020 |
| FR-A017 | View user permissions | UC-020 |
| FR-A018 | Require administrator MFA | UC-001 |
| FR-A019 | View subscription | UC-002 |
| FR-A020 | Enforce entitlement | UC-002 |
| FR-A021 | Audit platform events | UC-017 |
| FR-A022 | Reconcile derived models | UC-018 |

### Studio (FR-S)

| ID | Title | UC |
|---|---|---|
| FR-S001 | Sign in | UC-001 |
| FR-S002 | Sign out | UC-001 |
| FR-S003 | Select tenant | UC-001 |
| FR-S004 | Resolve tenant from host | UC-001 |
| FR-S005 | View current identity | UC-001 |
| FR-S006 | See permission-denied states | UC-001 |
| FR-S007 | Complete onboarding | UC-021 |
| FR-S008 | Manage business profile | UC-007 |
| FR-S009 | Manage operating hours | UC-007 |
| FR-S010 | Manage break times | UC-007 |
| FR-S011 | Configure booking policy | UC-007 |
| FR-S012 | Manage special dates | UC-007 |
| FR-S013 | Manage notification preferences | UC-007 |
| FR-S014 | Configure WhatsApp bot | UC-007 |
| FR-S015 | Manage promotions | UC-007 |
| FR-S016 | Select theme | UC-007 |
| FR-S017 | Select language | UC-007 |
| FR-S018 | View service catalog | UC-004 |
| FR-S019 | Create service | UC-004 |
| FR-S020 | Update service | UC-004 |
| FR-S021 | Retire service | UC-004 |
| FR-S022 | Search services | UC-004 |
| FR-S023 | Toggle service active | UC-004 |
| FR-S024 | View artists | UC-004 |
| FR-S025 | Create artist | UC-004 |
| FR-S026 | Update artist | UC-004 |
| FR-S027 | Deactivate artist | UC-004 |
| FR-S028 | Add artist capability | UC-004 |
| FR-S029 | Remove artist capability | UC-004 |
| FR-S030 | View customers | UC-005 |
| FR-S031 | Search customers | UC-005 |
| FR-S032 | Filter customers | UC-005 |
| FR-S033 | Create customer | UC-005 |
| FR-S034 | Update customer | UC-005 |
| FR-S035 | Retire customer | UC-005 |
| FR-S036 | View customer history | UC-005 |
| FR-S037 | Contact customer via WhatsApp | UC-005 |
| FR-S038 | View appointments | UC-006 |
| FR-S039 | Search available slots | UC-006 |
| FR-S040 | Create appointment | UC-006 |
| FR-S041 | Prevent booking collision | UC-006 |
| FR-S042 | Reschedule appointment | UC-006 |
| FR-S043 | Cancel appointment | UC-006 |
| FR-S044 | Confirm appointment | UC-006 |
| FR-S045 | Start appointment | UC-006 |
| FR-S046 | Complete appointment | UC-006 |
| FR-S047 | Mark no-show | UC-006 |
| FR-S048 | View appointment detail | UC-006 |
| FR-S049 | Send WhatsApp reminder | UC-006 |
| FR-S050 | Generate calendar link | UC-006 |
| FR-S051 | View dashboard KPIs | UC-003 |
| FR-S052 | View today's agenda | UC-003 |
| FR-S053 | View appointment detail from dashboard | UC-003 |
| FR-S054 | Access quick actions | UC-003 |
| FR-S055 | View monthly revenue | UC-003 |
| FR-S056 | View weekly comparison | UC-003 |
| FR-S057 | View revenue trend | UC-003 |
| FR-S058 | View revenue by category | UC-003 |
| FR-S059 | View top services | UC-003 |
| FR-S060 | View daily transactions | UC-003 |
| FR-S061 | Set monthly goal | UC-003 |
| FR-S062 | View advanced stats | UC-003 |
| FR-S063 | Export finances | UC-003 |
| FR-S064 | Connect Google account | UC-022 |
| FR-S065 | Disconnect Google account | UC-022 |
| FR-S066 | View Google OAuth status | UC-022 |
| FR-S067 | Sync appointments to Google Calendar | UC-022 |
| FR-S068 | Trigger manual sync | UC-022 |
| FR-S069 | Export to Google Sheets | UC-022 |
| FR-S070 | Re-export to spreadsheet | UC-022 |
| FR-S071 | List spreadsheets | UC-022 |
| FR-S072 | View sync status | UC-022 |
| FR-S073 | View calendar event links | UC-022 |
| FR-S074 | View subscription | UC-015 |
| FR-S075 | Change plan | UC-015 |
| FR-S076 | Initiate payment | UC-016 |
| FR-S077 | View payment | UC-016 |
| FR-S078 | List payments | UC-016 |
| FR-S079 | Request refund | UC-016 |
| FR-S080 | Upload knowledge document | UC-013 |
| FR-S081 | View document processing status | UC-013 |
| FR-S082 | View document chunks | UC-013 |
| FR-S083 | Retire knowledge document | UC-013 |
| FR-S084 | View conversations | UC-012 |
| FR-S085 | View conversation detail | UC-012 |
| FR-S086 | Close conversation | UC-012 |
| FR-S087 | View pending actions | UC-012 |
| FR-S088 | View notification history | UC-014 |
| FR-S089 | Send appointment reminder | UC-014 |
| FR-S090 | Create catalog item | UC-023 |
| FR-S091 | Browse catalog items | UC-023 |
| FR-S092 | Add catalog image | UC-023 |
| FR-S093 | Delete catalog item | UC-023 |
| FR-S094 | Export data backup | UC-024 |
| FR-S095 | Clear local cache | UC-024 |
| FR-S096 | Delete account | UC-024 |

### Client (FR-C)

| ID | Title | UC |
|---|---|---|
| FR-C001 | Customer sign in | UC-001 |
| FR-C002 | Update customer profile | UC-001 |
| FR-C003 | Receive WhatsApp message | UC-008 |
| FR-C004 | Use web chat | UC-008 |
| FR-C005 | Verify WhatsApp webhook | UC-008 |
| FR-C006 | Normalize channel messages | UC-008 |
| FR-C007 | Deduplicate inbound message | UC-008 |
| FR-C008 | Transcribe voice input | UC-009 |
| FR-C009 | Analyze image input | UC-009 |
| FR-C010 | Combine multimodal input | UC-009 |
| FR-C011 | Detect conversation intent | UC-010 |
| FR-C012 | Recommend services | UC-010 |
| FR-C013 | Estimate price | UC-010 |
| FR-C014 | Answer policy question | UC-010 |
| FR-C015 | Retrieve tenant knowledge | UC-010 |
| FR-C016 | Handle AI provider failure | UC-010 |
| FR-C017 | Draft conversational booking | UC-011 |
| FR-C018 | Propose booking action | UC-011 |
| FR-C019 | Confirm consequential action | UC-011 |
| FR-C020 | Reject proposed action | UC-011 |
| FR-C021 | Resume pending confirmation | UC-011 |
| FR-C022 | Cancel expired confirmation | UC-011 |
| FR-C023 | Search available slots | UC-006 (studio) |
| FR-C024 | Create appointment | UC-006 (studio) |
| FR-C025 | Reschedule appointment | UC-006 (studio) |
| FR-C026 | Cancel appointment | UC-006 (studio) |
| FR-C027 | Store conversation history | UC-008 |
| FR-C028 | Summarize conversation | UC-008 |
| FR-C029 | View conversation events | UC-008 |
| FR-C030 | Receive WhatsApp response | UC-014 (studio) |
| FR-C031 | Receive appointment reminder | UC-014 (studio) |
| FR-C032 | Sync appointment to calendar | UC-026 |
| FR-C033 | Unsync appointment from calendar | UC-026 |
| FR-C034 | Browse service catalog | UC-025 |
| FR-C035 | Match nail design | UC-025 |
| FR-C036 | Browse nail designs | UC-025 |
| FR-C037 | Customer sign in with social identity | UC-027 |
