import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

applyCoreConfiguration()

plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.2.2"
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
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
    api(project(":buildsystem-api"))

    compileOnlyApi(libs.annotations)
    compileOnlyApi(libs.jspecify)

    compileOnly(libs.spigot)
    compileOnly(libs.authlib)
    compileOnly(libs.luckperms)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.worldedit)
    compileOnly(libs.axiompaper)

    implementation(libs.paperlib)
    implementation(libs.xseries)
    implementation(libs.fastboard)
    implementation(libs.nbt) { isTransitive = false }
    implementation(libs.bstats)
    implementation(libs.aws.core) // Unable to find the dependency at runtime, so we add it here

    library(libs.bundles.aws)
    library(libs.bouncycastle)
    library(libs.sftp)
    library(libs.zip4j)
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<ShadowJar>("shadowJar") {
    minimize()
    archiveFileName.set("${rootProject.name}-${project.version}.jar")

    val shadePath = "de.eintosti.buildsystem.util.external"
    relocate("io.papermc.lib", "$shadePath.paperlib")
    relocate("com.cryptomorin.xseries", "$shadePath.xseries")
    relocate("fr.mrmicky.fastboard", "$shadePath.fastboard")
    relocate("dev.dewy.nbt", "$shadePath.nbt")
    relocate("org.bstats", "$shadePath.bstats")
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

bukkit {
    name = "BuildSystem"
    version = "${project.version}"
    description = "Powerful, easy to use system for builders"
    author = "einTosti"
    website = "https://buildsystem.eintosti.de"

    main = "de.eintosti.buildsystem.BuildSystemPlugin"
    apiVersion = "1.13"
    softDepend = listOf("LuckPerms", "PlaceholderAPI", "WorldEdit", "AxiomPaper")

    commands {
        register("back") {
            description = "Teleports you to your previous location."
            usage = "/<command>"
        }
        register("blocks") {
            description = "Opens a menu with secret blocks."
            usage = "/<command>"
        }
        register("build") {
            description = "Bypass build restrictions."
            usage = "/<command> [player]"
        }
        register("buildsystem") {
            description = "Overview of all plugin commands."
            usage = "/<command>"
        }
        register("config") {
            description = "Reload the config."
            usage = "/<command> reload"
        }
        register("day") {
            description = "Set a world's time to daytime."
            usage = "/<command> [world]"
        }
        register("explosions") {
            description = "Toggle explosions within a world."
            usage = "/<command> [world]"
        }
        register("gamemode") {
            description = "Change your gamemode."
            aliases = listOf("gm")
            usage = "/<command> <mode> [player]"
        }
        register("night") {
            description = "Set a world's time to nighttime."
            usage = "/<command> [world]"
        }
        register("noai") {
            description = "Disable all the entity AIs in a world."
            usage = "/<command> [world]"
        }
        register("physics") {
            description = "Toggle block physics."
            usage = "/<command> [world]"
        }
        register("settings") {
            description = "Manage user settings."
            usage = "/<command>"
        }
        register("setup") {
            description = "Change the default items in used in the navigator."
            usage = "/<command>"
        }
        register("skull") {
            description = "Receive a player's skull."
            usage = "/<command> [name]"
        }
        register("spawn") {
            description = "Teleport to the spawn."
            usage = "/<command>"
        }
        register("speed") {
            description = "Change your flying/walking speed."
            aliases = listOf("s")
            usage = "/<command> [1-5]"
        }
        register("top") {
            description = "Teleports you to the highest location."
            usage = "/<command>"
        }
        register("worlds") {
            description = "Open the world menu."
            aliases = listOf("w")
            usage = "/<command>"
        }
    }
    permissions {
        register("buildsystem.help") {
            children = listOf("buildsystem.help.buildsystem", "buildsystem.help.worlds")
            description = "Permission for help commands."
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("buildsystem.navigator") {
            description = "Open the worlds navigator."
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("buildsystem.navigator.item") {
            description = "Receive and use the navigator."
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("buildsystem.create") {
            children = listOf(
                "buildsystem.create.private",
                "buildsystem.create.type.normal",
                "buildsystem.create.type.flat",
                "buildsystem.create.type.nether",
                "buildsystem.create.type.end",
                "buildsystem.create.type.void"
            )
            description = "Permission for creating world types."
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("buildsystem.setstatus") {
            children = listOf(
                "buildsystem.setstatus.hidden",
                "buildsystem.setstatus.archive",
                "buildsystem.setstatus.finished",
                "buildsystem.setstatus.almostfinished",
                "buildsystem.setstatus.inprogress",
                "buildsystem.setstatus.notstarted"
            )
            description = "Permission for setting world status."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("buildsystem.gamemode") {
            children = listOf("buildsystem.gamemode.survival", "buildsystem.gamemode.creative", "buildsystem.gamemode.adventure", "buildsystem.gamemode.spectator")
            description = "Permission for changing own gamemode."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("buildsystem.gamemode.other") {
            children =
                listOf("buildsystem.gamemode.survival.other", "buildsystem.gamemode.creative.other", "buildsystem.gamemode.adventure.other", "buildsystem.gamemode.spectator.other")
            description = "Permission for changing other player's gamemode."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("buildsystem.physics.message") {
            description = "Receive the message that physics are disabled in a world."
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("buildsystem.updates") {
            description = "Receive update messages."
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}