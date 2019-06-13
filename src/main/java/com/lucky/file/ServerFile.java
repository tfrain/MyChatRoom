package com.lucky.file;

import com.lucky.thread.ServerSocketThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.Socket;

public class ServerFile {

    private DataInputStream dis;
    private DataOutputStream dos;

    private String fileName;
    private long fileLength;
    private StringBuilder fileContent;

    private Socket socket;
    public ServerFile(Socket socket, boolean alive) {
        this.socket = socket;
        fileContent = new StringBuilder();
    }

    public String getServerFile(ServerSocketThread sst) {
        System.out.println("服务端开始接收文件字节流,业务聊天流应当关闭");
        try {
            if (sst.getAlive() && sst.isSendFile()) {
                //此处可疑
                dis = new DataInputStream(socket.getInputStream());

                //文件名和长度
                fileName = dis.readUTF();
                fileLength = dis.readLong();

                // 开始接收文件
                byte[] bytes = new byte[1024];
                int length;
                while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
                    fileContent.append(new String(bytes,0,length));
                    //System.out.println(fileContent);
                    break;
                }
                System.out.println("服务端收到了文件字节流");
                //确保接收文件后他可以正常继续流转信息
                sst.setSendFile(false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } //finally {
            // try {
            //     // if (fos != null)
            //     //     fos.close();
            //     if (dis != null)
            //         dis.close();
            // } catch (Exception e) {
            // }
        //}
        return fileName;
    }

    public void sendServerFile(ServerSocketThread otherSocket) throws Exception{//这里需要抛出异常，这应当是

        this.socket = otherSocket.getSocket();
        otherSocket.setSendFile(true);

        if(otherSocket.getAlive() && otherSocket.isSendFile()) {//因为只一次，所以 alive 的意义不大,不用在while语句中添加
            try {//都放到try catch块里
                System.out.println("开始向异服务端发文件");
                dos = new DataOutputStream(socket.getOutputStream());

                //文件名、长度
                dos.writeUTF(fileName);
                dos.flush();
                dos.writeLong(fileLength);
                dos.flush();
                //再进行切割感觉意义不大，就没有实现
                byte[] bytes;
                bytes = fileContent.toString().getBytes();

                //System.out.println(new String(bytes));
                dos.write(bytes);
                dos.flush();
                System.out.println("异服务端发文件成功！");
                fileContent.delete(0, fileContent.length());
                otherSocket.setSendFile(false);
            } catch (Exception e) {
                e.printStackTrace();//考虑使用报错良好的报错功能
            } //finally {
                // if(fis != null)
                //     fis.close();
            //     if(dos != null)
            //        dos.close();
            // }
        }
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                dis.close();
                dos.close();
                //socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
