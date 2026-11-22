package com.plandi.fund;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PersonalFundTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FundAccess fundAccess;

    @Autowired
    FundQuery fundQuery;

    record User(long id, Cookie session) {}

    User register() throws Exception {
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
        return new User(id, session);
    }

    // fund #1
    @Test
    void registeringCreatesExactlyOneActivePersonalFund() throws Exception {
        User user = register();
        long fundId = fundQuery.personalFundOf(user.id());

        mvc.perform(get("/api/v1/funds").cookie(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(fundId))
                .andExpect(jsonPath("$[0].kind").value("PERSONAL"))
                .andExpect(jsonPath("$[0].memberCount").value(1));
        mvc.perform(get("/api/v1/funds/" + fundId).cookie(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("PERSONAL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        assertThat(fundQuery.activeMemberIds(fundId)).containsExactly(user.id());
        assertThat(fundQuery.activeCoupleFundOf(user.id())).isEmpty();
        fundAccess.requireMember(user.id(), fundId);
    }

    // fund #6 (các endpoint đã có): người ngoài quỹ nhận 404, không biết quỹ có tồn tại hay không
    @Test
    void someoneElsesFundLooksLikeItDoesNotExist() throws Exception {
        User a = register();
        User b = register();
        long fundOfB = fundQuery.personalFundOf(b.id());

        mvc.perform(get("/api/v1/funds/" + fundOfB).cookie(a.session())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/funds/999999999").cookie(a.session())).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/funds/" + fundOfB + "/leave").with(csrf()).cookie(a.session()))
                .andExpect(status().isNotFound());
        assertThatThrownBy(() -> fundAccess.requireMember(a.id(), fundOfB)).isInstanceOf(FundNotFoundException.class);
    }

    // fund #9
    @Test
    void personalFundCannotBeLeft() throws Exception {
        User user = register();
        long fundId = fundQuery.personalFundOf(user.id());

        mvc.perform(post("/api/v1/funds/" + fundId + "/leave").with(csrf()).cookie(user.session()))
                .andExpect(status().isBadRequest());
        fundAccess.requireMember(user.id(), fundId);
    }

    // fund #11: DB tự chặn hai quỹ PERSONAL đang hoạt động cho một người, kể cả khi code có bug
    @Test
    void databaseRejectsASecondActivePersonalMembership() throws Exception {
        User user = register();
        Long otherFund = jdbc.queryForObject(
                "INSERT INTO fund (kind, status) VALUES ('PERSONAL', 'ACTIVE') RETURNING id", Long.class);

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO fund_member (fund_id, account_id, fund_kind) VALUES (?, ?, 'PERSONAL')",
                        otherFund,
                        user.id()))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
