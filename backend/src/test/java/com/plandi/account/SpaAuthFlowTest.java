package com.plandi.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.plandi.TestcontainersConfiguration;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

// Chạy Tomcat thật với HTTP client giữ cookie như trình duyệt: đúng luồng của web/src/lib/api.ts.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SpaAuthFlowTest {

    @LocalServerPort
    int port;

    @Test
    void spaRegistersWithCsrfCookieAndStaysLoggedIn() throws Exception {
        CookieManager cookies = new CookieManager();
        HttpClient browser = HttpClient.newBuilder().cookieHandler(cookies).build();

        HttpResponse<String> anonymous =
                browser.send(request("/api/v1/me").GET().build(), body());
        assertThat(anonymous.statusCode()).isEqualTo(401);
        assertThat(cookie(cookies, "SESSION"))
                .as("chưa đăng nhập thì không tạo phiên")
                .isNull();
        HttpCookie xsrf = cookie(cookies, "XSRF-TOKEN");
        assertThat(xsrf).isNotNull();
        assertThat(xsrf.isHttpOnly()).isFalse();

        HttpResponse<String> registered = browser.send(
                request("/api/v1/auth/register")
                        .header("Content-Type", "application/json")
                        .header("X-XSRF-TOKEN", xsrf.getValue())
                        .POST(HttpRequest.BodyPublishers.ofString(
                                """
                                {"email":"%s","password":"matkhau-123","displayName":"An","area":"LONG_DUC","adultConfirmed":true}
                                """.formatted("user-" + UUID.randomUUID() + "@example.com")))
                        .build(),
                body());
        assertThat(registered.statusCode()).isEqualTo(201);
        assertThat(registered.headers().allValues("Set-Cookie"))
                .anySatisfy(c -> assertThat(c).startsWith("SESSION=").contains("HttpOnly", "Secure", "SameSite=Lax"));

        // Cookie phiên có cờ Secure nên HttpClient không tự gửi qua http://localhost (Chrome thì có); gửi tay.
        String session = registered.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("SESSION="))
                .findFirst()
                .orElseThrow()
                .split(";")[0];
        HttpResponse<String> me = HttpClient.newHttpClient()
                .send(request("/api/v1/me").header("Cookie", session).GET().build(), body());
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("\"area\":\"LONG_DUC\"");
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    }

    private static HttpResponse.BodyHandler<String> body() {
        return HttpResponse.BodyHandlers.ofString();
    }

    private static HttpCookie cookie(CookieManager cookies, String name) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
