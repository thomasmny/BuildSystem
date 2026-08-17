plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.diffplug.spotless") version "8.10.0" apply false
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
        configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            // Formatting is applied by the pre-commit hook, which every clone gets via the core.hooksPath
            // configuration above. A format violation is machine-fixable and tells a reviewer nothing, so it must not
            // fail `check` and cost a CI round trip. Run `spotlessCheck` directly if you want to verify.
            isEnforceCheck = false

            java {
                palantirJavaFormat("2.92.0")
                removeUnusedImports()
                formatAnnotations()
            }
        }
    }
}
