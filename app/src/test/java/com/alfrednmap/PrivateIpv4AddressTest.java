package com.alfrednmap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PrivateIpv4AddressTest {
    @Test
    public void allowsPrivateIpv4Ranges() {
        assertTrue(PrivateIpv4Address.isAllowed("10.0.0.1"));
        assertTrue(PrivateIpv4Address.isAllowed("172.16.0.1"));
        assertTrue(PrivateIpv4Address.isAllowed("172.31.255.254"));
        assertTrue(PrivateIpv4Address.isAllowed("192.168.1.1"));
        assertTrue(PrivateIpv4Address.isAllowed(" 192.168.1.1 "));
    }

    @Test
    public void acceptsPrivateRangeBoundaries() {
        assertTrue(PrivateIpv4Address.isAllowed("10.255.255.255"));
        assertTrue(PrivateIpv4Address.isAllowed("172.16.0.0"));
        assertTrue(PrivateIpv4Address.isAllowed("172.31.255.255"));
        assertTrue(PrivateIpv4Address.isAllowed("192.168.0.0"));
        assertTrue(PrivateIpv4Address.isAllowed("192.168.255.255"));
    }

    @Test
    public void rejectsPublicAndMalformedAddresses() {
        assertFalse(PrivateIpv4Address.isAllowed("8.8.8.8"));
        assertFalse(PrivateIpv4Address.isAllowed("172.32.0.1"));
        assertFalse(PrivateIpv4Address.isAllowed("192.169.1.1"));
        assertFalse(PrivateIpv4Address.isAllowed("256.1.1.1"));
        assertFalse(PrivateIpv4Address.isAllowed("192.168.1"));
        assertFalse(PrivateIpv4Address.isAllowed("192.168.1.1.example"));
        assertFalse(PrivateIpv4Address.isAllowed("010.0.0.1"));
        assertFalse(PrivateIpv4Address.isAllowed("172.15.255.255"));
        assertFalse(PrivateIpv4Address.isAllowed("172.32.0.0"));
        assertFalse(PrivateIpv4Address.isAllowed("192.167.255.255"));
        assertFalse(PrivateIpv4Address.isAllowed("192.169.0.0"));
        assertFalse(PrivateIpv4Address.isAllowed(""));
        assertFalse(PrivateIpv4Address.isAllowed(null));
    }
}
