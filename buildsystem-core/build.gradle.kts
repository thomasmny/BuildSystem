applyCoreConfiguration()

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
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
    compileOnly(libs.paperlib)
    compileOnly(libs.luckperms)
    compileOnly(libs.placeholderapi) { isTransitive = false }
    compileOnly(libs.worldedit)
    compileOnly(libs.annotations)

    implementation(libs.paperlib)
    implementation(libs.adventure.api)
    implementation(libs.adventure.minimessage)
    implementation(libs.adventure.platform)
    implementation(libs.xseries) { isTransitive = false }
    implementation(libs.fastboard)
    implementation(libs.nbt) { isTransitive = false }
    implementation(libs.bstats)
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    shadowJar {
        dependsOn(project.project(":buildsystem-abstraction").subprojects.map {
            it.tasks.named("assemble")
        })

        minimize()
        archiveFileName.set("${rootProject.name}-${project.version}.jar")

        val shadePath = "de.eintosti.buildsystem.util.external"
        relocate("com.cryptomorin.xseries", "$shadePath.xseries")
        relocate("dev.dewy.nbt", "$shadePath.nbt")
        relocate("fr.mrmicky.fastboard", "$shadePath.fastboard")
        relocate("io.papermc.lib", "$shadePath.paperlib")
        relocate("org.bstats", "$shadePath.bstats")
        relocate("net.kyori.adventure", "$shadePath.adventure")
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