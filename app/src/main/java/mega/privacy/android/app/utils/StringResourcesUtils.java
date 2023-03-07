package mega.privacy.android.app.utils;

import static nz.mega.sdk.MegaChatError.ERROR_ACCESS;
import static nz.mega.sdk.MegaChatError.ERROR_ARGS;
import static nz.mega.sdk.MegaChatError.ERROR_EXIST;
import static nz.mega.sdk.MegaChatError.ERROR_NOENT;
import static nz.mega.sdk.MegaChatError.ERROR_OK;
import static nz.mega.sdk.MegaChatError.ERROR_TOOMANY;
import static nz.mega.sdk.MegaChatError.ERROR_UNKNOWN;
import static nz.mega.sdk.MegaError.API_EACCESS;
import static nz.mega.sdk.MegaError.API_EAGAIN;
import static nz.mega.sdk.MegaError.API_EAPPKEY;
import static nz.mega.sdk.MegaError.API_EARGS;
import static nz.mega.sdk.MegaError.API_EBLOCKED;
import static nz.mega.sdk.MegaError.API_EBUSINESSPASTDUE;
import static nz.mega.sdk.MegaError.API_ECIRCULAR;
import static nz.mega.sdk.MegaError.API_EEXIST;
import static nz.mega.sdk.MegaError.API_EEXPIRED;
import static nz.mega.sdk.MegaError.API_EFAILED;
import static nz.mega.sdk.MegaError.API_EGOINGOVERQUOTA;
import static nz.mega.sdk.MegaError.API_EINCOMPLETE;
import static nz.mega.sdk.MegaError.API_EINTERNAL;
import static nz.mega.sdk.MegaError.API_EKEY;
import static nz.mega.sdk.MegaError.API_EMASTERONLY;
import static nz.mega.sdk.MegaError.API_EMFAREQUIRED;
import static nz.mega.sdk.MegaError.API_ENOENT;
import static nz.mega.sdk.MegaError.API_EOVERQUOTA;
import static nz.mega.sdk.MegaError.API_ERANGE;
import static nz.mega.sdk.MegaError.API_ERATELIMIT;
import static nz.mega.sdk.MegaError.API_EREAD;
import static nz.mega.sdk.MegaError.API_ESID;
import static nz.mega.sdk.MegaError.API_ESSL;
import static nz.mega.sdk.MegaError.API_ETEMPUNAVAIL;
import static nz.mega.sdk.MegaError.API_ETOOMANY;
import static nz.mega.sdk.MegaError.API_ETOOMANYCONNECTIONS;
import static nz.mega.sdk.MegaError.API_EWRITE;
import static nz.mega.sdk.MegaError.API_OK;
import static nz.mega.sdk.MegaError.PAYMENT_EBALANCE;
import static nz.mega.sdk.MegaError.PAYMENT_EBILLING;
import static nz.mega.sdk.MegaError.PAYMENT_ECARD;
import static nz.mega.sdk.MegaError.PAYMENT_EFRAUD;
import static nz.mega.sdk.MegaError.PAYMENT_EGENERIC;
import static nz.mega.sdk.MegaError.PAYMENT_ETOOMANY;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.domain.exception.MegaException;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaError;
import timber.log.Timber;

public class StringResourcesUtils {

    // English locale resources
    private static Resources enRes;

    /**
     * Get the English locale resources
     *
     * @param context The current context.
     * @return The English locale resources.
     */
    private static Resources getENResource(Context context) {
        if (enRes == null) {
            Configuration configuration = new Configuration(context.getResources().getConfiguration());
            configuration.setLocale(new Locale(Locale.ENGLISH.getLanguage()));
            enRes = context.createConfigurationContext(configuration).getResources();
        }
        return enRes;
    }

    /**
     * Return the string value associated with a particular resource ID in the current app language.
     * If something fails return the original string in English.
     *
     * @param resId The resource identifier of the desired string.
     * @return The desired string.
     * @deprecated Use {@link android.content.Context#getString(int)} instead.
     */
    @Deprecated
    public static String getString(int resId) {

        MegaApplication app = MegaApplication.getInstance();

        try {
            return app.getString(resId);
        } catch (Exception e) {
            Timber.w(e, "Error getting a translated string.");
            String originalString = getENResource(app).getString(resId);
            Timber.i("Using the original English string: %s", originalString);
            return originalString;
        }
    }

    /**
     * Return the string value associated with a particular resource ID in the current app language,
     * substituting the format arguments as defined in Formatter and String.format(String, Object...).
     * If something fails return the original string in English.
     *
     * @param resId      The resource identifier of the desired string.
     * @param formatArgs The format arguments that will be used for substitution.
     * @return The desired string.
     * @deprecated Use {@link android.content.Context#getString(int, Object...)} instead.
     */
    @Deprecated
    public static String getString(int resId, Object... formatArgs) {

        MegaApplication app = MegaApplication.getInstance();

        try {
            return app.getString(resId, formatArgs);
        } catch (Exception e) {
            Timber.w(e, "Error getting a translated and formatted string.");
            String originalString = getENResource(app).getString(resId, formatArgs);
            Timber.i("Using the original English string: %s", originalString);
            return originalString;
        }
    }

    /**
     * Returns the string necessary for grammatically correct pluralization of the given
     * resource ID for the given quantity in the current app language.
     * If something fails return the original string in English.
     *
     * @param resId    The resource identifier of the desired string
     * @param quantity The number used to get the correct string for the current language's plural rules.
     * @return The desired string.
     * @deprecated Use {@link android.content.res.Resources#getQuantityString(int, int)} instead.
     */
    @Deprecated
    public static String getQuantityString(int resId, int quantity) {

        MegaApplication app = MegaApplication.getInstance();

        try {
            return app.getResources().getQuantityString(resId, quantity);
        } catch (Exception e) {
            Timber.w(e, "Error getting a translated string with quantity modifier.");
            String originalString = getENResource(app).getQuantityString(resId, quantity);
            Timber.i("Using the original English string: %s", originalString);
            return originalString;
        }
    }

    /**
     * Returns the string necessary for grammatically correct pluralization of the given resource ID
     * for the given quantity in the current app language and substituting the format arguments as
     * defined in Formatter and String.format(String, Object...).
     * If something fails return the original string in English.
     *
     * @param resId      The resource identifier of the desired string.
     * @param quantity   The number used to get the correct string for the current language's plural rules.
     * @param formatArgs The format arguments that will be used for substitution.
     * @return The desired string.
     * @deprecated Use {@link android.content.res.Resources#getQuantityString(int, int, Object...)} instead.
     */
    @Deprecated
    public static String getQuantityString(int resId, int quantity, Object... formatArgs) {

        MegaApplication app = MegaApplication.getInstance();

        try {
            return app.getResources().getQuantityString(resId, quantity, formatArgs);
        } catch (Exception e) {
            Timber.w(e, "Error getting a translated and formatted string with quantity modifier.");
            String originalString = getENResource(app).getQuantityString(resId, quantity, formatArgs);
            Timber.i("Using the original English string: %s", originalString);
            return originalString;
        }
    }

    /**
     * Gets the translated string of an error received in a request.
     *
     * @param error MegaError received in the request
     * @return The translated string
     * @deprecated Use {@link  mega.privacy.android.app.presentation.extensions.MegaExceptionKt#getErrorStringId(MegaException)}
     * and {@link android.content.Context#getString(int, Object...)} instead.
     */
    @Deprecated
    public static String getTranslatedErrorString(MegaError error) {
        MegaApplication app = MegaApplication.getInstance();
        if (app == null) {
            return error.getErrorString();
        }

        return getTranslatedErrorString(error.getErrorCode(), error.getErrorString());
    }

    /**
     * Gets the translated string of a Mega error.
     *
     * @param errorCode MegaChat error code
     * @return The translated string
     * @deprecated Use {@link  mega.privacy.android.app.presentation.extensions.MegaExceptionKt#getErrorStringId(MegaException)}
     * and {@link android.content.Context#getString(int, Object...)} instead.
     */
    @Deprecated
    public static String getTranslatedErrorString(int errorCode, String errorString) {
        switch (errorCode) {
            case API_OK:
                return getString(R.string.api_ok);
            case API_EINTERNAL:
                return getString(R.string.api_einternal);
            case API_EARGS:
                return getString(R.string.api_eargs);
            case API_EAGAIN:
                return getString(R.string.api_eagain);
            case API_ERATELIMIT:
                return getString(R.string.api_eratelimit);
            case API_EFAILED:
                return getString(R.string.api_efailed);
            case API_ETOOMANY:
                if (errorString.equals("Terms of Service breached")) {
                    return getString(R.string.api_etoomany_ec_download);
                } else if (errorString.equals("Too many concurrent connections or transfers")) {
                    return getString(R.string.api_etoomay);
                } else {
                    return errorString;
                }
            case API_ERANGE:
                return getString(R.string.api_erange);
            case API_EEXPIRED:
                return getString(R.string.api_eexpired);
            case API_ENOENT:
                return getString(R.string.api_enoent);
            case API_ECIRCULAR:
                if (errorString.equals("Upload produces recursivity")) {
                    return getString(R.string.api_ecircular_ec_upload);
                } else if (errorString.equals("Circular linkage detected")) {
                    return getString(R.string.api_ecircular);
                } else {
                    return errorString;
                }
            case API_EACCESS:
                return getString(R.string.api_eaccess);
            case API_EEXIST:
                return getString(R.string.api_eexist);
            case API_EINCOMPLETE:
                return getString(R.string.api_eincomplete);
            case API_EKEY:
                return getString(R.string.api_ekey);
            case API_ESID:
                return getString(R.string.api_esid);
            case API_EBLOCKED:
                if (errorString.equals("Not accessible due to ToS/AUP violation") || errorString.equals("Blocked")) {
                    return getString(R.string.error_download_takendown_file);
                } else {
                    return errorString;
                }
            case API_EOVERQUOTA:
                return getString(R.string.api_eoverquota);
            case API_ETEMPUNAVAIL:
                return getString(R.string.api_etempunavail);
            case API_ETOOMANYCONNECTIONS:
                return getString(R.string.api_etoomanyconnections);
            case API_EWRITE:
                return getString(R.string.api_ewrite);
            case API_EREAD:
                return getString(R.string.api_eread);
            case API_EAPPKEY:
                return getString(R.string.api_eappkey);
            case API_ESSL:
                return getString(R.string.api_essl);
            case API_EGOINGOVERQUOTA:
                return getString(R.string.api_egoingoverquota);
            case API_EMFAREQUIRED:
                return getString(R.string.api_emfarequired);
            case API_EMASTERONLY:
                return getString(R.string.api_emasteronly);
            case API_EBUSINESSPASTDUE:
                return getString(R.string.api_ebusinesspastdue);
            case PAYMENT_ECARD:
                return getString(R.string.payment_ecard);
            case PAYMENT_EBILLING:
                return getString(R.string.payment_ebilling);
            case PAYMENT_EFRAUD:
                return getString(R.string.payment_efraud);
            case PAYMENT_ETOOMANY:
                return getString(R.string.payment_etoomay);
            case PAYMENT_EBALANCE:
                return getString(R.string.payment_ebalance);
            case PAYMENT_EGENERIC:
            default:
                if (errorCode > 0) {
                    return getString(R.string.api_error_http);
                } else {
                    return getString(R.string.payment_egeneric_api_error_unknown);
                }
        }
    }

    /**
     * Gets the translated string of an error received in a request.
     *
     * @param error MegaChatError received in the request
     * @return The translated string
     * @deprecated Use {@link  mega.privacy.android.app.presentation.extensions.MegaExceptionKt#getChatErrorStringId(MegaException)} (MegaException)}
     * and {@link android.content.Context#getString(int, Object...)} instead.
     */
    @Deprecated
    public static String getTranslatedErrorString(MegaChatError error) {
        MegaApplication app = MegaApplication.getInstance();
        if (app == null) {
            return error.getErrorString();
        }

        return getTranslatedChatErrorString(error.getErrorCode());
    }

    /**
     * Gets the translated string of a MegaChat error code.
     *
     * @param errorCode MegaChat error code
     * @return The translated string
     * @deprecated Use {@link  mega.privacy.android.app.presentation.extensions.MegaExceptionKt#getChatErrorStringId(MegaException)} (MegaException)}
     * and {@link android.content.Context#getString(int, Object...)} instead.
     */
    @Deprecated
    public static String getTranslatedChatErrorString(int errorCode) {
        switch (errorCode) {
            case ERROR_OK:
                return getString(R.string.error_ok);
            case ERROR_ARGS:
                return getString(R.string.error_args);
            case ERROR_ACCESS:
                return getString(R.string.error_access);
            case ERROR_NOENT:
                return getString(R.string.error_noent);
            case ERROR_EXIST:
                return getString(R.string.error_exist);
            case ERROR_TOOMANY:
                return getString(R.string.error_toomany);
            case ERROR_UNKNOWN:
            default:
                return getString(R.string.error_unknown);
        }
    }
}
