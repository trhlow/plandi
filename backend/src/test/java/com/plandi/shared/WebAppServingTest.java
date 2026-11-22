package com.plandi.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.plandi.TestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

// Chạy Tomcat thật: kiểu MIME của file tĩnh do servlet container quyết định, MockMvc không thấy được.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class WebAppServingTest {

    @LocalServerPort
    int port;

    @Test
    void manifestHasPwaContentType() throws Exception {
        HttpResponse<Void> response = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/manifest.webmanifest"))
                                .build(),
                        HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type"))
                .hasValueSatisfying(type -> assertThat(type).startsWith("application/manifest+json"));
    }
}
