# EMME — System Documentation

## Architecture

EMME is a **Spring Modulith** monolith deployed as a single application. Three client applications consume the backend:

| App | Folder | Audience | Actor Roles |
|---|---|---|---|
| **Admin** | `docs/admin/` | Platform administrators, system operators | Platform Administrator, System Operator |
| **Studio** | `docs/studio/` | Salon owners, managers, staff | Salon Owner, Salon Manager, Staff Member, Tenant Owner, Tenant Manager |
| **Client** | `docs/client/` | End customers | Customer, External Provider |

## Document Structure

Each app folder contains:

```
docs/{app}/
├── requirements.md        # Functional + non-functional requirements (FR-{A|S|C}###)
├── entity-model.md         # Subset of the global entity model
├── use_cases.puml          # App-specific PlantUML use case diagram
└── use-cases/
    ├── README.md           # Use case index + coverage map
    └── UC-XXX-name.md      # One spec per use case
```

## Global Artifacts

| File | Description |
|---|---|
| `docs/use_cases.puml` | System-level PlantUML diagram — all 28 use cases across all three apps |
| `docs/entity_model.md` | Complete entity model with Mermaid ER diagram |
| `docs/use_cases/` | **Superseded.** Original system-level specs before the three-app split. Per-app folders are the authoritative source. |

## Backend Modules → Apps Mapping

Each Spring Modulith module serves one or more apps:

| Module | Admin | Studio | Client |
|---|---|---|---|
| `appointments` | | :white_check_mark: | :white_check_mark: |
| `assistant` (AI + WhatsApp) | | :white_check_mark: | :white_check_mark: |
| `booking` | | | :white_check_mark: |
| `calendar` | | :white_check_mark: | :white_check_mark: |
| `catalog` | | :white_check_mark: | :white_check_mark: |
| `clients` | | :white_check_mark: | |
| `documents` | | :white_check_mark: | |
| `identity` | :white_check_mark: | :white_check_mark: | :white_check_mark: |
| `notification` | | :white_check_mark: | :white_check_mark: |
| `payment` | | :white_check_mark: | :white_check_mark: |
| `salon` | | :white_check_mark: | |
| `services` | | :white_check_mark: | |
| `staffing` | | :white_check_mark: | |
| `subscriptions` | :white_check_mark: | :white_check_mark: | |
| `tenancy` | :white_check_mark: | :white_check_mark: | :white_check_mark: |

## Use Case Numbering

Use cases share a **global numbering space** (UC-001 through UC-028). A use case may appear in multiple apps when it represents the same system capability viewed from different actor perspectives (e.g., UC-016 Process Payments exists in both Studio and Client).

## Business Rules

Business rules (BR-###) are scoped **per app**. BR numbers are unique within each app's use case folder but may overlap across apps. This mirrors the FR-{A|S|C}### requirement numbering convention.

## Conventions

- **UC IDs**: Globally unique (UC-001 – UC-028). Assigned sequentially across all three apps.
- **FR IDs**: Prefixed by app (FR-A###, FR-S###, FR-C###). Unique within each app.
- **BR IDs**: Scoped per app. Unique within each app's `use-cases/` folder.
- **Entity model**: Single global model at `docs/entity_model.md`. Per-app subsets reference it.
- **Status**: `Implemented` means the feature exists in the backend and has a verified path. `Draft` means spec is written but the code path is not yet fully verified.
