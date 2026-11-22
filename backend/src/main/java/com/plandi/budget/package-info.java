/** Mục con, ngân sách tháng, khoản chi. Spec: spec/SPEC-budget.md. */
@ApplicationModule(allowedDependencies = {"account", "fund", "catalog", "privacy", "shared"})
package com.plandi.budget;

import org.springframework.modulith.ApplicationModule;
