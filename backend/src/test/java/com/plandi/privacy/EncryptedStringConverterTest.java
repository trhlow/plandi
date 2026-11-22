package com.plandi.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

// privacy #9
class EncryptedStringConverterTest {

    static EncryptedStringConverter converterWithRandomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return new EncryptedStringConverter(new SecretKeySpec(key, "AES"));
    }

    @Test
    void decryptsBackToTheOriginal() {
        EncryptedStringConverter converter = converterWithRandomKey();
        String note = "Ăn tối ở quán bờ sông với người yêu";

        assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(note)))
                .isEqualTo(note);
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() {
        EncryptedStringConverter converter = converterWithRandomKey();

        assertThat(converter.convertToDatabaseColumn("ăn chay"))
                .isNotEqualTo(converter.convertToDatabaseColumn("ăn chay"));
    }

    @Test
    void storedValueIsVersionedAndDoesNotContainThePlaintext() {
        String stored = converterWithRandomKey().convertToDatabaseColumn("không ăn hải sản");

        assertThat(stored).startsWith("v1:").doesNotContain("hải sản").doesNotContain("h\\u1ea3i");
    }

    @Test
    void tamperedCiphertextIsRejected() {
        EncryptedStringConverter converter = converterWithRandomKey();
        byte[] raw = Base64.getDecoder()
                .decode(converter.convertToDatabaseColumn("bí mật").substring(3));
        raw[raw.length - 1] ^= 1;
        String tampered = "v1:" + Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anotherKeyCannotDecrypt() {
        String stored = converterWithRandomKey().convertToDatabaseColumn("bí mật");

        assertThatThrownBy(() -> converterWithRandomKey().convertToEntityAttribute(stored))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nullStaysNull() {
        EncryptedStringConverter converter = converterWithRandomKey();

        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
