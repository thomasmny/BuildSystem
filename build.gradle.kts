import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessTask

plugins {
    // The core module writes the plugin jar to the root build/libs, so the root needs a clean task too.
    base
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.diffplug.spotless") version "8.10.4" apply false
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

// Point Git at the versioned hooks directory so the Spotless pre-commit hook
// is active in every clone. Runs once per configuration if not already set.
if (file(".git").exists()) {
    val hooksPath = providers.exec {
        commandLine("git", "config", "--get", "core.hooksPath")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
    if (hooksPath != ".githooks") {
        providers.exec {
            commandLine("git", "config", "core.hooksPath", ".githooks")
        }.result.get()
        logger.lifecycle("Configured git core.hooksPath = .githooks")
    }
}

subprojects {
    plugins.withId("java") {
        pluginManager.apply("com.diffplug.spotless")
        configure<SpotlessExtension> {
            java {
                palantirJavaFormat("2.92.0")
                removeUnusedImports()
                formatAnnotations()
            }
        }
        // Spotless hooks every clean task to close its cached formatter classloaders. With the configuration cache,
        // tasks run in parallel, so a clean in one module could close the classloader another module's spotless task
        // is still using (NoClassDefFoundError for Guava classes). The broken result then got stored in the build
        // cache and replayed on every later build.
        tasks.withType<SpotlessTask>().configureEach {
            mustRunAfter(rootProject.subprojects.map { "${it.path}:clean" })
            outputs.cacheIf { false }
        }
    }
}
