package ru.larpinovplay.finniapp.domain.util.result

sealed interface Result<out D, out E : DomainError> {

    data class Success<out D>(
        val data: D
    ) : Result<D, Nothing>

    data class Error<out E : DomainError>(
        val error: E
    ) : Result<Nothing, E>
}

typealias EmptyResult<E> = Result<Unit, E>

val EmptyDataSuccess: Result.Success<Unit> = Result.Success(Unit)