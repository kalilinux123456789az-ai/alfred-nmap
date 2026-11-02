package com.alfrednmap;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

final class DeviceNameFormatter {
    private DeviceNameFormatter() {}

    static String displayName(String address, String hostName) {
        if (hostName == null) {
            return "Unknown device";
        }

        String normalized = hostName.trim();
        if (normalized.isEmpty()
                || normalized.equals(address)
                || isIpv4Literal(normalized)) {
            return "Unknown device";
        }
        return normalized;
    }

    private static boolean isIpv4Literal(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            return false;
        }
        try {
            return InetAddress.getByName(normalized).getHostAddress().equals(normalized);
        } catch (UnknownHostException exception) {
            return false;
        }
    }
}
