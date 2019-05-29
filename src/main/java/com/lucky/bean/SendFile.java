package com.lucky.bean;

import com.lucky.Client;

import org.apache.log4j.Logger;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;

/**
 * 发文件
 */
public class SendFile {

    private Logger logger = Logger.getLogger(SendFile.class);
    private FileInputStream fis;
    private DataOutputStream dos;//对我而言，好处是传文件名
    private Client client;
    private Socket socket;

    public SendFile(Client client, String sourcePath) throws Exception {

        this.client = client;
        this.socket = client.getSocket();

        try {//都放到try catch块里
                File file = new File(sourcePath);
                if(file.exists()) {
                fis = new FileInputStream(file);
                dos = new DataOutputStream(socket.getOutputStream());

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
