package com.gnss.ntripserial.Utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FileIOUtils {


    /**
     * 写入章节内容
     * @param Dir 根目录
     * @param sub_path 子目录需带有书名"/ZsearchRes/BookReserve/bookname"
     * @param content 内容
     */
    public static void WriteTXT(File Dir,String sub_path,String content){
        File mk_txt=new File(Dir+sub_path+"/content.txt");
        writeToFile(content, mk_txt,false);
    }

    /**
     * 输出到TXT
     * @param content 内容
     * @param mk_txt 文件名
     * @param append 是否追加
     */
    private static void writeToFile(String content, File mk_txt,boolean append) {
        FileOutputStream fot = null;
        try {
            fot = new FileOutputStream(mk_txt,append);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            fot.write(content.getBytes());
            fot.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (NullPointerException e){
            e.printStackTrace();
            Log.e("write file","地址不存在");
        } finally {
            if (fot != null) {
                try {
                    fot.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 将一个列表写入文件
     * @param Dir 根目录
     * @param sub_path 子目录
     * @param list 列表
     */
    public static void WriteList(File Dir, String sub_path, List<String> list,boolean append){
        StringBuilder content=new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            content.append(list.get(i));
            if(i!=list.size()-1)content.append('\n');
        }
        String content_string=content.toString();
        File mk_txt=new File(Dir + sub_path);
        writeToFile(content_string, mk_txt,append);
        Log.d("file io utils","已输出链接到文件");
    }

    /**
     * 错误报告
     * @param Dir 根目录
     * @param e 错误
     * @param from_where 出处
     */
    public static void WriteErrReport(File Dir,Throwable e,String ...from_where){
        File Folder =new File(Dir,"Errors");
        if(!Folder.exists()){
            Folder.mkdir();
        }
        File mk_txt=new File(Dir+"/Errors/"+TimeUtil.getCurrentDateInString()+"_crashLog.txt" );
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('\n');
        stringBuilder.append(TimeUtil.getCurrentTimeInString());
        stringBuilder.append('\n');
        stringBuilder.append("CAUSE=");
        stringBuilder.append(Arrays.toString(from_where));
        stringBuilder.append('\n');
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        Throwable cause = e.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        stringBuilder.append(writer.toString());
        stringBuilder.append("-----------------------------------\n");
        writeToFile(stringBuilder.toString(), mk_txt,true);
    }

    public static List<String> read_list(File file){
        List<String> list = new ArrayList<>();
       if (!file.exists()) {
           Log.e("file io read list","list file_not_found");
           return list;
       }
        FileInputStream fis=null;
        try {
            fis = new FileInputStream(file);
            InputStreamReader i_reader=new InputStreamReader(fis);
            BufferedReader b_reader=new BufferedReader(i_reader);
            String temp="";
            while ((temp=b_reader.readLine())!=null) {
                list.add(temp);
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
            Log.e("file io read list","list file_not_found");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis!=null){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return list;
    }
}
