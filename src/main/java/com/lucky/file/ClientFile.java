package com.lucky.file;

import com.lucky.Client;

import org.apache.log4j.Logger;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
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
    private DataOutputStream dos;//对我而言，好处是传文件名
    private Socket socket;
    private  boolean alive;


    static {
        // 设置数字格式，保留一位有效小数
        df = new DecimalFormat("#0.0");
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(1);
    }

    public ClientFile(Client client) {
        this.client = client;
        this.socket = client.getSocket();
        alive = client.isAlive();
    }

    public void SendClientFile(String sourcePath) throws Exception {

        this.client = client;
        this.socket = client.getSocket();
        alive = client.isAlive();

        if(alive) {//因为只一次，所以 alive 的意义不大,不用在while语句中添加
            try {//都放到try catch块里
                File file = new File(sourcePath);
                if(file.exists()) {
                    fis = new FileInputStream(file);
                    dos = new DataOutputStream(client.getOs());

                    //文件名、长度
                    dos.writeUTF(file.getName());
                    dos.flush();
                    dos.writeLong(file.length());
                    dos.flush();
                    //再进行切割感觉意义不大，就没有实现

                    logger.info("======== 开始传输文件 ========");
                    byte[] bytes = new byte[1024];
                    int length = 0;
                    long progress = 0;
                    while((length  = fis.read(bytes, 0, bytes.length)) != -1) {
                        dos.write(bytes, 0, length);
                        dos.flush();
                        progress += length;
                        logger.info("| " + (100*progress/file.length()) + "% |");
                    }
                    logger.info("======== 文件传输成功 ========");
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

    public boolean getClientFile() {

            try {
                dis = new DataInputStream(socket.getInputStream());//都不是本套接字



                File directory = new File("/home/wei/file");
                if (!directory.exists()) {
                    directory.mkdir();
                }
            /*
            todo 应该要和线程一端进行交互，file 要在客户端创建，这些东西应该通过多线程服务端进行中转，建议每一个服务端都配一个
            TODo getfile实例，然后不断进行交互

             */

                File file = new File(directory.getAbsolutePath() + File.separatorChar );
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
            return true;
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