package com.pyanpyan.domain.repository

sealed class RepositoryError {
    data class FileReadError(
        val message: String,
        val cause: Throwable? = null
    ) : RepositoryError()

    data class FileWriteError(
        val message: String,
        val cause: Throwable? = null
    ) : RepositoryError()

    data class JsonParseError(
        val message: String,
        val cause: Throwable? = null
    ) : RepositoryError()

    data class InvalidDataError(
        val message: String
    ) : RepositoryError()
}
