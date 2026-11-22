package com.plandi.privacy;

/** Mục đích xử lý dữ liệu, đồng ý riêng từng cái (NĐ 356/2025). Bảng đầy đủ: spec/SPEC-privacy.md. */
public enum Purpose {
    /** Điều khoản và chính sách: bắt buộc, ghi lúc đăng ký, không rút riêng được. */
    TERMS,
    SPENDING_LOG,
    PERSONALIZATION,
    FOOD_FILTERS
}
