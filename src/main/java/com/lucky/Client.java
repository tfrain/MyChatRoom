package com.lucky;

import com.lucky.constant.MsgType;
import com.lucky.file.ClientFile;
import com.lucky.thread.ClientSocketThread;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

public class Client {

    private static final Logger logger = Logger.getLogger(Client.class);

    private String host;
    private int port;
    private int retry;         // 最大重连次数
    private Socket socket;
    private InputStream is;
    private OutputStream os;


    private ClientFile clientFile;

    private boolean alive;
    private boolean chatting;  // 是否处于聊天模式，聊天模式中不发送文件
    private String chatRoom;   // 存储当前属于的chat room

    private boolean sendFile;  // 发送文件后客户端的输入流方法，变成数据流

    /** Public Methods */

    public Client() {
        initFromProperties();
        addShutdownHook();
    }

    public void connect() {
        try {
            socket = new Socket(host, port);
            is = socket.getInputStream();
            os = socket.getOutputStream();
            //是为了客户端线程而存在，如果socket关闭。alive为false，就可以停止客户端线程的一切活动了
            //同时也为了重连而存在，不然直接判断socket是否存在就可以了
            alive = true;
            chatting = false;
            sendFile = false;
            chatRoom = "";
            Thread socketThread = new ClientSocketThread(this);
            clientFile = new ClientFile(this);
            socketThread.start();//阻塞语句
            logger.info("Connect to the chat room successfully");
            printInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收用户终端输入
     */
    public void startGetUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (alive) {
            String msg = scanner.nextLine();
            if (alive) { // 双重校验，可能在阻塞时候，服务器socket断开连接了
                handlerMsg(msg);
            } else {
                reconnect();
            }
        }

        logger.info("stop get user input");
    }

    public Socket getSocket() {
        return socket;
    }

    public InputStream getIs() {
        return is;
    }

    public OutputStream getOs() {
        return os;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isChatting() {
        return chatting;
    }

    public void setChatting(boolean chatting) {
        this.chatting = chatting;
    }

    public String getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(String chatRoom) {
        this.chatRoom = chatRoom;
    }
    public ClientFile getClientFile() {
        return clientFile;
    }

    public boolean isSendFile() {
        return sendFile;
    }

    public void setSendFile(boolean sendFile) {
        this.sendFile = sendFile;
    }

    /** Private Methods */

    /**
     * 根据用户的不同输入选择不同操作
     */
    private void handlerMsg(String msg) {
        if (msg == null || "".equals(msg)) {
            return;
        }

        if (chatting) { // 处于聊天模式,直接将用户输入,否则调用类型信息方法
            handlerMsgInChattingMode(msg);
            return;
        }

        if (msg.length() < 2 || msg.length() > 1000 || msg.charAt(0) != '#') {
            logger.info("invalid option!");
            printInfo();
            return;
        }


        char type = msg.charAt(1);
        handlerMsgWithType(type, msg.substring(1));
    }

    /**
     * 根据不同的消息类型，向socket写入不同的信息
     */
    private void handlerMsgWithType(char type, String msg) {
        if (type == MsgType.LIST_ROOM ) {
            sendMsg(type);
            return;
        }

        if (type == MsgType.QUIT_ROOM) {
            if ("".equals(chatRoom)) {
                logger.info("You are not in a chat room");
                return;
            }

            sendMsg(type);
            return;
        }

        if (type == MsgType.JOIN_ROOM ) {
            if (!"".equals(chatRoom)) {
                logger.info("You have already joined a chat room");
                return;
            }

            sendMsg(msg.replaceAll(" +", ""));  //去掉空格
            return;
        }

        if (type == MsgType.CREATE_ROOM) {
            if (!"".equals(chatRoom)) {
                logger.info("You have already joined a chat room");
                return;
            }

            sendMsg(msg.replaceAll(" +", ""));  //去掉空格
            return;
        }

        if (type == MsgType.CHAT) {
            if ("".equals(chatRoom)) {
                logger.info("Please join a chat room first");
                return;
            }

            chatting = true;//聊天模式开启
            logger.info("You are in chatting mode, please input '#exit' to quit chatting mode");
            return;
        }

        if (type == MsgType.SEND_FILE) {
            //以空格为划分线
            String[] splited = msg.split("\\s+");
            if ("".equals(chatRoom)) {
                logger.info("Please join a chat room first");
                return;
            }
            try {
                if(sendFile) {
                    clientFile.SendClientFile(splited[1], sendFile);
                    sendFile = false;
                }
                sendMsg(type);//确认是7
                //clientFile.SendClientFile(splited[1]);//单开文件流
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;//不至于输出流冲突
        }

        if (type == MsgType.QUIT_SYSTEM) {
            close();
            return;
        }

        logger.info("invalid type of message");
        printInfo();
    }

    /**
     * 处理聊天模式下收到的信息,并交由服务端进行处理，服务端通过类型来处理传递过来的内容
     */
    private void handlerMsgInChattingMode(String msg) {
        if (msg == null || "".equals(msg)) {
            return;
        }

        if("#exit".equals(msg)) {
            chatting = false;
            logger.info("quit chatting mode");
            printInfo();
            return;
        }

        String protocolMsg = MsgType.CHAT + msg;
        sendMsg(protocolMsg);
    }

    private void sendMsg(String msg) {
        try {
            os.write(msg.getBytes());
        } catch (IOException e) {
            alive = false;
            reconnect();
            e.printStackTrace();
        }
    }

    private void sendMsg(char msg) {
        try {
            os.write(msg);//todo 这里面为什么不要关闭输出流
        } catch (IOException e) {
            alive = false;
            reconnect();
            e.printStackTrace();
        }
    }

    /**
     * socket断开后，隔一段时间尝试重新连接
     */
    private void reconnect() {
        int retry = 0;
        while (retry < 5) {
            logger.info("reconnect..." + (++retry));
            connect();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (alive) {
                logger.info("reconnect success");
                return;
            }
        }

        logger.info("reconnect failed, close client");
        close();
    }

    private void close() {
        try {
            alive = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printInfo() {
        System.out.println("------------------------------");
        System.out.println("1查看聊天室状态: input '#1' to see current chat rooms");
        System.out.println("2加入聊天室: input '#2 {roomName}' to join a chat room");
        System.out.println("3退出聊天室: input '#3' to quit chat room");
        System.out.println("4创建房间: input '#4 {roomName}' to create a chat room");
        System.out.println("5退出聊天: input '#5' to start chatting");
        System.out.println("6关闭本客户端: input '#6' to quit system");
        System.out.println("7文件传输: input '#7 {sourcePath}' to send a file");
        System.out.println("------------------------------");
    }

    private void initFromProperties() {
        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getResourceAsStream("/client.properties"));
            this.port = Integer.valueOf(properties.getProperty("port"));
            this.host = properties.getProperty("host");
            this.retry = Integer.valueOf(properties.getProperty("retry"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                logger.info("Shutdown hook");
                close();
            }
        });
    }
}
