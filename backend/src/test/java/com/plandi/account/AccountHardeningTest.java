package com.plandi.account;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AccountHardeningTest {

    static final String PASSWORD = "matkhau-123";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    // Mỗi test dùng một IP riêng để bộ đếm đăng nhập sai không lẫn giữa các test.
    static MockHttpServletRequestBuilder fromIp(MockHttpServletRequestBuilder request, String ip) {
        return request.with(r -> {
            r.setRemoteAddr(ip);
            return r;
        });
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

    int login(String email, String password, String ip) throws Exception {
        return mvc.perform(fromIp(post("/api/v1/auth/login"), ip)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    Cookie loginCookie(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isNoContent())
                .andReturn()
                .getResponse()
                .getCookie("SESSION");
    }

    // account #4
    @Test
    void sixthAttemptWithin15MinutesIsLockedEvenWithCorrectPassword() throws Exception {
        String email = uniqueEmail();
        register(email);

        for (int i = 0; i < 5; i++) {
            assertThat(login(email, "sai-mat-khau", "10.0.0.4")).isEqualTo(401);
        }

        assertThat(login(email, PASSWORD, "10.0.0.4")).isEqualTo(429);
        assertThat(login(email, PASSWORD, "10.0.0.5"))
                .as("khóa theo email + IP: IP khác vẫn đăng nhập được")
                .isEqualTo(204);
    }

    // account #7
    @Test
    void changingPasswordLogsOutOtherDevicesButKeepsCurrentSession() throws Exception {
        String email = uniqueEmail();
        register(email);
        Cookie phone = loginCookie(email, PASSWORD);
        Cookie laptop = loginCookie(email, PASSWORD);

        mvc.perform(put("/api/v1/me/password")
                        .with(csrf())
                        .cookie(phone)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"currentPassword\":\"%s\",\"newPassword\":\"matkhau-moi-456\"}".formatted(PASSWORD)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/me").cookie(laptop)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/me").cookie(phone)).andExpect(status().isOk());
        assertThat(login(email, PASSWORD, "10.0.0.7")).isEqualTo(401);
        assertThat(login(email, "matkhau-moi-456", "10.0.0.7")).isEqualTo(204);
    }

    @Test
    void changingPasswordRequiresCurrentPassword() throws Exception {
        String email = uniqueEmail();
        register(email);
        Cookie session = loginCookie(email, PASSWORD);

        mvc.perform(put("/api/v1/me/password")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"sai-mat-khau\",\"newPassword\":\"matkhau-moi-456\"}"))
                .andExpect(status().isBadRequest());
    }

    // account #9
    @Test
    void adminEndpointsAreForAdminsOnly() throws Exception {
        String user = uniqueEmail();
        String admin = uniqueEmail();
        register(user);
        register(admin);
        jdbc.update("UPDATE account SET role = 'ADMIN' WHERE email = ?", admin);

        mvc.perform(get("/api/v1/admin/anything").cookie(loginCookie(user, PASSWORD)))
                .andExpect(status().isForbidden());
        // Đường dẫn không tồn tại: ADMIN qua được lớp phân quyền và nhận 404 (endpoint admin thật: PasswordResetTest).
        mvc.perform(get("/api/v1/admin/anything").cookie(loginCookie(admin, PASSWORD)))
                .andExpect(status().isNotFound());
    }

    // account #10
    @Test
    void logsNeverContainEmail(CapturedOutput output) throws Exception {
        String email = uniqueEmail();
        register(email);
        login(email, "sai-mat-khau", "10.0.0.10");
        login(email, PASSWORD, "10.0.0.10");

        assertThat(output.getAll()).doesNotContain(email);
    }

    @Test
    void profileCanBeUpdated() throws Exception {
        String email = uniqueEmail();
        register(email);
        Cookie session = loginCookie(email, PASSWORD);

        mvc.perform(patch("/api/v1/me")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"  Bình  \",\"area\":\"AO_BA_OM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Bình"))
                .andExpect(jsonPath("$.area").value("AO_BA_OM"));

        mvc.perform(patch("/api/v1/me")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void areasArePublicWithCentreCoordinates() throws Exception {
        mvc.perform(get("/api/v1/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].code").value("TRUNG_TAM"))
                .andExpect(jsonPath("$[0].name").value("Trung tâm (chợ Trà Vinh, Vincom)"))
                .andExpect(jsonPath("$[0].lat").isNumber())
                .andExpect(jsonPath("$[0].lng").isNumber());
    }
}
