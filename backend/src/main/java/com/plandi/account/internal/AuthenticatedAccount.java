package com.plandi.account.internal;

import java.io.Serializable;
import java.security.Principal;

/**
 * Principal lưu trong phiên (Redis). Chỉ giữ id và vai trò: không email, không hash mật khẩu. {@link #getName()} là id, để
 * Spring Session đánh chỉ mục phiên theo tài khoản và hủy được mọi phiên của một người khi đổi mật khẩu.
 */
public record AuthenticatedAccount(long id, Role role) implements Principal, Serializable {

    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
