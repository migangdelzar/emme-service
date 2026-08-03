pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
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
include(":modules:customer")
include(":modules:workforce")
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
