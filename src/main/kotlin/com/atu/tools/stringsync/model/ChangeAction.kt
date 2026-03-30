package com.atu.tools.stringsync.model

enum class ChangeAction {
    ADD,
    UPDATE,
    SKIP_ALREADY_EXISTS,
    SKIP_MISSING_IN_SHEET,
    ERROR_INVALID_KEY,
    ERROR_XML_INJECTION,
    ERROR_PLACEHOLDER_MISMATCH,
    ERROR_FILE
}
