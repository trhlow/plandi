package com.plandi.account;

import com.plandi.shared.Area;
import jakarta.validation.constraints.Size;

/** PATCH /me: trường nào null thì giữ nguyên. */
public record ProfileUpdate(@Size(min = 1, max = 50) String displayName, Area area) {

    public ProfileUpdate {
        displayName = displayName == null ? null : displayName.strip();
    }
}
