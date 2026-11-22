package com.plandi.account.internal;

import java.io.Serializable;

/** Principal lưu trong phiên (Redis). Chỉ giữ id và vai trò: không email, không hash mật khẩu. */
public record AuthenticatedAccount(long id, Role role) implements Serializable {}
