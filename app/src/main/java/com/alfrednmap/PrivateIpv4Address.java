package com.alfrednmap;

final class PrivateIpv4Address {
    private PrivateIpv4Address() {}

    static boolean isAllowed(String address) {
        if (address == null) {
            return false;
        }

        String[] parts = address.trim().split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }

        int[] octets = new int[4];
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()
                    || (parts[i].length() > 1 && parts[i].startsWith("0"))
                    || !parts[i].matches("[0-9]{1,3}")) {
                return false;
            }
            try {
                octets[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException exception) {
                return false;
            }
            if (octets[i] > 255) {
                return false;
            }
        }

        return octets[0] == 10
                || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
                || (octets[0] == 192 && octets[1] == 168);
    }
}
