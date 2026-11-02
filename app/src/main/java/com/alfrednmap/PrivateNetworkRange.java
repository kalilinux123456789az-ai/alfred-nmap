package com.alfrednmap;

import java.util.ArrayList;
import java.util.List;

final class PrivateNetworkRange {
    private PrivateNetworkRange() {}

    static List<String> addressesToCheck(String localAddress, int prefixLength) {
        List<String> addresses = new ArrayList<>();
        if (!PrivateIpv4Address.isAllowed(localAddress)
                || prefixLength < 8
                || prefixLength > 30) {
            return addresses;
        }

        int address = toInt(localAddress.trim());
        int scanPrefix = Math.max(prefixLength, 24);
        int mask = (int) (0xffffffffL << (32 - scanPrefix));
        int network = address & mask;
        int broadcast = network | ~mask;
        for (int candidate = network + 1; candidate < broadcast; candidate++) {
            if (candidate != address) {
                addresses.add(toAddress(candidate));
            }
        }
        return addresses;
    }

    private static int toInt(String address) {
        String[] parts = address.split("\\.");
        int value = 0;
        for (String part : parts) {
            value = (value << 8) | Integer.parseInt(part);
        }
        return value;
    }

    private static String toAddress(int address) {
        return ((address >>> 24) & 255) + "."
                + ((address >>> 16) & 255) + "."
                + ((address >>> 8) & 255) + "."
                + (address & 255);
    }
}
