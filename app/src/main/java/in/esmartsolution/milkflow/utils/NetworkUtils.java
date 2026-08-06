package in.esmartsolution.milkflow.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class NetworkUtils {

    public static final String GROUP_OWNER_IP = "192.168.49.1";
    public static final int CONTROL_PORT = 8890;
    public static final int PORT = 8888;

    public static String getLocalIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                // Wifi Direct interfaces typically start with "p2p" or "wlan"
                if (intf.getName().contains("p2p") || intf.getName().contains("wlan")) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }
}
