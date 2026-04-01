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

// ✅ 保留了你原有的 Java 环境管理插件，防止报错
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // 1. 图表库的仓库
        maven { url = java.net.URI("https://jitpack.io") }

        // 2. OPPO 健康 SDK 的 Releases 仓库
        maven {
            url = java.net.URI("https://maven.columbus.heytapmobi.com/repository/heytap-health-releases/")
            isAllowInsecureProtocol = true
            credentials {
                username = "healthUser"
                password = "8174a9eac1264495b593a9d5ab221491"
            }
        }

        // 3. OPPO 健康 SDK 的 Snapshots 仓库
        maven {
            url = java.net.URI("https://maven.columbus.heytapmobi.com/repository/heytap-health-snapshots/")
            isAllowInsecureProtocol = true
            credentials {
                username = "healthUser"
                password = "8174a9eac1264495b593a9d5ab221491"
            }
        }
    }
}

// 注意：这里的名字保持你目前的项目名即可
rootProject.name = "智脉"
include(":app")