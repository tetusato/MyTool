package com.ibm.jp.isol.tetusato.socket;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpReceiver {
    private static final String CONTENT_LENGTH = "Content-Length:";

    public static void main(String... args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(20080)) {
            try (Socket socket = serverSocket.accept()) {
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                // read(in);
                transfer(in, out);
                // write(out);
            }
        }

    }

    private static void transfer(InputStream in, OutputStream out) throws IOException {
        Socket socket = new Socket("localhost", 9080);
        OutputStream os = socket.getOutputStream();
        InputStream ret = socket.getInputStream();
        StringBuilder buff = read(in);
        String read = buff.toString();
        String send = read.replace(":20080", ":9080");
        System.out.println("### SEND TOP    ###");
        System.out.println(send);
        System.out.println("### SEND BOTTOM ###");
        os.write(send.getBytes());
        StringBuilder retBuff = read(ret);
        os.write(retBuff.toString().getBytes());
        os.flush();
        socket.close();
    }

    private static void write(OutputStream os) throws IOException {
        System.out.println("### START WRITE ###");
        try (OutputStreamWriter osw = new OutputStreamWriter(os); BufferedWriter writer = new BufferedWriter(osw)) {
            writer.write("HTTP/1.1 200 OK\r\n");
            writer.write("Content-Type: text/plain\r\n");
            writer.write("HELLO!\r\n");
        }
        System.out.println("### E N D WRITE ###");
    }

    private static final String CRLF = "\r\n";

    private static StringBuilder read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();

        System.out.println("### START READ ###");
        BufferedInputStream bis = new BufferedInputStream(in);
        BufferedReader br = new BufferedReader(new InputStreamReader(bis));
        String line;
        int contentLength = 0;
        while ((line = br.readLine()) != null && !line.isEmpty()) {
            System.out.println(line);
            sb.append(line).append(CRLF);
            if (line.contains(CONTENT_LENGTH)) {
                contentLength = Integer.valueOf(line.substring(CONTENT_LENGTH.length() + 1).trim());
            }
        }
        System.out.println("...HEADER END...");
        sb.append(CRLF);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len;
        byte[] buff = new byte[2048];
        System.out.println("contentLength=" + contentLength);
        if (contentLength != 0) {
            while ((len = bis.read(buff)) >= 0) {
                System.out.println("len=" + len);
                baos.write(buff, 0, len);
                contentLength -= len;
                System.out.println("contentLength=" + contentLength);
                if (contentLength < 1) {
                    break;
                }
            }
        }
        sb.append(new String(baos.toByteArray()));
        System.out.println(sb.toString());
        System.out.println("### E N D READ ###");
        return sb;
    }
}
