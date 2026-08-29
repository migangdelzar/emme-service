plugins {
    id("emme.java-library")
}

group = "com.emme"

// Provider-neutral AI capability contracts. This library must remain framework-independent.

dependencies {
    api(project(":libraries:kernel"))
}
