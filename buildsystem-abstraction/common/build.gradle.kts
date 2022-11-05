applyCommonConfiguration()

plugins {
    java
}

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.paperlib)
    compileOnly(libs.annotations)
}