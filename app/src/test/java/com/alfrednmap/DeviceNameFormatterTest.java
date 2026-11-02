package com.alfrednmap;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class DeviceNameFormatterTest {
    @Test
    public void displaysHostNamesWhenAvailable() {
        assertEquals(
                "living-room-tv",
                DeviceNameFormatter.displayName("192.168.1.20", "living-room-tv"));
        assertEquals(
                "printer.local",
                DeviceNameFormatter.displayName("192.168.1.30", " printer.local "));
    }

    @Test
    public void usesUnknownLabelWhenLookupReturnsOnlyAnAddress() {
        assertEquals(
                "Unknown device",
                DeviceNameFormatter.displayName("192.168.1.20", "192.168.1.20"));
        assertEquals("Unknown device", DeviceNameFormatter.displayName("192.168.1.20", null));
        assertEquals("Unknown device", DeviceNameFormatter.displayName("192.168.1.20", "  "));
        assertEquals(
                "Unknown device",
                DeviceNameFormatter.displayName("192.168.1.20", "192.168.1.99"));
    }
}
