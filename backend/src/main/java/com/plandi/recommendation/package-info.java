/** Pipeline gợi ý kế hoạch vừa ngân sách. Spec: spec/SPEC-recommendation.md. */
@ApplicationModule(allowedDependencies = {"account", "fund", "budget", "catalog", "preference", "privacy", "shared"})
package com.plandi.recommendation;

import org.springframework.modulith.ApplicationModule;
