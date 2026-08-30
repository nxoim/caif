package convention

import caif.convention.Constants
import caif.convention.autoVersionFromGit
import caif.convention.getAnyCredentialOf
import caif.convention.localProperties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

class PublishPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("signing")

        project.group = Constants.packageRootName
        project.version = resolveVersion(project)

        if (project.version != "undefined") {
            println("> Assembling artifact for publishing ${project.group}:${project.name}:${project.version}")
        }

        val signingWasSetUp = setupSigning(project)
        project.configure<SigningExtension> {
            isRequired = signingWasSetUp
        }
        project.tasks.withType<Sign>().configureEach {
            onlyIf { signingWasSetUp }
        }

        if (signingWasSetUp) {
            loadMavenCredentialsForPublishingOnCentralPlugin(project)
        } else {
            println("Signing was not setup, therefore will not set up maven central credentials for publishing")
        }
    }

    private fun resolveVersion(project: Project): Any {
        val explicit = (project.findProperty("version") as? String)
            ?.trim()
            ?.takeIf { it.isNotEmpty() && it != "unspecified" }
        return explicit ?: project.autoVersionFromGit()
    }

    private fun setupSigning(project: Project): Boolean {
        val signingKey = project.getAnyCredentialOf(
            "SIGNING_KEY",
            "SIGNING_KEY_PATH",
            "SIGNING_KEY_BASE64"
        ) ?: return false

        val signingPassword = project.getAnyCredentialOf(
            "SIGNING_KEY_PASSWORD",
            "SIGNING_KEY_PASSWORD_BASE64"
        ) ?: return false

        if (signingKey.isBlank() || signingPassword.isBlank()) {
            println("Signing credentials incomplete: both signing key and its password are required.")
            return false
        }

        fixSigningTaskExecution(project)

        project.configure<SigningExtension> {
            useInMemoryPgpKeys(
                resolveEnvOrProp("SIGNING_KEY_ID", "signingKeyId", project),
                signingKey,
                signingPassword
            )
        }

        return true
    }

    private fun fixSigningTaskExecution(project: Project) {
        project.tasks.withType<AbstractPublishToMaven>().configureEach {
            val signingTasks = project.tasks.withType<Sign>()
            mustRunAfter(signingTasks)
        }
    }

    private fun loadMavenCredentialsForPublishingOnCentralPlugin(project: Project): Pair<String, String>? {
        listOf("mavenCentralPortalUsername", "mavenCentralPortalPassword").forEach { key ->
            if (!project.hasProperty(key)) {
                project.localProperties.getProperty(key)?.let {
                    project.extensions.extraProperties.set(key, it)
                }
            }
        }

        val username = project.getAnyCredentialOf(
            "MAVEN_CENTRAL_PORTAL_USERNAME",
            "MAVEN_CENTRAL_USERNAME",
            "mavenCentralPortalUsername",
            "sonatypeUsername",
            "ossrhUsername"
        )

        val password = project.getAnyCredentialOf(
            "MAVEN_CENTRAL_PORTAL_PASSWORD",
            "MAVEN_CENTRAL_PASSWORD",
            "mavenCentralPortalPassword",
            "sonatypePassword",
            "ossrhPassword"
        )

        if (username == null || password == null) {
            println(
                buildString {
                    appendLine("Could not resolve Maven Central credentials for project '${project.name}'.")
                    appendLine("Expected variables or properties (any one of the alternatives):")
                    appendLine("  Environment:")
                    appendLine("    MAVEN_CENTRAL_PORTAL_USERNAME / MAVEN_CENTRAL_PORTAL_PASSWORD")
                    appendLine("  Fallback environment:")
                    appendLine("    MAVEN_CENTRAL_USERNAME / MAVEN_CENTRAL_PASSWORD")
                    appendLine("  Gradle properties:")
                    appendLine("    mavenCentralPortalUsername / mavenCentralPortalPassword")
                    appendLine("  Legacy Sonatype properties:")
                    appendLine("    sonatypeUsername / sonatypePassword")
                    appendLine("    ossrhUsername / ossrhPassword")
                }
            )
            return null
        }

        return username to password
    }

    private fun resolveEnvOrProp(envName: String, propName: String, project: Project): String? =
        System.getenv(envName)?.trim()?.takeIf { it.isNotEmpty() }
            ?: (project.findProperty(propName) as? String)?.let { it.trim().takeIf { s -> s.isNotEmpty() } }
}
