applyCommonConfiguration()

plugins {
    id("java")
    id("maven-publish")
    id("signing")
}

project.description = "API"

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.annotations)
    compileOnly(libs.xseries)
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Javadoc> {
    title = "BuildSystem API (v" + project.version + ")"
    val opt = options as StandardJavadocDocletOptions
    opt.overview("javadoc/overview.html")
    opt.encoding("UTF-8")
    opt.charSet("UTF-8")
    opt.links("https://docs.oracle.com/javase/21/docs/api/")
    opt.links("https://hub.spigotmc.org/javadocs/spigot/")
    opt.links("https://javadoc.io/static/org.jetbrains/annotations/")
    opt.isLinkSource = true
    opt.isUse = true
    opt.keyWords()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("BuildSystem API")
                description.set("API for the BuildSystem Minecraft plugin.")
                url.set("https://github.com/thomasmny/BuildSystem")

                licenses {
                    license {
                        name.set("GNU General Public License, Version 3")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("thomasmny")
                        name.set("Thomas Meaney")
                        email.set("thomas.meaney@icloud.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/thomasmny/BuildSystem.git")
                    developerConnection.set("scm:git:ssh://github.com:thomasmny/BuildSystem.git")
                    url.set("https://github.com/thomasmny/BuildSystem")
                    tag.set(project.version.toString())
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/thomasmny/BuildSystem/issues")
                }
            }
        }
    }
}

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project

    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    isRequired = true
    sign(publishing.publications["mavenJava"])
}