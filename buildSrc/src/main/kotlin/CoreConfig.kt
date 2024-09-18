import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.the

fun Project.applyCoreConfiguration() {
    applyCommonConfiguration()

    apply(plugin = "java")
    apply(plugin = "eclipse")
    apply(plugin = "idea")

    if (name in setOf("buildsystem-core")) {
        the<JavaPluginExtension>().withSourcesJar()
    }
}