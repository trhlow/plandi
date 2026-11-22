package com.plandi.account;

/** Phát đồng bộ trong transaction đăng ký: listener lỗi thì đăng ký rollback (module fund tạo quỹ Cá nhân từ event này). */
public record AccountRegistered(long accountId) {}
