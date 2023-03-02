package mega.privacy.android.app.presentation.twofactorauthentication.model

/**
 * UI State for enabling/disabling the two factor authentication
 * @property AuthenticationPassed UI state for successful authentication via valid pin codes
 * @property AuthenticationFailed UI state for failed authentication via invalid pin codes
 * @property AuthenticationError UI state for failed authentication for unknown reason
 */
enum class AuthenticationState {
    AuthenticationPassed,
    AuthenticationFailed,
    AuthenticationError
}