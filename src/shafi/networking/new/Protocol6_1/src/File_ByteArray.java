/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;


public class File_ByteArray {

    byte[] bytes;

    public byte[] toByteArray(String s) {

        File f = new File(s);
        FileInputStream fin = null;
        FileChannel ch = null;
        try {
            fin = new FileInputStream(f);
            ch = fin.getChannel();
            int size = (int) ch.size();
            MappedByteBuffer buf = ch.map(MapMode.READ_ONLY, 0, size);
            bytes = new byte[size];
            buf.get(bytes);


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
                if (ch != null) {
                    ch.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return bytes;
    }

    public void byteArrayToFile(byte[] bytes, String s, int size, int fpos, int lpos) {
        try {
            File file = new File(s);
            byte[] toWrite = null;
            if (file.exists()) {

                File_ByteArray FBA3 = new File_ByteArray();
                toWrite = FBA3.toByteArray(s);
            } else {


                toWrite = new byte[size];
                for (int i = fpos, j = 0; i <= lpos; i++, j++) {
                    toWrite[i] = bytes[j];
                    System.out.print(toWrite[i]);
                    FileOutputStream fos = new FileOutputStream(s);
                    fos.write(toWrite);
                    fos.close();
                }



            }
            FileOutputStream fos = new FileOutputStream(s);

            fos.write(toWrite);
            fos.close();
        } catch (FileNotFoundException ex) {
            System.out.println("FileNotFoundException : " + ex);
        } catch (IOException ioe) {
            System.out.println("IOException : " + ioe);
        }

    }
}
