import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

val keystorePropsFile = rootProject.file("release.properties")
val keystoreProps = Properties()

if (keystorePropsFile.exists()) {
    FileInputStream(keystorePropsFile).use {
        keystoreProps.load(it)
    }
}

val hasValidSigningProps = listOf(
    "storeFile",
    "storePassword",
    "keyAlias",
    "keyPassword"
).all { keystoreProps[it] != null }

android {
    namespace = "com.example.cineworld"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.cineworld"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (hasValidSigningProps) {
            create("release") {
                storeFile = rootProject.file(
                    keystoreProps["storeFile"] as String
                )
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            if (hasValidSigningProps) {
                signingConfig = signingConfigs.getByName("release")
            }

            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        checkReleaseBuilds = false
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
            excludes.add("META-INF/kotlinx_coroutines_core.version")

            pickFirsts.add("nonJvmMain/default/linkdata/package_androidx/0_androidx.knm")
            pickFirsts.add("nonJvmMain/default/linkdata/root_package/0_.knm")
            pickFirsts.add("nonJvmMain/default/linkdata/module")
            pickFirsts.add("nativeMain/default/linkdata/root_package/0_.knm")
            pickFirsts.add("nativeMain/default/linkdata/module")
            pickFirsts.add("commonMain/default/linkdata/root_package/0_.knm")
            pickFirsts.add("commonMain/default/linkdata/module")
            pickFirsts.add("commonMain/default/linkdata/package_androidx/0_androidx.knm")
            pickFirsts.add("META-INF/kotlin-project-structure-metadata.json")

            merges.add("commonMain/default/manifest")
            merges.add("nonJvmMain/default/manifest")
            merges.add("nativeMain/default/manifest")
        }
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:deprecation")
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.android.material)
    implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.interpolator)

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.hbb20:ccp:2.7.3")

    // CineWorld Media3 Video Player
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
    implementation("androidx.media3:media3-common:1.6.1")

    // 4K DASH Video Support
    implementation("androidx.media3:media3-exoplayer-dash:1.6.1")

    // Firebase CineWorld
    implementation(platform("com.google.firebase:firebase-bom:35.2.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    testImplementation(libs.junit)
}
