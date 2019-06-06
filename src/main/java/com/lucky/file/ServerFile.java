package com.lucky.file;

import com.lucky.thread.ServerSocketThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.Socket;

public class ServerFile {

    private DataInputStream dis;
    private DataOutputStream dos;

    private boolean alive;

    private String fileName;
    private long fileLength;
    private StringBuilder fileContent;
    private boolean sendFile;

    private Socket socket;
    public ServerFile(Socket socket, boolean alive) {
        this.socket = socket;
        this.alive = alive;
        fileContent = new StringBuilder();
    }

    public String getServerFile(boolean sendFile) {
        this.sendFile = sendFile;
        System.out.println("服务端开始接收文件字节流");
        try {
            //此处可疑
            dis = new DataInputStream(socket.getInputStream());//本套接字
            dos = new DataOutputStream(socket.getOutputStream());

            // 文件名和长度
            fileName = dis.readUTF();
            fileLength = dis.readLong();

            // 开始接收文件
            byte[] bytes = new byte[1024];
            int length = 0;
            while (alive && sendFile && (length = dis.read(bytes, 0, bytes.length)) != -1) {
                //fos.write(bytes, 0, length);
                //fos.flush();
                fileContent.append(new String(bytes,0,length));
            }
            System.out.println("服务端收到了文件字节流");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // if (fos != null)
                //     fos.close();
                if (dis != null)
                    dis.close();
            } catch (Exception e) {
            }
        }
        return fileName;
    }

    public void sendServerFile(ServerSocketThread otherSocket) throws Exception{//这里需要抛出异常，这应当是

        this.socket = otherSocket.getSocket();

        if(alive && sendFile) {//因为只一次，所以 alive 的意义不大,不用在while语句中添加
            try {//都放到try catch块里
                System.out.println("开始向异服务端发文件");
                dos = new DataOutputStream(socket.getOutputStream());

                //文件名、长度
                dos.writeUTF(fileName);
                dos.flush();
                dos.writeLong(fileLength);
                dos.flush();
                //再进行切割感觉意义不大，就没有实现
                //todo 可以提示哪个用户传文件，哪个用户获取文件
                //todo 将fileContent内容转换为byte传输过去
                byte[] bytes;
                bytes = fileContent.toString().getBytes();
                dos.write(bytes);//todo 咨询老师，有没有传递大文件的好方法，将Stringbuilder切割成byte,这里sendFile立刻设置为false，会不妥
                dos.flush();
                sendFile = false;
                System.out.println("异服务端发文件成功！");
            } catch (Exception e) {
                e.printStackTrace();//考虑使用报错良好的报错功能
            } finally {
                // if(fis != null)
                //     fis.close();
                if(dos != null)
                    dos.close();
            }
        }
    }
}
