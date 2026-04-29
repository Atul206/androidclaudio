// Shared signing + repository configuration applied in each module.
// Plugins (maven-publish, signing) are declared in the consuming module's build.gradle.kts.
// This script only wires credentials and repository targets.

val signingKeyId: String? =
    findProperty("signing.keyId") as String? ?: System.getenv("SIGNING_KEY_ID")
val signingPassword: String? =
    findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")
val signingSecretKey: String? =
    findProperty("signing.secretKey") as String? ?: System.getenv("SIGNING_SECRET_KEY")
val mavenCentralUsername: String? =
    findProperty("mavenCentral.username") as String? ?: System.getenv("MAVEN_CENTRAL_USERNAME")
val mavenCentralPassword: String? =
    findProperty("mavenCentral.password") as String? ?: System.getenv("MAVEN_CENTRAL_PASSWORD")

val isReleaseBuild = !version.toString().endsWith("-SNAPSHOT")
val isSigningConfigured = signingKeyId != null && signingPassword != null && signingSecretKey != null

configure<org.gradle.plugins.signing.SigningExtension> {
    // Signing only required for Maven Central releases, not local AAR distribution
    isRequired = false
    if (isSigningConfigured) {
        useInMemoryPgpKeys(signingKeyId, signingSecretKey, signingPassword)
        val publishing = the<PublishingExtension>()
        sign(publishing.publications)
    }
}

configure<PublishingExtension> {
    repositories {
        // Maven Central via Sonatype OSSRH
        maven {
            name = "MavenCentral"
            url = if (isReleaseBuild) {
                uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            } else {
                uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            }
            credentials {
                username = mavenCentralUsername
                password = mavenCentralPassword
            }
        }

        // Local Maven — publish here for development/testing without Sonatype
        maven {
            name = "LocalMaven"
            url = uri(rootProject.layout.buildDirectory.dir("localMaven"))
        }
    }
}
