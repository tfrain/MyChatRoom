package com.lucky.bean;

import com.lucky.thread.ServerSocketThread;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.File;

import java.io.FileOutputStream;
import java.math.RoundingMode;
import java.net.Socket;
import java.text.DecimalFormat;

/**
 * 接文件
 */
public class GetFile {

    private Logger logger = Logger.getLogger(SendFile.class);
    private FileOutputStream fos;
    private DataInputStream dis;
    private Socket socket;
    private static DecimalFormat df = null;

    static {
        // 设置数字格式，保留一位有效小数
        df = new DecimalFormat("#0.0");
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(1);
    }

    public GetFile(ServerSocketThread socketThread) throws Exception {
        this.socket = socketThread.getSocket();

        try {
            dis = new DataInputStream(socket.getInputStream());

            // 文件名和长度
            String fileName = dis.readUTF();
            long fileLength = dis.readLong();
            File directory = new File("D:\\FTCache");
            if (!directory.exists()) {
                directory.mkdir();
            }
            /*
            todo 应该要和线程一端进行交互，file 要在客户端创建，这些东西应该通过多线程服务端进行中转，建议每一个服务端都配一个
            TODo getfile实例，然后不断进行交互

             */

            File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
            fos = new FileOutputStream(file);

            // 开始接收文件
            byte[] bytes = new byte[1024];
            int length = 0;
            while ((length = dis.read(bytes, 0, bytes.length)) != -1) {
                fos.write(bytes, 0, length);
                fos.flush();

                logger.info("======== 文件接收成功 [File Name：" + fileName + "] [Size：" + getFormatFileSize(fileLength) + "] ========");

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
                if (dis != null)
                    dis.close();
                socket.close();
            } catch (Exception e) {
            }
        }
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
