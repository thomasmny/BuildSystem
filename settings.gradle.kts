rootProject.name = "BuildSystem"

include("buildsystem-api")
include("buildsystem-core")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}