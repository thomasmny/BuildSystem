applyCommonConfiguration()

plugins {
    `java-library`
}

project.description = "API"

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.annotations)
}