package com.plandi;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    // Fail khi module dùng `internal` của module khác, có vòng phụ thuộc,
    // hoặc phụ thuộc ngoài allowedDependencies trong package-info (theo spec/capability-map.md).
    @Test
    void modulesFollowCapabilityMap() {
        ApplicationModules.of(PlandiApplication.class).verify();
    }
}
