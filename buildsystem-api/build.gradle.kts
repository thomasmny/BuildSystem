applyCommonConfiguration()

plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.annotations)
    compileOnly(libs.xseries)
}

publishing {
    publications {
        create<MavenPublication>("buildsystem-api") {
            from(components["java"])
        }
    }
}