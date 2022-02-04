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

val abstract = configurations.create("abstract") {
    description = "Adapters to include in the JAR"
    isCanBeConsumed = false
    isCanBeResolved = true
    shouldResolveConsistentlyWith(configurations["runtimeClasspath"])
}

dependencies {
    project.project(":buildsystem-abstraction").subprojects.forEach {
        implementation(project(it.path))
    }

    compileOnly("org.spigotmc:spigot-api:1.18-rc3-R0.1-SNAPSHOT")
    compileOnly("com.mojang:authlib:1.5.25")
    compileOnly("net.luckperms:api:5.3")
    compileOnly("me.clip:placeholderapi:2.11.1")
    compileOnly("com.sk89q.worldedit:worldedit-core:7.2.0-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:23.0.0")

    implementation("com.github.cryptomorin:XSeries:8.6.1")
    implementation("fr.mrmicky:fastboard:1.2.1")
    implementation("org.bstats:bstats-bukkit:3.0.0")
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

        fun reloc(pkg: String) = relocate(pkg, "com.eintosti.buildsystem.util.external.$pkg")
        reloc("com.cryptomorin.xseries")
        reloc("fr.mrmicky.fastboard")
        reloc("org.bstats")
    }
}