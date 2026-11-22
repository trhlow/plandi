package com.plandi.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.plandi.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ExportAndDeleteTest {

    static final String PASSWORD = "matkhau-123";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    List<PersonalDataHandler> handlers;

    record User(String email, Cookie session) {}

    User register() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        Cookie session = mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","displayName":"An","area":"TRUNG_TAM","adultConfirmed":true}
                                """.formatted(email, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getCookie("SESSION");
        mvc.perform(put("/api/v1/consents/SPENDING_LOG")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"granted\":true}"))
                .andExpect(status().isNoContent());
        return new User(email, session);
    }

    long idOf(String email) {
        return jdbc.queryForObject("SELECT id FROM account WHERE email = ?", Long.class, email);
    }

    String export(User user) throws Exception {
        return mvc.perform(get("/api/v1/me/export").cookie(user.session()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    int deleteAccount(User user, String password) throws Exception {
        return mvc.perform(delete("/api/v1/me")
                        .with(csrf())
                        .cookie(user.session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"%s\"}".formatted(password)))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    // privacy #4
    @Test
    void exportHasEverySectionAndOnlyTheCallersData() throws Exception {
        User a = register();
        User b = register();

        String json = export(a);

        Map<String, Object> sections = JsonPath.read(json, "$.sections");
        assertThat(sections.keySet())
                .containsExactlyInAnyOrderElementsOf(
                        handlers.stream().map(PersonalDataHandler::section).toList());
        assertThat(json).contains(a.email()).doesNotContain(b.email());
        assertThat(json).doesNotContain(String.valueOf("\"id\":" + idOf(b.email())));
    }

    @Test
    void exportIsLimitedToFivePerDay() throws Exception {
        User user = register();
        for (int i = 0; i < 5; i++) {
            export(user);
        }

        mvc.perform(get("/api/v1/me/export").cookie(user.session())).andExpect(status().isTooManyRequests());
    }

    // privacy #5
    @Test
    void wrongPasswordDeletesNothing() throws Exception {
        User user = register();

        assertThat(deleteAccount(user, "sai-mat-khau")).isEqualTo(403);

        mvc.perform(get("/api/v1/me").cookie(user.session())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM consent_event WHERE account_id = ?", Integer.class, idOf(user.email())))
                .isEqualTo(2);
    }

    // privacy #6: tự áp dụng cho mọi bảng sau này có cột tên kết thúc bằng account_id
    @Test
    void afterDeletionNoTableHasARowForThatAccount() throws Exception {
        User user = register();
        long id = idOf(user.email());

        assertThat(deleteAccount(user, PASSWORD)).isEqualTo(204);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM account WHERE id = ?", Integer.class, id))
                .isZero();
        List<Map<String, Object>> columns =
                jdbc.queryForList("SELECT table_name, column_name FROM information_schema.columns"
                        + " WHERE table_schema = 'public' AND column_name LIKE '%account_id'");
        assertThat(columns).isNotEmpty();
        for (Map<String, Object> column : columns) {
            Integer left = jdbc.queryForObject(
                    "SELECT count(*) FROM \"%s\" WHERE \"%s\" = ?"
                            .formatted(column.get("table_name"), column.get("column_name")),
                    Integer.class,
                    id);
            assertThat(left)
                    .as("%s.%s", column.get("table_name"), column.get("column_name"))
                    .isZero();
        }
    }

    // privacy #7
    @Test
    void afterDeletionOldCookieAndLoginBothFail() throws Exception {
        User user = register();

        assertThat(deleteAccount(user, PASSWORD)).isEqualTo(204);

        mvc.perform(get("/api/v1/me").cookie(user.session())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(user.email(), PASSWORD)))
                .andExpect(status().isUnauthorized());
    }
}
