plugins {
  id("org.jetbrains.kotlin.jvm")
  id("elmslie.detekt")
  id("elmslie.spotless")
  id("elmslie.tests-convention")
}

dependencies {
  implementation(projects.tussaudCore)
  implementation(libs.kotlinx.coroutinesCore)

  testImplementation(projects.tussaudCore)
  testImplementation(libs.kotlinx.coroutinesTest)
}
