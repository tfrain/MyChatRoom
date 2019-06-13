package com.lucky.file;

import com.lucky.Client;

import org.apache.log4j.Logger;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.math.RoundingMode;
import java.text.DecimalFormat;


public class ClientFile {


    private Client client;
    private Logger logger = Logger.getLogger(ClientFile.class);
    private static DecimalFormat df = null;


    private FileInputStream fis;
    private FileOutputStream fos;
    private DataInputStream dis;
    private DataOutputStream dos;
    private  boolean alive;


    static {
        // 设置数字格式，保留一位有效小数
        df = new DecimalFormat("#0.0");
        //df.setRoundingMode(RoundingMode.HALF_UP);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(1);
    }

    public ClientFile(Client client) {
        this.client = client;
        //this.socket = client.getSocket();
        alive = client.isAlive();
    }

    public void SendClientFile(String sourcePath, Client client) throws Exception {

        if(alive && client.isSendFile()) {
            try {//都放到try catch块里
                File file = new File(sourcePath);
                if(file.exists()) {
                    fis = new FileInputStream(file);
                    dos = new DataOutputStream(client.getOs());//使用同一个输入流

                    //文件名、长度
                    dos.writeUTF(file.getName());
                    dos.flush();
                    dos.writeLong(file.length());
                    dos.flush();

                    logger.info("======== 开始传输文件 ========");
                    //可以设置byte后面的整数，方便传输大文件，默认为4吧
                    byte[] bytes = new byte[1024];
                    int length;
                    long progress = 0;
                    while((length  = fis.read(bytes, 0, bytes.length)) != -1) {
                        dos.write(bytes, 0, length);
                        dos.flush();
                        System.out.println(new String(bytes));
                        progress += length;
                        logger.info("| " + (100*progress/file.length()) + "% |");
                        break;
                    }
                    logger.info("======== 文件传输成功 ========");
                } else {
                    logger.info("invalid sourcePath!");
                }

            } catch (Exception e) {
                e.printStackTrace();
            } //finally {//发送后立即关闭
            //     if(fis != null)
            //         fis.close();
            //     if(dos != null)
            //         dos.close();
            // }
        }
    }

    public void getClientFile() {

        logger.info("============================");
        logger.info("======== 准备接收文件 ========");
            try {
                dis = new DataInputStream(client.getIs());//都不是本套接字

                String fileName = dis.readUTF();
                long fileLength = dis.readLong();

                File directory = new File("/home/wei/file");
                if (!directory.exists()) {
                    directory.mkdir();
                }


                File file = new File(directory.getAbsolutePath() + "/" + fileName);
                fos = new FileOutputStream(file);

                // 开始接收文件
                byte[] bytes = new byte[1024];
                int length;
                while (client.isSendFile() && ((length = dis.read(bytes, 0, bytes.length)) != -1)) {
                    fos.write(bytes, 0, length);
                    fos.flush();
                    System.out.println(new String(bytes));
                    break;
                }
                logger.info("======== 文件接收成功 [File Name：" + fileName + "] [Size：" + getFormatFileSize(fileLength) + "] ========");
                client.setSendFile(false);//确保接收文件的客户端接收完毕后可以正常聊天
            } catch (Exception e) {
                e.printStackTrace();
             } //finally {
            //     try {
            //         if (fos != null)
            //             fos.close();
            //         if (dis != null)
            //             dis.close();
            //     } catch (Exception e) {
            //     }
            // }
        }

    /**
     * 格式化文件大小
     * @param length
     * @return
     */
    private String getFormatFileSize(long length) {
        double size = ((double) length) / (1 << 30);
        if(size >= 1) {
            return df.format(size) + "GB";
        }
        size = ((double) length) / (1 << 20);
        if(size >= 1) {
            return df.format(size) + "MB";
        }
        size = ((double) length) / (1 << 10);
        if(size >= 1) {
            return df.format(size) + "KB";
        }
        return length + "B";
    }



}
