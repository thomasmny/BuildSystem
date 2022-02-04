rootProject.name = "BuildSystem"

include("buildsystem-core")

include("buildsystem-abstraction:adapter-1_12")
include("buildsystem-abstraction:adapter-1_13")
include("buildsystem-abstraction:adapter-1_14")
include("buildsystem-abstraction:common")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
include("buildsystem-abstraction:common")
findProject(":buildsystem-abstraction:common")?.name = "common"
