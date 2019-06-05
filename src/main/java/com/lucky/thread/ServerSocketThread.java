package com.lucky.thread;

import com.google.gson.Gson;
import com.lucky.Server;
import com.lucky.bean.ChatMsg;
import com.lucky.constant.MsgType;
import com.lucky.constant.ResponseStatus;
import com.lucky.file.ServerFile;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

public class ServerSocketThread extends Thread {

    private Logger logger = Logger.getLogger(ServerSocketThread.class);

    private Socket socket;
    private Server server;
    private BufferedInputStream bis;
    private BufferedOutputStream bos;
    private boolean sendFile;
   // private DataInputStream dis;
    //private DataOutputStream dos;


    private boolean alive;
    private byte[] buffer = new byte[1024];
    private String message;
    private String chatRoom = "";
    private Gson gson = new Gson();
    private ServerFile serverFile;
    //private String fileName;

    public ServerSocketThread(Socket socket, Server server, ServerFile serverFile) {
        this.socket = socket;
        this.server = server;
        this.serverFile = serverFile;
        sendFile = false;

        try {
            // todo 采用确认的方式，设置并传递变量，等客户端接受服务端同意的消息后，服务端使用变量开启数据流，不使用缓冲流，
            // todo 并不经过麻烦的判断，直接在服务端用循环一步步传给客户端大容量文件，客户端则立刻进行写入文件操作，
            // todo 只有变量确认进行文件传输，才使用判断内容，和前面的方式衔接在一起，函数名应该叫getsend方法，并发送文件成功的提示给客户端
            // todo while 语句下面给输出流传递接受完毕的指令，都要在接受完将变量设置为false，这样才能继续正常的聊天，都要关闭相应的数据流
            // todo 如果想通用一套代码，如何搞定文件名，如何利用缓冲流，得到大文件，有相应的函数实现吗?
            // dis = new DataInputStream(socket.getInputStream());//本套接字
            // fileName = dis.readUTF();
            // if(fileName == null) {
            //
            // }
            bis = new BufferedInputStream(socket.getInputStream());//缓冲流
            bos = new BufferedOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        alive = true;
    }

    @Override
    public void run() {
        while (alive && !sendFile) {  // 接收客户端socket发送的消息
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

    public void sendFileMsg(String msg) {
        sendMsgWithType(MsgType.SEND_FILE, ResponseStatus.OK, msg);
    }

    public Socket getSocket() {
        return socket;
    }
    public boolean getAlive() {
        return alive;
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
                    //服务器端创建中转站,获取中转站文件全路径,已经在中转站创建文件
                    if(sendFile) {
                        String Ok = "The server is ready";
                        sendMsgWithType(MsgType.SEND_FILE, ResponseStatus.OK, Ok);
                        sendFile = true;
                        server.deliverfile(chatRoom, socket,serverFile.getServerFile(sendFile));//为了一致，就这样调用函数了
                    }
                    // bis.close();todo 包装对象没有流，我认为没必要关闭
                    // bos.close();
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
                bos.close();//为什么缓冲流关了，而简单输入输出没关,因为简单输入输出一直要进行
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
