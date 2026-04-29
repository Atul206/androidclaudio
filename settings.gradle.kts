pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "androplaudio"

include(":androplaudio-core")
include(":androplaudio-ksp")
include(":sample-app")
