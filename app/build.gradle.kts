import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Needed by data binding (activity_main_dashboard.xml uses <layout>)
    kotlin("kapt")
}

// Secrets live in local.properties (git-ignored), e.g.
//   SUPABASE_URL=https://xxxx.supabase.co
//   SUPABASE_ANON_KEY=eyJ...
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.skd.wallpaper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.skd.wallpaper"
        minSdk = 24
        targetSdk = 36
        versionCode = 7
        versionName = "1.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
        buildConfigField("String", "PIXABAY_API_KEY", "\"${localProps.getProperty("PIXABAY_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            // R8: removes unused code and resources and shrinks the release app (see proguard-rules.pro)
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")


    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Image loading
    implementation("io.coil-kt:coil:2.7.0")

    // Repeating "new trending wallpapers" reminder
    implementation("androidx.work:work-runtime-ktx:2.10.1")

    // Push notifications sent from the Firebase console to every install
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")

    // "New version available" snackbar and Play Store in-app updates
    implementation("com.google.android.play:app-update-ktx:2.1.0")
}
// Firebase reads app/google-services.json (downloaded from the Firebase console, git-ignored).
// Without it the app still builds and runs; push notifications are simply off.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
