package com.plandi.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.plandi.TestcontainersConfiguration;
import com.plandi.shared.Area;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

// account #1
@Import(TestcontainersConfiguration.class)
@ApplicationModuleTest
class AccountRegisteredEventTest {

    @Autowired
    AccountService accounts;

    @Test
    void registeringPublishesAccountRegistered(Scenario scenario) {
        var request = new RegistrationRequest(
                "user-" + UUID.randomUUID() + "@example.com", "matkhau-123", "An", Area.TRUNG_TAM, true);

        scenario.stimulate(() -> accounts.register(request))
                .andWaitForEventOfType(AccountRegistered.class)
                .toArriveAndVerify(
                        (event, account) -> assertThat(event.accountId()).isEqualTo(account.id()));
    }
}
