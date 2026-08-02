/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.penguinehis.ultrasshservice.tunnel.vpn;

import java.net.InetAddress;
import java.util.Locale;

public class CIDRIP {
    String mIp;
    int len;

    public CIDRIP(String address, int prefix_length) {
        len = prefix_length;
        mIp = address;
    }

    public CIDRIP(String address, long prefix_length) {
        len = (int) prefix_length;
        mIp = address;
    }

    @Override
    public String toString() {
        if (InetAddressUtils.isIPv4Address(mIp)) {
            return String.format(Locale.ENGLISH, "%s/%d", mIp, len);
        } else if (InetAddressUtils.isIPv6Address(mIp)) {
            return String.format(Locale.ENGLISH, "%s/%d", mIp, (long) len);
        } else {
            return mIp;
        }
    }

    static class InetAddressUtils {
        private static final int INET4_ADDRESS_LENGTH = 4;
        private static final int INET6_ADDRESS_LENGTH = 16;

        public static boolean isIPv4Address(String input) {
            try {
                InetAddress inetAddress = InetAddress.getByName(input);
                return inetAddress.getAddress().length == INET4_ADDRESS_LENGTH;
            } catch (Exception e) {
                return false;
            }
        }

        public static boolean isIPv6Address(String input) {
            try {
                InetAddress inetAddress = InetAddress.getByName(input);
                return inetAddress.getAddress().length == INET6_ADDRESS_LENGTH;
            } catch (Exception e) {
                return false;
            }
        }
    }

    static long getInt(String ipaddr) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ipaddr);
            byte[] addressBytes = inetAddress.getAddress();
            long ip = 0;

            if (InetAddressUtils.isIPv4Address(ipaddr)) {
                ip += (addressBytes[0] & 0xFFL) << 24;
                ip += (addressBytes[1] & 0xFFL) << 16;
                ip += (addressBytes[2] & 0xFFL) << 8;
                ip += addressBytes[3] & 0xFFL;
            } else if (InetAddressUtils.isIPv6Address(ipaddr)) {
                for (byte b : addressBytes) {
                    ip = (ip << 8) | (b & 0xFFL);
                }
            }

            return ip;
        } catch (Exception e) {
            return 0;
        }
    }

    public long getInt() {
        return getInt(mIp);
    }
}
