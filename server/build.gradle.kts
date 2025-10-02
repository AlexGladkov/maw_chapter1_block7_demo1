plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.framework)
    alias(libs.plugins.spring.dependencies)
    alias(libs.plugins.spring.plugin)
}

java.sourceCompatibility = JavaVersion.VERSION_21

dependencies {
    implementation(libs.spring.framework)
    implementation(libs.spring.web)
    implementation(libs.spring.validation)
    implementation(libs.kotlinx.serialization.json)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}