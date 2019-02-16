object CredentialsRepository {
  private val usersCredentialsEncrypted = Map(
    "nimda" -> "$2a$10$jUWLpy4Uv2bdFDpUe7WdqeKPQg6Qa9zRFv95rLjvG0KYHa3MliU.6"
  )

  def getCredentialsMap: Map[String, String] = usersCredentialsEncrypted
}
