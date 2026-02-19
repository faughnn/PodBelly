pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Podbelly"

include(":app")
include(":core:database")
include(":core:network")
include(":core:playback")
include(":core:common")
include(":feature:home")
include(":feature:discover")
include(":feature:podcast")
include(":feature:player")
include(":feature:queue")
include(":feature:settings")
