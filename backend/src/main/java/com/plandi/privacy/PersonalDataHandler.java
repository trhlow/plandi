package com.plandi.privacy;

/**
 * Mỗi module giữ dữ liệu cá nhân tự cài interface này (bean Spring), privacy gom lại khi xuất và xóa tài khoản. Bảng mới
 * có cột {@code ..._account_id} mà quên cài handler thì test quét của privacy (#6) sẽ fail.
 */
public interface PersonalDataHandler {

    /** Tên mục trong file xuất, ví dụ "account", "fund", "budget". */
    String section();

    /** Dữ liệu của chính người này (record), được serialize vào file JSON. */
    Object export(long accountId);

    /** Xóa mọi dòng của người này; chạy trong transaction của privacy, lỗi thì rollback toàn bộ. */
    void delete(long accountId);

    /** Số nhỏ chạy trước. Account chạy cuối vì các bảng khác trỏ tới nó. */
    int deleteOrder();
}
