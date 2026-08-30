package caif.convention

import org.danilopianini.gradle.mavencentral.PublishOnCentralExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType


internal fun publicationArtifactId(baseArtifactId: String, publicationName: String): String =
    if (publicationName == "android") "$baseArtifactId-android" else baseArtifactId


fun Project.setupPublishing(
    artifactId: String = this.name,
    description: String = "Multiplatform animation and interaction library",
    block: PublishOnCentralExtension.() -> Unit = { },
) {
    pluginManager.apply("org.danilopianini.publish-on-central")
    pluginManager.apply("org.jetbrains.dokka")
    pluginManager.apply("com.nxoim.gradle.publish")

    extensions.configure<PublishOnCentralExtension> {
        repoOwner.set("nxoim")
        projectLongName.set("caif - animated interactions")
        projectDescription.set(description)
        licenseName.set("The Apache License, Version 2.0")
        licenseUrl.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
        projectUrl.set("https://github.com/nxoim/caif")
        scmConnection.set("scm:git:https://github.com/nxoim/caif.git")
        block()
    }

    pluginManager.withPlugin("org.gradle.maven-publish") {
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                this.artifactId = publicationArtifactId(
                    baseArtifactId = artifactId,
                    publicationName = name
                )
                pom {
                    this.description.set(description)

                    developers {
                        developer {
                            id.set("nxoim")
                            name.set("nxoim")
                            email.set("reach@nxoim.com")
                        }
                    }
                }
            }
        }
    }
}
