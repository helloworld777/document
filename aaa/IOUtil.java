package com.lu.main;



import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.FileChannel;

/**
 * IO辅助类
 */
public class IOUtil {
    /**
     * 写数据
     * @param filePath 文件路径
     * @param data 数据
     * @param append 是否从写在文件后端 true 是 false 覆盖
     * @throws IOException
     */
    public static void writerString(String filePath, String data,boolean append) throws IOException {
        BufferedWriter out = null;
        try {
            File file=new File(filePath);
            if (!file.exists()){
                file.createNewFile();
            }
            out = new BufferedWriter(new FileWriter(filePath, append));
            out.write(data);
            out.newLine();
            out.flush();
        } finally {
            closeStream(out);
        }
    }

    /**
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static InputStream copyInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BufferedInputStream br = new BufferedInputStream(inputStream);
        byte[] b = new byte[1024];
        for (int c = 0; (c = br.read(b)) != -1; ) {
            bos.write(b, 0, c);
        }
        return new ByteArrayInputStream(bos.toByteArray());
    }

    /**
     * 已给定的编码去读流信息
     * @param in
     * @param iencode null或者""时 默认的编码
     * @return
     * @throws IOException
     */
    public static String encodeStream(InputStream in, String iencode) throws IOException {
        InputStreamReader reader;
//        if (TextUtils.isEmpty(iencode)){
            reader=new InputStreamReader(in);
//        }else{
//            reader = new InputStreamReader(in, iencode);
//        }
        BufferedReader tBufferedReader = new BufferedReader(reader);
        StringBuffer tStringBuffer = new StringBuffer();
        String sTempOneLine = new String("");
        while ((sTempOneLine = tBufferedReader.readLine()) != null) {
            tStringBuffer.append(sTempOneLine);

        }
        return tStringBuffer.toString();
    }
    /**
     *关闭流
     */
    public static void closeStream(Object stream){
        if(stream==null){return;}
        try {
            if(stream instanceof Reader){
                ((Reader)stream).close();
            }else if(stream instanceof Writer){
                ((Writer)stream).close();
            }else if(stream instanceof InputStream){
                ((InputStream)stream).close();
            }else if(stream instanceof OutputStream){
                ((OutputStream)stream).close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param filePath 文件路径
     * @return
     * @throws IOException
     */
    public static String readerString(String filePath) throws IOException{
        return encodeStream(new FileInputStream(filePath),null);

    }

    /**
     * 文件拷贝
     * @param in 源文件
     * @param out
     * @throws IOException
     */
    public static void fileCopy(File in, File out) throws IOException {

        FileChannel inChannel = new FileInputStream(in).getChannel();

        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            int maxCount = (64 * 1024 * 1024) - (32 * 1024);
            long size = inChannel.size();
            long position = 0;
            while (position < size) {
                position += inChannel.transferTo(position, maxCount, outChannel);
            }

        } finally {

            if (inChannel != null) {

                inChannel.close();

            }

            if (outChannel != null) {

                outChannel.close();

            }

        }

    }

    /**
     * copy 文件
     *
     * @param scrPath   原文件目录
     * @param soucePath 目标文件目标
     */
    public static boolean copyFile(String scrPath, String soucePath) {
        File f = new File(scrPath);
        File o = new File(soucePath);
        if (o.exists()) {
            o.delete();
        }
        if (f.exists()) {
            FileChannel outF;
            try {
                outF = new FileOutputStream(o).getChannel();
                new FileInputStream(f).getChannel().transferTo(0, f.length(),
                        outF);
                return  true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {

                e.printStackTrace();

            }
        }
        return false;
    }
}