import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.ivy
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

fun RepositoryHandler.modrinthMavenWorkaround(nameOrId: String, version: String, fileName: String) {
    val url = "https://api.modrinth.com/maven/maven/modrinth/$nameOrId/$version/$fileName"
    val group = "maven.modrinth.workaround"
    ivy(url.substringBeforeLast('/')) {
        name = "Modrinth Maven Workaround for $nameOrId"
        patternLayout { artifact(url.substringAfterLast('/')) }
        metadataSources { artifact() }
        content { includeModule(group, nameOrId) }
    }
}