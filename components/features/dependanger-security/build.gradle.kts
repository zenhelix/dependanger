plugins {
    id("dependanger.feature")
}

description = "Dependanger Security - CVE vulnerability scanning via OSV API"

dependencies {
    implementation(projects.components.shared.dependangerFeatureModel)
    implementation(projects.components.shared.dependangerFeatureSupport)
    implementation(projects.components.shared.integrations.dependangerOsvHttpClient)
    implementation(projects.components.shared.dependangerCache)
    implementation(libs.kotlinx.datetime)
}
