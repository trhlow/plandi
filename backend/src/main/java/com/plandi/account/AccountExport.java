package com.plandi.account;

import com.plandi.shared.Area;
import java.time.Instant;

public record AccountExport(String email, String displayName, Area area, String role, Instant createdAt) {}
