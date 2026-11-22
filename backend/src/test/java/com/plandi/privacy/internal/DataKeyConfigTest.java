package com.plandi.privacy.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DataKeyConfigTest {

    final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(DataKeyConfig.class);

    // privacy #10
    @Test
    void prodWithoutKeyDoesNotStart() {
        runner.withInitializer(ctx -> ctx.getEnvironment().setActiveProfiles("prod"))
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void prodWithA32ByteKeyStarts() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        runner.withInitializer(ctx -> ctx.getEnvironment().setActiveProfiles("prod"))
                .withPropertyValues("plandi.data-key=" + key)
                .run(ctx -> assertThat(ctx).hasSingleBean(SecretKey.class));
    }

    @Test
    void keyOfTheWrongLengthDoesNotStart() {
        String key = Base64.getEncoder().encodeToString(new byte[16]);
        runner.withPropertyValues("plandi.data-key=" + key)
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void devAndTestWithoutKeyGetAThrowawayKey() {
        runner.run(ctx -> assertThat(ctx).hasSingleBean(SecretKey.class));
    }
}
