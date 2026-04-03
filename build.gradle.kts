plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.7.1"
}
//build dev: ./gradlew buildPlugin
//build update plugin: ./gradlew publishPlugin
group = "com.atu.tools"
version = "1.0.4"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure IntelliJ Platform Gradle Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    intellijPlatform {
        create("IC", "2024.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "241"
        }

        changeNotes = """
            <h3>1.0.4</h3>
            <ul>
              <li>Thêm Tool Window với icon nhỏ và giao diện tiếng Việt dễ dùng hơn.</li>
              <li>Bổ sung 4 chế độ sync rõ ràng: Add all, Add missing, Update all, Update changed.</li>
              <li>Thêm nút chọn ngôn ngữ theo dữ liệu sheet (auto map locale như en-US, vi-VN, hi-IN...).</li>
              <li>Sửa luồng Apply: chỉ ghi file sau khi bạn xác nhận ở màn hình preview.</li>
              <li>Sửa logic ghi file để không replace toàn bộ nội dung cũ; chỉ chèn/cập nhật key cần thiết.</li>
              <li>Sửa escape ký tự để tránh lỗi build Android: hỗ trợ dấu nháy đơn và chuyển nháy kép sang ngoặc kép Unicode.</li>
              <li>Cải thiện xử lý fallback bản dịch theo default strings/base locale và validate placeholder.</li>
            </ul>
        """.trimIndent()
    }
    publishing {
        token = providers.gradleProperty("intellijPlatformPublishingToken")
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
