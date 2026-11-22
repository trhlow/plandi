package com.plandi.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired
    MockMvc mvc;

    static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    static String registerJson(String email) {
        return """
                {"email":"%s","password":"matkhau-123","displayName":"An","area":"TRUNG_TAM","adultConfirmed":true}
                """.formatted(email);
    }

    Cookie register(String email) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie session = result.getResponse().getCookie("SESSION");
        assertThat(session).isNotNull();
        return session;
    }

    @Test
    void registerLogsInAndMeReturnsNormalizedProfile() throws Exception {
        String email = uniqueEmail();
        Cookie session = register("  " + email.toUpperCase() + " ");

        mvc.perform(get("/api/v1/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("An"))
                .andExpect(jsonPath("$.area").value("TRUNG_TAM"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // account #2
    @Test
    void registeringSameEmailWithDifferentCaseReturns409() throws Exception {
        String email = uniqueEmail();
        register(email);

        mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(email.toUpperCase())))
                .andExpect(status().isConflict());
    }

    @Test
    void registerRejectsInvalidInput() throws Exception {
        String email = uniqueEmail();
        String samePasswordAsEmail = """
                {"email":"%s","password":"%s","displayName":"An","area":"TRUNG_TAM","adultConfirmed":true}
                """.formatted(email, email);
        String notAdult = registerJson(email).replace("\"adultConfirmed\":true", "\"adultConfirmed\":false");
        String unknownArea = registerJson(email).replace("TRUNG_TAM", "HA_NOI");

        for (String body : new String[] {samePasswordAsEmail, notAdult, unknownArea}) {
            mvc.perform(post("/api/v1/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    // account #3
    @Test
    void wrongPasswordAndUnknownEmailLookTheSame() throws Exception {
        String email = uniqueEmail();
        register(email);

        MvcResult wrongPassword = mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"sai-mat-khau\"}".formatted(email)))
                .andReturn();
        MvcResult unknownEmail = mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"sai-mat-khau\"}".formatted(uniqueEmail())))
                .andReturn();

        assertThat(wrongPassword.getResponse().getStatus()).isEqualTo(401);
        assertThat(unknownEmail.getResponse().getStatus()).isEqualTo(401);
        assertThat(wrongPassword.getResponse().getContentAsString())
                .isEqualTo(unknownEmail.getResponse().getContentAsString());
    }

    @Test
    void loginWithCorrectPasswordReturns204AndSession() throws Exception {
        String email = uniqueEmail();
        register(email);

        MvcResult login = mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"matkhau-123\"}".formatted(email.toUpperCase())))
                .andExpect(status().isNoContent())
                .andReturn();

        mvc.perform(get("/api/v1/me").cookie(login.getResponse().getCookie("SESSION")))
                .andExpect(status().isOk());
    }

    // account #6
    @Test
    void oldCookieIsRejectedAfterLogout() throws Exception {
        Cookie session = register(uniqueEmail());

        mvc.perform(post("/api/v1/auth/logout").with(csrf()).cookie(session)).andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/me").cookie(session)).andExpect(status().isUnauthorized());
    }

    // account #8
    @Test
    void postWithoutCsrfTokenReturns403() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\",\"password\":\"matkhau-123\"}"))
                .andExpect(status().isForbidden());
    }

    // account #8
    @Test
    void meWithoutLoginReturns401() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }
}
