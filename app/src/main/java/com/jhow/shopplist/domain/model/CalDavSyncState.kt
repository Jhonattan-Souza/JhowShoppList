package com.jhow.shopplist.domain.model

enum class CalDavSyncState {
    Disabled,
    Idle,
    Syncing,
    Success,
    AuthError,
    NetworkError,
    MissingList,
    AmbiguousListName,
    UserActionRequired,
    Warning
}
