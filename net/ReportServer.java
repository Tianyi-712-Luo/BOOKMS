package com.bookms.net;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TCP 服务端：监听 9999 端口，接收 BookMS 客户端发来的「借阅统计」文本，
 * 追加写入 report.log（带时间戳前缀）。
 *
 * <p>独立线程处理连接，accept() 阻塞等待。</p>
 */
public class ReportServer {

    public static final int DEFAULT_PORT = 9999;
    public static final String LOG_FILE   = "report.log";

    public static void start(int port) throws IOException {
        ServerSocket server = new ServerSocket(port);
        System.out.println("[ReportServer] 监听端口 " + port + " ...（按 Ctrl+C 退出）");
        while (true) {
            Socket client = server.accept();
            // 独立线程处理每个客户端
            new Thread(() -> handleClient(client), "client-" + client.getPort()).start();
        }
    }

    private static void handleClient(Socket client) {
        String remote = client.getRemoteSocketAddress().toString();
        System.out.println("[ReportServer] 客户端连接 → " + remote);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
             PrintWriter ack   = new PrintWriter(client.getOutputStream(), true)) {

            String line;
            StringBuilder report = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if ("EOF".equals(line)) break;
                report.append(line).append('\n');
            }

            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String entry = "===== " + ts + " | 来自 " + remote + " =====\n"
                         + report.toString()
                         + "\n";
            try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
                fw.write(entry);
            }
            System.out.println("[ReportServer] 已记录:\n" + entry);

            ack.println("ACK: server received " + report.toString().length() + " chars at " + ts);
        } catch (IOException e) {
            System.err.println("[ReportServer] 处理失败: " + e.getMessage());
        } finally {
            try { client.close(); } catch (IOException ignored) {}
        }
    }

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        start(port);
    }
}