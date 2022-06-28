package mega.privacy.android.app.domain.usecase

/**
 * Set push token use case.
 */
fun interface SetPushToken {

    /**
     * Invoke
     *
     * @param newToken The push token.
     */
    operator fun invoke(newToken: String)
}