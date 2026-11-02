package com.alfrednmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public final class PrivateNetworkRangeTest {
    @Test
    public void limitsLargerSubnetsToLocalPrivateSlash24() {
        List<String> addresses =
                PrivateNetworkRange.addressesToCheck("10.12.34.56", 16);

        assertEquals(253, addresses.size());
        assertEquals("10.12.34.1", addresses.get(0));
        assertEquals("10.12.34.254", addresses.get(addresses.size() - 1));
        assertTrue(!addresses.contains("10.12.34.56"));
    }

    @Test
    public void respectsSmallerSubnetRanges() {
        List<String> addresses =
                PrivateNetworkRange.addressesToCheck("192.168.1.70", 26);

        assertEquals(61, addresses.size());
        assertEquals("192.168.1.65", addresses.get(0));
        assertEquals("192.168.1.126", addresses.get(addresses.size() - 1));
        assertTrue(!addresses.contains("192.168.1.70"));
    }

    @Test
    public void scansOnlyUsableAddressesInsideSmallSubnet() {
        List<String> addresses =
                PrivateNetworkRange.addressesToCheck("192.168.1.10", 30);

        assertEquals(1, addresses.size());
        assertEquals("192.168.1.9", addresses.get(0));
    }

    @Test
    public void privateClassARangeStillScansOnlyLocalSlash24() {
        List<String> addresses =
                PrivateNetworkRange.addressesToCheck("10.255.0.10", 8);

        assertEquals(253, addresses.size());
        assertEquals("10.255.0.1", addresses.get(0));
        assertEquals("10.255.0.254", addresses.get(addresses.size() - 1));
    }

    @Test
    public void trimsAddressLikeThePrivateAddressValidator() {
        assertEquals(
                PrivateNetworkRange.addressesToCheck("192.168.1.10", 24),
                PrivateNetworkRange.addressesToCheck(" 192.168.1.10 ", 24));
    }

    @Test
    public void everyGeneratedAddressIsInsideLocalRangeAndExcludesNetworkAndBroadcast() {
        List<String> addresses =
                PrivateNetworkRange.addressesToCheck("172.20.5.77", 23);

        assertEquals(253, addresses.size());
        assertEquals("172.20.5.1", addresses.get(0));
        assertEquals("172.20.5.254", addresses.get(addresses.size() - 1));
        assertTrue(!addresses.contains("172.20.5.0"));
        assertTrue(!addresses.contains("172.20.5.255"));
        assertTrue(!addresses.contains("172.20.5.77"));
        for (String address : addresses) {
            assertTrue(address.startsWith("172.20.5."));
        }
    }

    @Test
    public void rejectsPublicAddressesAndUnsupportedMasks() {
        assertTrue(PrivateNetworkRange.addressesToCheck("8.8.8.8", 24).isEmpty());
        assertTrue(PrivateNetworkRange.addressesToCheck("192.168.1.10", 31).isEmpty());
        assertTrue(PrivateNetworkRange.addressesToCheck("192.168.1.10", 0).isEmpty());
        assertTrue(PrivateNetworkRange.addressesToCheck("192.168.1.10", 32).isEmpty());
        assertTrue(PrivateNetworkRange.addressesToCheck("192.168.1.10", -1).isEmpty());
        assertTrue(PrivateNetworkRange.addressesToCheck("192.168.1", 24).isEmpty());
    }
}
