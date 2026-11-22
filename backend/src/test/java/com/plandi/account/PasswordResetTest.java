package com.plandi.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.plandi.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class PasswordResetTest {

    static final String PASSWORD = "matkhau-123";
    static final String NEW_PASSWORD = "matkhau-moi-456";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    void register(String email) throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"An","area":"TRUNG_TAM","adultConfirmed":true}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated());
    }

    Cookie login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("SESSION");
    }

    int loginStatus(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    Cookie adminSession() throws Exception {
        String admin = uniqueEmail();
        register(admin);
        jdbc.update("UPDATE account SET role = 'ADMIN' WHERE email = ?", admin);
        return login(admin, PASSWORD);
    }

    String createCode(Cookie admin, String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/admin/password-resets")
                        .with(csrf())
                        .cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.code");
    }

    MvcResult reset(String email, String code, String ip) throws Exception {
        return mvc.perform(post("/api/v1/auth/reset-password")
                        .with(csrf())
                        .with(r -> {
                            r.setRemoteAddr(ip);
                            return r;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"code\":\"%s\",\"newPassword\":\"%s\"}"
                                .formatted(email, code, NEW_PASSWORD)))
                .andReturn();
    }

    // account #9 (phần ADMIN), cùng quy tắc chỉ lưu hash của mã
    @Test
    void onlyAdminsCreateCodesAndOnlyTheHashIsStored() throws Exception {
        String email = uniqueEmail();
        register(email);

        mvc.perform(post("/api/v1/admin/password-resets")
                        .with(csrf())
                        .cookie(login(email, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isForbidden());

        String code = createCode(adminSession(), email);

        assertThat(code).hasSize(8);
        String stored = jdbc.queryForObject(
                "SELECT r.code_hash FROM password_reset r JOIN account a ON a.id = r.account_id WHERE a.email = ?",
                String.class,
                email);
        assertThat(stored).doesNotContain(code).startsWith("{bcrypt}");
    }

    // account #12
    @Test
    void resetLogsOutEverySessionAndReplacesThePassword() throws Exception {
        String email = uniqueEmail();
        register(email);
        Cookie phone = login(email, PASSWORD);
        Cookie laptop = login(email, PASSWORD);
        String code = createCode(adminSession(), email);

        assertThat(reset(email, code, "10.1.0.12").getResponse().getStatus()).isEqualTo(204);

        mvc.perform(get("/api/v1/me").cookie(phone)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").cookie(laptop)).andExpect(status().isUnauthorized());
        assertThat(loginStatus(email, PASSWORD)).isEqualTo(401);
        assertThat(loginStatus(email, NEW_PASSWORD)).isEqualTo(204);
    }

    // account #11
    @Test
    void wrongExpiredUsedAndReplacedCodesAllGetTheSame400() throws Exception {
        Cookie admin = adminSession();

        String wrongEmail = uniqueEmail();
        register(wrongEmail);
        createCode(admin, wrongEmail);
        MvcResult wrong = reset(wrongEmail, "ZZZZZZZZ", "10.1.0.11");

        String expiredEmail = uniqueEmail();
        register(expiredEmail);
        String expiredCode = createCode(admin, expiredEmail);
        jdbc.update(
                "UPDATE password_reset SET expires_at = now() - interval '1 minute'"
                        + " WHERE account_id = (SELECT id FROM account WHERE email = ?)",
                expiredEmail);
        MvcResult expired = reset(expiredEmail, expiredCode, "10.1.0.11");

        String usedEmail = uniqueEmail();
        register(usedEmail);
        String usedCode = createCode(admin, usedEmail);
        assertThat(reset(usedEmail, usedCode, "10.1.0.11").getResponse().getStatus())
                .isEqualTo(204);
        MvcResult used = reset(usedEmail, usedCode, "10.1.0.11");

        String replacedEmail = uniqueEmail();
        register(replacedEmail);
        String oldCode = createCode(admin, replacedEmail);
        createCode(admin, replacedEmail);
        MvcResult replaced = reset(replacedEmail, oldCode, "10.1.0.11");

        for (MvcResult result : new MvcResult[] {wrong, expired, used, replaced}) {
            assertThat(result.getResponse().getStatus()).isEqualTo(400);
            assertThat(result.getResponse().getContentAsString())
                    .isEqualTo(wrong.getResponse().getContentAsString());
        }
    }

    @Test
    void wrongCodesAreThrottledLikeLogins() throws Exception {
        String email = uniqueEmail();
        register(email);
        String code = createCode(adminSession(), email);

        for (int i = 0; i < 5; i++) {
            assertThat(reset(email, "ZZZZZZZZ", "10.1.0.13").getResponse().getStatus())
                    .isEqualTo(400);
        }

        assertThat(reset(email, code, "10.1.0.13").getResponse().getStatus()).isEqualTo(429);
    }

    // account #10 (phần đặt lại mật khẩu)
    @Test
    void logsNeverContainEmailOrCode(CapturedOutput output) throws Exception {
        String email = uniqueEmail();
        register(email);
        String code = createCode(adminSession(), email);
        reset(email, "ZZZZZZZZ", "10.1.0.10");
        reset(email, code, "10.1.0.10");

        assertThat(output.getAll()).doesNotContain(email).doesNotContain(code);
    }
}
