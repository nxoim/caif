package caif.convention

import org.gradle.api.Project
import java.util.Base64
import java.util.Properties

private var _localProps: Properties? = null

val Project.localProperties: Properties
    get() {
        if (_localProps != null) return _localProps!!
        val props = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }
        _localProps = props
        return props
    }

fun Project.autoVersionFromGit(): String = runCatching {
    ProcessBuilder("git", "describe", "--tags", "--always")
        .redirectErrorStream(true)
        .start()
        .run { inputStream.bufferedReader().readText().trim() }
}
    .getOrDefault("undefined")

/** environment → local.properties → project property */
@Suppress("NewApi")
fun Project.getAnyCredentialOf(vararg names: String): String? {
    for (name in names) {
        val raw = getenv(name)
            ?: localProperties.getProperty(name)?.trimAndNullIfEmpty()
            ?: projectProp(name)

        if (raw == null) continue

        val resolved = runCatching {
            when {
                name.contains("_PATH") -> rootProject
                    .file(raw)
                    .takeIf { it.exists() }
                    ?.readText()

                name.contains("_BASE64") -> Base64
                    .getDecoder()
                    .decode(raw)
                    .decodeToString()

                else -> raw
            }
        }.getOrNull()?.trimAndNullIfEmpty()

        if (!resolved.isNullOrEmpty()) return resolved
    }

    println(
        buildString {
            appendLine("Could not resolve any of the credentials: ${names.joinToString(", ")} for project '${project.name}'.")
            appendLine("Searched sources in order for each name:")
            appendLine("  1. Environment variable <NAME>")
            appendLine("  2. local.properties entry <NAME>")
            appendLine("  3. Gradle property -P<NAME>")
        }
    )

    return null
}

fun Project.projectProp(name: String): String? = (findProperty(name) as? String).trimAndNullIfEmpty()

private fun getenv(name: String): String? = System.getenv(name)?.trimAndNullIfEmpty()

private fun String?.trimAndNullIfEmpty(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
