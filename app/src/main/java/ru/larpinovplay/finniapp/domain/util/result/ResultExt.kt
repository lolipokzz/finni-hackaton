package ru.larpinovplay.finniapp.domain.util.result

inline fun <D, E: DomainError, R> Result<D, E>.map(map: (D) -> R): Result<R, E> {
    return when(this) {
        is Result.Error -> Result.Error(error)
        is Result.Success -> Result.Success(map(data))
    }
}

fun <D, E: DomainError> Result<D, E>.asEmptyDataResult(): EmptyResult<E> {
    return map {  }
}

inline fun <D, E : DomainError> Result<D, E>.onSuccess(
    action: (D) -> Unit
): Result<D, E> {
    if (this is Result.Success) {
        action(data)
    }

    return this
}

inline fun <D, E : DomainError> Result<D, E>.onError(
    action: (E) -> Unit
): Result<D, E> {
    if (this is Result.Error) {
        action(error)
    }

    return this
}

fun <D, E: DomainError> Result<D, E>.dataOrNull(): D? {
    return when(this) {
        is Result.Error -> null
        is Result.Success -> data
    }
}