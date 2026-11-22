package com.plandi.fund;

/** Phát đồng bộ ngay trước khi xóa dòng {@code fund}; module khác xóa dữ liệu theo quỹ đó (không cần khóa ngoại chéo module). */
public record FundDeleted(long fundId) {}
