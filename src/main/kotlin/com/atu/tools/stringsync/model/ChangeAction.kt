package com.atu.tools.stringsync.model

enum class ChangeAction(val label: String) {
    ADD("THÊM"),
    UPDATE("CẬP NHẬT"),
    SKIP_ALREADY_EXISTS("BỎ QUA: ĐÃ TỒN TẠI"),
    SKIP_NOT_EXISTS_FOR_UPDATE("BỎ QUA: CHƯA TỒN TẠI ĐỂ CẬP NHẬT"),
    SKIP_MISSING_IN_SHEET("BỎ QUA: SHEET THIẾU BẢN DỊCH"),
    ERROR_INVALID_KEY("LỖI: KEY KHÔNG HỢP LỆ"),
    ERROR_XML_INJECTION("LỖI: NGHI XML INJECTION"),
    ERROR_PLACEHOLDER_MISMATCH("LỖI: SAI PLACEHOLDER"),
    ERROR_FILE("LỖI FILE")
}
