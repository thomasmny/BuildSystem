applyCoreConfiguration()

plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.6"
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

    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
    }

    modrinthMavenWorkaround(
        "axiom-paper-plugin",
        "4.0.1-1.21.1",
        "AxiomPaper-4.0.1-for-MC1.21.1.jar"
    )
}

dependencies {
    project.project(":buildsystem-abstraction").subprojects.forEach {
        implementation(project(it.path))
    }

    compileOnly(libs.spigot)
    compileOnly(libs.authlib)
    compileOnly(libs.paperlib)
    compileOnly(libs.luckperms)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.worldedit)
    compileOnly(libs.annotations)
    compileOnly(libs.axiompaper)

    implementation(libs.paperlib)
    implementation(libs.xseries)
    implementation(libs.fastboard)
    implementation(libs.nbt) { isTransitive = false }
    implementation(libs.bstats)
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    shadowJar {
        dependsOn(project.project(":buildsystem-abstraction").subprojects.map {
            it.tasks.named("assemble")
        })

        minimize()
        archiveFileName.set("${rootProject.name}-${project.version}.jar")

        val shadePath = "de.eintosti.buildsystem.util.external"
        relocate("io.papermc.lib", "$shadePath.paperlib")
        relocate("com.cryptomorin.xseries", "$shadePath.xseries")
        relocate("fr.mrmicky.fastboard", "$shadePath.fastboard")
        relocate("dev.dewy.nbt", "$shadePath.nbt")
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