package com.plandi.account;

import com.plandi.shared.Area;

public record AccountView(long id, String email, String displayName, Area area, String role) {}
