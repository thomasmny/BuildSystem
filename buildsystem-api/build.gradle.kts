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

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    repositories {
        maven {
            name = "BuildSystem"
            url = if (project.version.toString().endsWith("-SNAPSHOT")) {
                uri("https://repo.eintosti.de/snapshots")
            } else {
                uri("https://repo.eintosti.de/release")
            }
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "de.eintosti"
            artifactId = "buildsystem-api"
            version = project.version.toString()
            from(components["java"])
        }
    }
}