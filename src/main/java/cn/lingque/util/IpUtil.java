package cn.lingque.util;

import java.net.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;

/**
 * IP地址工具类
 * 提供获取本机局域网IP和公网IP的功能
 * 
 * 主要功能：
 * 1. 获取本机局域网IPv4地址
 * 2. 获取本机公网IP地址
 * 
 * @author zlm
 * @version 1.0
 * @since 2024-03-xx
 */
public class IpUtil {
    
    /**
     * 获取本机局域网IP地址
     * 通过遍历网络接口来获取第一个有效的IPv4地址
     * 
     * 实现步骤：
     * 1. 获取所有网络接口
     * 2. 过滤掉回环接口(127.0.0.1)和非活动接口
     * 3. 获取接口的所有IP地址
     * 4. 返回第一个IPv4地址
     * 
     * 异常处理：
     * - 如果发生SocketException，将打印异常堆栈并返回本地回环地址
     * 
     * @return String 返回值说明：
     *         - 成功：返回本机局域网IPv4地址，格式如"192.168.1.100"
     *         - 失败：返回本地回环地址"127.0.0.1"
     */
    public static String getLocalIp() {
        try {
            // 获取所有网络接口
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                // 过滤回环接口(127.0.0.1)和非活动接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }

                // 遍历网络接口的所有IP地址
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // 只返回IPv4地址
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    /**
     * 获取本机公网IP地址
     * 通过访问第三方API服务获取公网IP
     * 
     * 实现步骤：
     * 1. 首先尝试访问ipify.org的API
     * 2. 如果ipify.org失败，则尝试访问ip.sb的API作为备选
     * 3. 设置5秒超时时间避免长时间等待
     * 
     * 注意事项：
     * 1. 需要网络连接
     * 2. 依赖第三方API服务的可用性
     * 3. 可能受网络状况影响响应时间
     * 4. 建议在使用时增加重试机制和缓存机制
     * 
     * 异常处理：
     * - 主API失败时会自动切换到备用API
     * - 所有API都失败时返回空字符串
     * - 异常会被捕获并打印堆栈信息
     * 
     * @return String 返回值说明：
     *         - 成功：返回公网IP地址字符串，格式如"8.8.8.8"
     *         - 失败：返回空字符串""
     */
    public static String getPublicIp() {
        String publicIp = "";
        try {
            // 首选API：ipify.org
            URL url = new URL("https://api.ipify.org");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);  // 连接超时5秒
            conn.setReadTimeout(5000);     // 读取超时5秒
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            publicIp = reader.readLine();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            // 备选API：ip.sb
            try {
                URL url = new URL("https://api.ip.sb/ip");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                publicIp = reader.readLine();
                reader.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return publicIp;
    }
}
