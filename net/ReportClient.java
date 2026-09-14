package com.bookms.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * TCP 客户端：把 List<String> 借阅统计通过 Socket 发到 ReportServer。
 *
 * <p>GUI 借还面板的「上报统计」按钮调用此工具。</p>
 */
public class ReportClient {

    public static final int DEFAULT_PORT = 9999;

    /** 发到服务端，返回服务端 ACK。host 默认 127.0.0.1。 */
    public static String sendReport(List<String> lines, String host, int port, int timeoutMs) throws IOException {
        if (lines == null || lines.isEmpty()) {
            return "[ReportClient] 报表内容为空，未发送";
        }
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);
        socket.setSoTimeout(timeoutMs);

        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader ack = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"))) {

            // 报头：客户端 + 时间戳（便于服务端日志分类）
            out.println("### BookMS 借阅统计报表");
            out.println("### time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            out.println("### host: " + host + ":" + port);
            for (String l : lines) {
                out.println(l);
            }
            out.println("EOF");
            out.flush();

            return ack.readLine();
        } finally {
            socket.close();
        }
    }

    public static void main(String[] args) throws IOException {
        // 自检：本地发一条测试报表
        java.util.List<String> sample = new java.util.ArrayList<>();
        sample.add("借阅总数: 10");
        sample.add("在借: 7");
        sample.add("已还: 3");
        String ack = sendReport(sample, "127.0.0.1", ReportServer.DEFAULT_PORT, 3000);
        System.out.println("[ReportClient] 服务端 ACK → " + ack);
    }
}