plugins {
  id("elmslie.android-lib")
  id("elmslie.publishing")
  alias(libs.plugins.binaryCompatibilityValidator)
//  kotlin("plugin.serialization") version "2.2.0"
}

android { namespace = "money.vivid.elmslie.android" }

elmsliePublishing {
  pom {
    name = "Elmslie Android"
    description =
      "Elmslie is a minimalistic reactive implementation of TEA/ELM. Android specific. https://github.com/vivid-money/elmslie/"
  }
}

dependencies {
  api(projects.tussaudCore)

  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.lifecycle.runtime)
  implementation(libs.androidx.lifecycle.viewmodel)
  implementation(libs.androidx.startup.runtime)
  implementation(libs.gson)
}
