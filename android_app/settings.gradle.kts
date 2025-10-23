pluginManagement {
    repositories {
        google {
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
// In your settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }

        // PayPal CardinalCommerce repository with more specific configuration
        maven {
            url = uri("https://cardinalcommerceprod.jfrog.io/artifactory/android")
            credentials {
                username = "paypal_sgerritz"
                password = "AKCp8jQ8tAahqpaCxWCX3RjDJADi2583bKHpHbLkFtbwTWxL5PudXLwuVnNCY7M59scLKzQf3"
            }
            content {
                includeGroup("org.jfrog.cardinalcommerce.gradle")
            }
        }

        // Also add the PayPal repository
        maven {
            url = uri("https://github.com/paypal/paypal-checkout-android/raw/maven")
        }
    }
}

rootProject.name = "MultiNav"
include(":app")
