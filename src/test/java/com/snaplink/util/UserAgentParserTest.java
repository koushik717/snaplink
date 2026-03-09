package com.snaplink.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserAgentParserTest {

    private final UserAgentParser parser = new UserAgentParser();

    @Test
    void parseDeviceType_mobile() {
        assertEquals("mobile", parser.parseDeviceType("Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X)"));
        assertEquals("mobile", parser.parseDeviceType("Mozilla/5.0 (Linux; Android 13; Pixel 7)"));
    }

    @Test
    void parseDeviceType_tablet() {
        assertEquals("tablet", parser.parseDeviceType("Mozilla/5.0 (iPad; CPU OS 16_0 like Mac OS X)"));
    }

    @Test
    void parseDeviceType_desktop() {
        assertEquals("desktop", parser.parseDeviceType("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/120.0"));
    }

    @Test
    void parseDeviceType_nullOrEmpty() {
        assertEquals("unknown", parser.parseDeviceType(null));
        assertEquals("unknown", parser.parseDeviceType(""));
    }

    @Test
    void parseBrowser_chrome() {
        assertEquals("Chrome", parser.parseBrowser("Mozilla/5.0 (Macintosh) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));
    }

    @Test
    void parseBrowser_safari() {
        assertEquals("Safari", parser.parseBrowser("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"));
    }

    @Test
    void parseBrowser_firefox() {
        assertEquals("Firefox", parser.parseBrowser("Mozilla/5.0 (X11; Linux x86_64; rv:120.0) Gecko/20100101 Firefox/120.0"));
    }

    @Test
    void parseBrowser_nullOrEmpty() {
        assertEquals("unknown", parser.parseBrowser(null));
        assertEquals("unknown", parser.parseBrowser(""));
    }
}
