pluginManagement {
    repositories {
        maven { 
            url = uri("https://maven.google.com")
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven { 
            url = uri("https://maven.google.com")
        }
        mavenCentral()
    }
}

rootProject.name = "sdk4" // ✅ Use your actual project folder name
include(":app")
