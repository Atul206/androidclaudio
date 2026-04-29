import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
    signing
}

group = "com.androplaudio"
version = libs.versions.androplaudio.get()

kotlin {
    explicitApi()

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs += "-Xexpect-actual-classes"
            }
        }
    }

    val xcf = XCFramework("AndroClaudio")
    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach {
        it.binaries.framework {
            baseName = "AndroClaudio"
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.ktor.server.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.coroutines.android)
            compileOnly(libs.koin.android)
        }

        iosMain.dependencies {
            implementation(libs.ktor.server.cio)
            implementation(libs.ktor.server.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
    }
}

android {
    namespace = "com.androplaudio.core"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    publishing {
        singleVariant("debug") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("core") {
                artifactId = "androplaudio-core"
                from(components["debug"])

                pom {
                    name = "AndroClaudio Core"
                    description = "Embed an MCP server in your Android debug build so Claude Code can discover and call your app's live functions."
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
}

apply(from = rootProject.file("gradle/publish.gradle.kts"))
