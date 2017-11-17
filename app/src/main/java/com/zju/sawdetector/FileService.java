package com.zju.sawdetector;


        import android.content.Context;
        import android.os.Environment;
        import android.util.Log;

        import java.io.BufferedWriter;
        import java.io.ByteArrayOutputStream;
        import java.io.File;
        import java.io.FileInputStream;
        import java.io.FileOutputStream;
        import java.io.IOException;
        import java.io.OutputStreamWriter;
        import java.nio.Buffer;

/**
 * Created by Administrator on 2016/11/30.
 */
public class FileService {

    Context context;

    public FileService(){

    }

    public FileService(Context context){
        this.context = context;
    }

    /**Android自带内存的文件储存
     *
     * @param fileName 文件名称 文件类容
     * @param fileText
     * @throws Exception
     */
    public void writing(String fileName, String fileText) throws Exception{
        //默认保存路径../data/date/package name/file目录下
        //Android还提供了两种方法getCacheDir()和getFilesDir()方法：
        //getCacheDir()方法用于获取/data/data/<package name>/cache目录
        //getFilesDir()方法用于获取/data/data/<package name>/file目录

        //openFileOutput（Sting ，int） 快速获取一个输出流，Sting==>文件名称，int操作模式（可看源码信息）
        //Context.MODE_PRIVATE:为默认操作模式，代表该文件是私有数据，只能被该应用访问，在该模式下，写入的内容会覆盖源文件的内容；
        //Context.MODE_APPEND:为追加操作模式，代表该文件是私有的，只能够被该应用访问，在该模式下，写入的内容追加在源文件内容的后面；
        ////还有一种追加方式为FileoutputStream中的两个参数（String name,boolean append）其中第二个参数决定是否以追加的模式来写入内容;
        //Context.MODE_READABLE:表示当前文件能被其他应用读取;
        // Context.MODE_WRITEABLE:表示当前文件能被其他应用写入;
        FileOutputStream fileOutputStream = context.openFileOutput(fileName,Context.MODE_PRIVATE);
        fileOutputStream.write(fileText.getBytes());
        fileOutputStream.close();


    }

    /**将文件存放在SDCard
     * 需要权限
     * 在SDCard中创建与删除文件权限
     * <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS"/>
     * 往SDCard写入数据权限
     * <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
     * @param fileName 文件名称
     * @param fileText 文件内容
     * @throws Exception
     */

    public void saveToSDCard(String fileName, String fileText) throws Exception{
        //第一个参数方法为获取SDCard目录
        File path = new File ( Environment.getExternalStorageDirectory () + "/Android"+"/data"+"/com.zju.sawdetector"+File.separator);

        if (!path.exists ())
        {
            // Make sure the Pictures directory exists.
            path.mkdirs();
        }

        File file = new File(path,fileName);
        Log.d ( "saveToSDCard", String.valueOf ( file ) );

        if (!file.exists ())
        {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileOutputStream outputStream = new FileOutputStream(file);
        BufferedWriter writer = new BufferedWriter ( new OutputStreamWriter ( outputStream ));
        writer.write (fileText);
        writer.close ();
        outputStream.close();
    }


    public String read(String fileName) throws Exception{
        //默认读取路径../data/date/package name/file目录下
        FileInputStream fileInputStream = context.openFileInput(fileName);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len = 0;
        while ((len = fileInputStream.read(buffer)) != -1){
            outputStream.write(buffer,0,len);
        }

        return new String(outputStream.toByteArray());
    }

}
