plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
    signing
}

group = "com.androplaudio"
version = libs.versions.androplaudio.get()

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinx.serialization.json)
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("ksp") {
            artifactId = "androplaudio-ksp"
            from(components["java"])

            pom {
                name = "AndroClaudio KSP"
                description = "KSP processor for AndroClaudio — generates group registries at build time."
                url = "https://github.com/androplaudio/androplaudio"
                licenses {
                    license {
                        name = "Apache License 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "androplaudio"
                        name = "AndroClaudio"
                        email = "dev@androplaudio.com"
                    }
                }
                scm {
                    connection = "scm:git:github.com/androplaudio/androplaudio.git"
                    developerConnection = "scm:git:ssh://github.com/androplaudio/androplaudio.git"
                    url = "https://github.com/androplaudio/androplaudio"
                }
            }
        }
    }
}

apply(from = rootProject.file("gradle/publish.gradle.kts"))
