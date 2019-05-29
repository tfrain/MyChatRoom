package com.lucky.thread;

import com.google.gson.Gson;
import com.lucky.Server;
import com.lucky.bean.ChatMsg;
import com.lucky.bean.GetFile;
import com.lucky.constant.MsgType;
import com.lucky.constant.ResponseStatus;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public class ServerSocketThread extends Thread {

    private Logger logger = Logger.getLogger(ServerSocketThread.class);

    private Socket socket;
    private Server server;
    private BufferedInputStream bis;
    private BufferedOutputStream bos;
    private boolean alive;
    private byte[] buffer = new byte[1024];
    private String message;
    private String chatRoom = "";
    private Gson gson = new Gson();

    public ServerSocketThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            bis = new BufferedInputStream(socket.getInputStream());//缓冲流
            bos = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        alive = true;
    }

    @Override
    public void run() {
        while (alive) {  // 接收客户端socket发送的消息
            try {
                int len = bis.read(buffer);  // 假设数据都是一次读完，不存在组包
                if (len == -1) {  // 客户端socket已经关闭,服务端也相应关闭
                    close();
                    break;
                }

                message = new String(buffer, 0, len);
                //logger.info(this.getId() + ": " + message);
                handlerMsg(message);
            } catch (IOException e) {
                alive = false;
                e.printStackTrace();
            }
        }

        logger.info("user-" + this.getId() + " closed， thread quit");
        server.socketDisconnect(this);
    }

    public String getChatRoom() {
        return chatRoom;
    }

    public void sendChatMsg(String msg) {
        sendMsgWithType(MsgType.CHAT, ResponseStatus.OK, msg);
    }
    public Socket getSocket() {
        return socket;
    }

    /**
     * 处理从客户端发来的请求
     */
    private void handlerMsg(String msg) {
        if (msg == null || "".equals(msg)) {
            return;
        }

        char type = msg.charAt(0);
        String roomName;
        switch (type) {
            case MsgType.LIST_ROOM:
                String data = server.getRoomList();
                sendMsgWithType(type, ResponseStatus.OK, data);
                break;
            case MsgType.JOIN_ROOM:
                roomName = msg.substring(1);
                try {
                    server.addSocketToRoom(this, roomName);
                    sendMsgWithType(type, ResponseStatus.OK, roomName);
                    chatRoom = roomName;
                } catch (Exception e) {
                    sendMsgWithType(type, ResponseStatus.FAIL, e.getMessage());
                }
                break;
            case MsgType.QUIT_ROOM:
                try {
                    server.removeSocketFromRoom(this, chatRoom);
                    sendMsgWithType(type, ResponseStatus.OK, null);
                    chatRoom = "";
                } catch (Exception e) {
                    sendMsgWithType(type, ResponseStatus.FAIL, e.getMessage());
                }
                break;
            case MsgType.CREATE_ROOM:
                roomName = msg.substring(1);
                try {
                    server.createChatRoom(this, roomName);
                    sendMsgWithType(type, ResponseStatus.OK, roomName);
                    chatRoom = roomName;
                } catch (Exception e) {
                    sendMsgWithType(type, ResponseStatus.FAIL, e.getMessage());
                }
                break;
            case MsgType.CHAT:
                ChatMsg chatMsg = new ChatMsg("user-" + getId(), msg.substring(1), new Date());
                try {
                    server.deliverChatMsg(chatRoom, gson.toJson(chatMsg));//房间号绑定了，才可能继续聊天，调用server让socket一个一个来
                } catch (Exception e) {
                    sendMsgWithType(type, ResponseStatus.FAIL, e.getMessage());
                }
                break;
            case MsgType.SEND_FILE:
                try {
                    server.deliverfile(chatRoom, socket);//为了一致，就这样调用函数了
                } catch (Exception e) {
                    sendMsgWithType(type, ResponseStatus.FAIL, e.getMessage());
                }
                break;
            default:
                logger.info("invalid message type");
        }
    }
    //不聊天就只是普通的多线程服务端和客户端输入输出流进行交流，不需要分发消息
    private void sendMsgWithType(char type, char status, String data) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(type).append(status).append(data);
            bos.write(sb.toString().getBytes());
            bos.flush();     // 使用bufferOutputStream要记得flush,不然就没用到缓冲的作用
        } catch (IOException e) {
            close();
            e.printStackTrace();
        }
    }

    private void close() {
        try {
            alive = false;
            if (socket != null && !socket.isClosed()) {
                bis.close();
                bos.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
