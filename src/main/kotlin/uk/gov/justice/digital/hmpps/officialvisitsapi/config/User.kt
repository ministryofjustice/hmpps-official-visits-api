package uk.gov.justice.digital.hmpps.officialvisitsapi.config

data class User(val username: String, val activeCaseLoadId: String? = null) {
  companion object {
    val SYS_USER = User("SYS")
    const val REQUEST_ATTRIBUTE = "user"
  }
}
