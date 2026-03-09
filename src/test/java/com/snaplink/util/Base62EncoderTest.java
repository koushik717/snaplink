package com.snaplink.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    void encode_zero_returnsZero() {
        assertEquals("0", encoder.encode(0));
    }

    @Test
    void encode_one_returnsOne() {
        assertEquals("1", encoder.encode(1));
    }

    @Test
    void encode_62_returns10() {
        assertEquals("10", encoder.encode(62));
    }

    @Test
    void encode_1000_returnsG8() {
        assertEquals("G8", encoder.encode(1000));
    }

    @Test
    void encode_largNumber() {
        String result = encoder.encode(1000000000L);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void decode_roundTrip() {
        for (long id : new long[]{0, 1, 62, 1000, 999999, 1000000000L}) {
            String encoded = encoder.encode(id);
            long decoded = encoder.decode(encoded);
            assertEquals(id, decoded, "Roundtrip failed for id: " + id);
        }
    }

    @Test
    void decode_invalidCharacter_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> encoder.decode("abc!"));
    }

    @Test
    void encode_sequentialIds_uniqueCodes() {
        String code1 = encoder.encode(1);
        String code2 = encoder.encode(2);
        String code3 = encoder.encode(3);
        assertNotEquals(code1, code2);
        assertNotEquals(code2, code3);
    }
}
