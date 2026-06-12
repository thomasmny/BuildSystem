plugins {
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("com.diffplug.spotless") version "7.0.2" apply false
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

subprojects {
    plugins.withId("java") {
        pluginManager.apply("com.diffplug.spotless")
        configure<com.diffplug.gradle.spotless.SpotlessExtension> {
            java {
                palantirJavaFormat("2.92.0").formatJavadoc(true)
                removeUnusedImports()
                formatAnnotations()
            }
        }
    }
}
