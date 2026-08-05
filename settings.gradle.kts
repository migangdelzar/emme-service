pluginManagement {
    includeBuild("build-logic")
    includeBuild("build-logic-settings")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.emme.environment-settings")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

buildCache {
    local {
        directory = File(rootDir, ".gradle/build-cache")
    }
}

rootProject.name = "emme-service"

// ── Platform ──
include(":platform")

// ── Application ──
include(":applications:emme-platform")

// ── Business Modules ──
include(":modules:shared")
include(":modules:tenancy")
include(":modules:identity")
include(":modules:studio")
include(":modules:clients")
include(":modules:staffing")
include(":modules:catalog")
include(":modules:booking")
include(":modules:calendar")
include(":modules:notification")
include(":modules:payment")
include(":modules:assistant")
include(":modules:audit")

// ── Libraries ──
include(":libraries:functional")
include(":libraries:kernel")
include(":libraries:testing")
include(":libraries:test-containers")

// ── Database ──
include(":database")
include(":libraries:observability-support")

// ── Operational tools ──
include(":tools:e2e-provisioner")
