package org.lkg.util;

import org.springframework.util.ObjectUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * @author likaiguang
 * @date 2023/2/7 9:06 下午
 */
public class IpUtil {

    public static final String UN_KNOW = "unknown";

    private static String ip;

    private static String hostName;

    static {
        IpUtil.ip = getIp();
        IpUtil.hostName = getHostName();
    }

    public static void setHostName(String hostName) {
        IpUtil.hostName = hostName;
    }

    public static void setIp(String cachedIpAddress) {
        IpUtil.ip = cachedIpAddress;
    }

    public static String getIp() {
        if (ObjectUtils.isEmpty(ip)) {
            return ip;
        }
        Enumeration<NetworkInterface> netInterfaces;
        try {
            netInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (final SocketException ex) {
            throw new RuntimeException(ex);
        }
        InetAddress localIpAddress = null;
        while (netInterfaces.hasMoreElements()) {
            NetworkInterface netInterface = netInterfaces.nextElement();
            Enumeration<InetAddress> ipAddresses = netInterface.getInetAddresses();
            while (ipAddresses.hasMoreElements()) {
                InetAddress ipAddress = ipAddresses.nextElement();
                if (isPublicIpAddress(ipAddress)) {
                    String publicIpAddress = ipAddress.getHostAddress();
                    setIp(ipAddress.getHostAddress());
                    return publicIpAddress;
                }
                if (isLocalIpAddress(ipAddress)) {
                    localIpAddress = ipAddress;
                }
            }

        }
        setIp(localIpAddress.getHostAddress());
        setHostName(localIpAddress.getHostName());
        return localIpAddress.getHostAddress();
    }


    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException ex) {
            return UN_KNOW;
        }
    }
    /**
     * 公网ip地址
     *
     * @param ipAddress
     * @return
     */
    private static boolean isPublicIpAddress(final InetAddress ipAddress) {
        return !ipAddress.isSiteLocalAddress() && !ipAddress.isLoopbackAddress() && !isIpV6Address(ipAddress);
    }

    /**
     * 本机地址
     *
     * @param ipAddress
     * @return
     */
    private static boolean isLocalIpAddress(final InetAddress ipAddress) {
        return ipAddress.isSiteLocalAddress() && !ipAddress.isLoopbackAddress() && !isIpV6Address(ipAddress);
    }

    /**
     * IPV6地址
     *
     * @param ipAddress
     * @return
     */
    private static boolean isIpV6Address(final InetAddress ipAddress) {
        return ipAddress.getHostAddress().contains(":");
    }
}
