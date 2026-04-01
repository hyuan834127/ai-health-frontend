plugins {
    alias(libs.plugins.android.application)

}

android {
    // 这里的 namespace 对应你的包名
    namespace = "com.example.zhimai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.zhimai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }



    // 开启 ViewBinding (咱们页面必须要用的)
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // 1. Android Studio 默认自带的基础核心库 (帮你补回来了)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // 2. 咱们自己加的“三大金刚”核心库
    // 网络请求 (对接队长后端)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // 高级图表 (画心电图)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // OPPO 官方健康 SDK
    implementation("com.heytap.health:sdk:2.1.7")

    // 3. 默认的测试库 (帮你补回来了)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}