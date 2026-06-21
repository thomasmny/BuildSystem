applyCommonConfiguration()

plugins {
    id("java")
}

project.description = "API consumer used as a CI guard against breaking API changes"

dependencies {
    // compileOnly mirrors how a real third-party plugin depends on the API: BuildSystem provides it at runtime.
    compileOnly(project(":buildsystem-api"))
    compileOnly(libs.spigot)
    compileOnly(libs.xseries)
    compileOnly(libs.annotations)
    compileOnly(libs.jspecify)
}
