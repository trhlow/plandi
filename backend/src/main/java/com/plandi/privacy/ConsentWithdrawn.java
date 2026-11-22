package com.plandi.privacy;

/** Module nào giữ dữ liệu theo purpose này tự nghe event và dọn dữ liệu của mình (spec của từng module). */
public record ConsentWithdrawn(long accountId, Purpose purpose) {}
