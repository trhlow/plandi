package com.plandi.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plandi.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class ConsentTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ApplicationEvents events;

    String email;

    Cookie register() throws Exception {
        email = "user-" + UUID.randomUUID() + "@example.com";
        return mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"matkhau-123","displayName":"An","area":"TRUNG_TAM","adultConfirmed":true}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getCookie("SESSION");
    }

    long accountId() {
        return jdbc.queryForObject("SELECT id FROM account WHERE email = ?", Long.class, email);
    }

    void putConsent(Cookie session, String purpose, boolean granted, int expectedStatus) throws Exception {
        mvc.perform(put("/api/v1/consents/" + purpose)
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"granted\":" + granted + "}"))
                .andExpect(status().is(expectedStatus));
    }

    // privacy #1
    @Test
    void newAccountHasTermsWithPolicyVersionAndNothingElse() throws Exception {
        Cookie session = register();

        mvc.perform(get("/api/v1/consents").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyVersion").value(ConsentService.POLICY_VERSION))
                .andExpect(jsonPath("$.consents.TERMS").value(true))
                .andExpect(jsonPath("$.consents.SPENDING_LOG").value(false))
                .andExpect(jsonPath("$.consents.PERSONALIZATION").value(false))
                .andExpect(jsonPath("$.consents.FOOD_FILTERS").value(false));
        assertThat(jdbc.queryForObject(
                        "SELECT policy_version FROM consent_event WHERE account_id = ? AND purpose = 'TERMS'",
                        String.class,
                        accountId()))
                .isEqualTo(ConsentService.POLICY_VERSION);
    }

    // privacy #2
    @Test
    void grantingThenWithdrawingKeepsBothRowsAndShowsTheLatest() throws Exception {
        Cookie session = register();

        putConsent(session, "SPENDING_LOG", true, 204);
        putConsent(session, "SPENDING_LOG", false, 204);

        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM consent_event WHERE account_id = ? AND purpose = 'SPENDING_LOG'",
                        Integer.class,
                        accountId()))
                .isEqualTo(2);
        mvc.perform(get("/api/v1/consents").cookie(session))
                .andExpect(jsonPath("$.consents.SPENDING_LOG").value(false));
    }

    // privacy #3
    @Test
    void withdrawingPublishesConsentWithdrawnAndTermsCannotBeWithdrawn() throws Exception {
        Cookie session = register();

        putConsent(session, "PERSONALIZATION", true, 204);
        assertThat(events.stream(ConsentWithdrawn.class)).isEmpty();
        putConsent(session, "PERSONALIZATION", false, 204);

        assertThat(events.stream(ConsentWithdrawn.class))
                .containsExactly(new ConsentWithdrawn(accountId(), Purpose.PERSONALIZATION));
        putConsent(session, "TERMS", false, 400);
        putConsent(session, "KHONG_CO", true, 400);
    }

    // privacy #11
    @Test
    void newPolicyVersionBlocksTheApiUntilTermsAreAcceptedAgain() throws Exception {
        Cookie session = register();
        jdbc.update("UPDATE consent_event SET policy_version = '2000-01' WHERE account_id = ?", accountId());

        mvc.perform(patch("/api/v1/me")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Bình\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TERMS_REQUIRED"));
        // Vẫn xem được mình là ai và trạng thái đồng ý, để web hiện màn hình đồng ý lại.
        mvc.perform(get("/api/v1/me").cookie(session)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/consents").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consents.TERMS").value(false));

        putConsent(session, "TERMS", true, 204);

        mvc.perform(patch("/api/v1/me")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Bình\"}"))
                .andExpect(status().isOk());
    }
}
