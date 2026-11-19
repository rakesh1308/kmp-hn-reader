package com.rakesh1308.hnreader.domain.model

/**
 * Expected failures are values, not exceptions.
 *
 * Why: an uncaught Kotlin exception that reaches Swift crashes the app unless the function is
 * annotated with @Throws. Returning [Outcome] keeps error handling identical on both platforms.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

sealed interface AppError {
    /** No connection, timeout, DNS failure... */
    data object Network : AppError

    /** The server answered with a non-2xx status. */
    data class Server(val statusCode: Int) : AppError

    /** The server answered, but the body was not what we expected. */
    data object InvalidResponse : AppError

    data class Unknown(val message: String?) : AppError
}
