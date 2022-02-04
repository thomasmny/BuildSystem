applyCoreConfiguration()

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

project.description = "Core"

repositories {
    maven {
        name = "AuthLib"
        url = uri("https://libraries.minecraft.net/")
    }
    maven {
        name = "EngineHub"
        url = uri("https://maven.enginehub.org/repo/")
    }
    maven {
        name = "PlaceholderAPI"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

dependencies {
    api(project(":buildsystem-api"))

    project.project(":buildsystem-abstraction").subprojects.forEach {
        implementation(project(it.path))
    }

    compileOnly(libs.spigot)
    compileOnly(libs.authlib)
    compileOnly(libs.luckperms)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.worldedit)
    compileOnly(libs.annotations)

    implementation(libs.xseries)
    implementation(libs.fastboard)
    implementation(libs.bstats)
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    shadowJar {
        archiveFileName.set("${rootProject.name}-${project.version}.jar")

        dependsOn(project.project(":buildsystem-abstraction").subprojects.map {
            it.tasks.named("assemble")
        })

        val shadePath = "com.eintosti.buildsystem.util.external"
        relocate("com.cryptomorin.xseries", "$shadePath.xseries")
        relocate("fr.mrmicky.fastboard", "$shadePath.fastboard")
        relocate("org.bstats", "$shadePath.bstats")
    }

    processResources {
        from(sourceSets.main.get().resources.srcDirs) {
            filesMatching("plugin.yml") {
                expand("version" to project.version)
            }
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}