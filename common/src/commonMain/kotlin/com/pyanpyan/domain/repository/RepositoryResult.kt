package com.pyanpyan.domain.repository

/**
 * A functional Result type for repository operations.
 * Similar to Kotlin's Result but specialized for repository errors.
 */
sealed class RepositoryResult<out T> {
    data class Success<out T>(val value: T) : RepositoryResult<T>()
    data class Failure(val error: RepositoryError) : RepositoryResult<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun errorOrNull(): RepositoryError? = when (this) {
        is Success -> null
        is Failure -> error
    }

    inline fun <R> map(transform: (T) -> R): RepositoryResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    inline fun <R> flatMap(transform: (T) -> RepositoryResult<R>): RepositoryResult<R> = when (this) {
        is Success -> transform(value)
        is Failure -> this
    }

    inline fun onSuccess(action: (T) -> Unit): RepositoryResult<T> {
        if (this is Success) action(value)
        return this
    }

    inline fun onFailure(action: (RepositoryError) -> Unit): RepositoryResult<T> {
        if (this is Failure) action(error)
        return this
    }

    companion object {
        fun <T> success(value: T): RepositoryResult<T> = Success(value)
        fun <T> failure(error: RepositoryError): RepositoryResult<T> = Failure(error)
    }
}
