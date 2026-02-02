package com.test.Database_Testing;

import okhttp3.*;
import okio.ByteString;

public class AI_agent_Websocket {

    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("wss://llm.humanbrain.in:1062/ws/ai_agent")
                .build();

        WebSocketListener listener = new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("✅ Connected to server");

                // Wait 1 second before sending
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // Let server settle
                        webSocket.send("hello");
                        System.out.println("📤 Sent: hello");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                System.out.println("📨 Server says: " + text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                System.out.println("📦 Binary Message: " + bytes.hex());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                System.out.println("🔁 Closing: " + code + " / " + reason);
                webSocket.close(1000, null);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                System.err.println("❌ Error: " + t.getMessage());
            }
        };

        client.newWebSocket(request, listener);

        // Keep alive long enough to receive server response
        try {
            Thread.sleep(60000); // 1 minute
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        client.dispatcher().executorService().shutdown();
    }
}
