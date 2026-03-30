package com.atu.tools.stringsync.model

enum class SyncMode(val uiLabel: String) {
    ADD_ALL("1. Thêm tất cả ( thêm toàn bộ file string vào project)"),
    ADD_MISSING("2. Chỉ thêm key còn thiếu (chỉ add những key chưa có trong file strings.xml)"),
    UPDATE_ALL("3. Cập nhật tất cả ( add những string chưa có và update lại những string đã tồn tại)"),
    UPDATE_CHANGED("4. Chỉ cập nhật key đã có ( chỉ update lại string đã tồn tại)");

    override fun toString(): String = uiLabel
}
