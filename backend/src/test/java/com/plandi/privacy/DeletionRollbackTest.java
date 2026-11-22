package com.plandi.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plandi.TestcontainersConfiguration;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

// privacy #8: một handler lỗi giữa chừng thì không dòng nào bị xóa, kể cả của handler đã chạy trước nó.
@Import({TestcontainersConfiguration.class, DeletionRollbackTest.FailingHandlerConfig.class})
@SpringBootTest
@AutoConfigureMockMvc
class DeletionRollbackTest {

    @TestConfiguration
    static class FailingHandlerConfig {
        @Bean
        PersonalDataHandler failingHandler() {
            return new PersonalDataHandler() {
                @Override
                public String section() {
                    return "failing";
                }

                @Override
                public Object export(long accountId) {
                    return null;
                }

                @Override
                public void delete(long accountId) {
                    throw new IllegalStateException("handler lỗi giả lập");
                }

                @Override
                public int deleteOrder() {
                    return 500; // sau consent (privacy), trước account
                }
            };
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void failingHandlerRollsBackEverything() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        Cookie session = mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"matkhau-123","displayName":"An","area":"TRUNG_TAM","adultConfirmed":true}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getCookie("SESSION");
        long id = jdbc.queryForObject("SELECT id FROM account WHERE email = ?", Long.class, email);

        // MockMvc ném lỗi ra ngoài thay vì trả 500 như Tomcat; điều cần kiểm tra là dữ liệu còn nguyên.
        assertThatThrownBy(() -> mvc.perform(delete("/api/v1/me")
                        .with(csrf())
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"matkhau-123\"}")))
                .hasRootCauseMessage("handler lỗi giả lập");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM consent_event WHERE account_id = ?", Integer.class, id))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM account WHERE id = ?", Integer.class, id))
                .isEqualTo(1);
        mvc.perform(get("/api/v1/me").cookie(session)).andExpect(status().isOk());
    }
}
