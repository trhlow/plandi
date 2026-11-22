package com.plandi.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plandi.TestcontainersConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.http.MediaType;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.test.web.servlet.MockMvc;

// account #5: phiên nằm trong Redis chứ không nằm trong bộ nhớ app, nên app khởi động lại vẫn đọc được.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SessionPersistenceTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    RedisConnectionFactory redis;

    @Test
    void sessionSurvivesInRedisForANewAppInstance() throws Exception {
        String cookie = mvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"matkhau-123","displayName":"An","area":"DAI_HOC","adultConfirmed":true}
                                """.formatted("user-" + UUID.randomUUID() + "@example.com")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getCookie("SESSION")
                .getValue();
        String sessionId = new String(Base64.getDecoder().decode(cookie), StandardCharsets.UTF_8);

        // Một repository mới, không chia sẻ gì với app đang chạy ngoài Redis: giống một instance vừa khởi động.
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redis);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.afterPropertiesSet();
        Session restored = new RedisSessionRepository(template).findById(sessionId);

        assertThat(restored).isNotNull();
        assertThat((Object) restored.getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();
    }
}
