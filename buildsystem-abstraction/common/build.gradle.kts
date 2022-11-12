applyCommonConfiguration()

plugins {
    `java-library`
}

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.paperlib)
    compileOnly(libs.annotations)
}