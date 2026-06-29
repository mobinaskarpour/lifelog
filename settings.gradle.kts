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

rootProject.name = "LifeLog"

include(
    ":app",
    ":core",
    ":data",
    ":domain",
    ":feature_dashboard",
    ":feature_timeline",
    ":feature_apps",
    ":feature_calls",
    ":feature_notifications",
    ":feature_sms",
    ":feature_location",
    ":feature_settings",
    ":feature_permissions",
    ":feature_export",
    ":service",
    ":database",
    ":ui",
    ":utils",
)
