import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

fun Project.applyAdapterConfiguration() {
    applyCommonConfiguration()
    apply(plugin = "java-library")

    dependencies {
        "api"(project(":buildsystem-abstraction:common"))
    }
}