package com.lucky.file;

import com.lucky.thread.ServerSocketThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;

public class ServerFile {

    private FileOutputStream fos;
    private DataInputStream dis;
    private FileInputStream fis;
    private DataOutputStream dos;

    private boolean alive;

    private String fileName;
    private long fileLength;
    private String absolutePath;
    private File file;

    private Socket socket;
    public ServerFile(Socket socket, boolean alive) {
        this.socket = socket;
        this.alive = alive;
    }

    public String getServerFile() {
        try {

            dis = new DataInputStream(socket.getInputStream());//本套接字

            // 文件名和长度
            fileName = dis.readUTF();
            fileLength = dis.readLong();

            File directory = new File("/home/wei/serverFile");
            absolutePath = directory + fileName;

            if (!directory.exists()) {
                directory.mkdir();
            }

            file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
            fos = new FileOutputStream(file);

            // 开始接收文件
            byte[] bytes = new byte[1024];
            int length = 0;
            while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
                fos.write(bytes, 0, length);
                fos.flush();

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
            }
        }
        return absolutePath;
    }

    public void sendServerFile(ServerSocketThread otherSocket, String pathName) throws Exception{//这里需要抛出异常，这应当是

        this.socket = otherSocket.getSocket();

        if(alive) {//因为只一次，所以 alive 的意义不大,不用在while语句中添加
            try {//都放到try catch块里
                File file = new File(pathName);
                if(file.exists()) {
                    fis = new FileInputStream(file);
                    dos = new DataOutputStream(socket.getOutputStream());

                    //文件名、长度
                    dos.writeUTF(file.getName());
                    dos.flush();
                    dos.writeLong(file.length());
                    dos.flush();
                    //再进行切割感觉意义不大，就没有实现

                    //可以提示哪个用户传文件，哪个用户获取文件
                    byte[] bytes = new byte[1024];
                    int length;
                    while((length  = fis.read(bytes, 0, bytes.length)) != -1) {
                        dos.write(bytes, 0, length);
                        dos.flush();

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();//考虑使用报错良好的报错功能
            } finally {
                if(fis != null)
                    fis.close();
                if(dos != null)
                    dos.close();
            }
        }
    }

    public String getFileName() {
        return file.getName();
    }

    public long getFileLength() {
        return fileLength;
    }
}
