plugins {
    id("emme.java-library")
}

group = "com.emme"

dependencies {
    api(project(":libraries:functional"))
    implementation("org.slf4j:slf4j-api:2.0.17")
}
