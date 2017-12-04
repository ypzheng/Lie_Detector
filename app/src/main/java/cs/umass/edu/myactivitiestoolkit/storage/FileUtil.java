package cs.umass.edu.myactivitiestoolkit.storage;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import cs.umass.edu.myactivitiestoolkit.constants.Constants;

/**
 * This class handles file input/output operations, such as saving the activity-labeled accelerometer
 * data, opening/closing the file writer and deleting any existing data files.
 * <br><br>
 * Note this class is deprecated. You should instead be sending data to the server as long as you have
 * a network connection.
 *
 * @author aparate
 *
 * @see BufferedWriter
 * @see Constants
 */
@Deprecated
public class FileUtil {

    /** Used during debugging to identify logs by class */
    private static final String TAG = FileUtil.class.getName();

    /** Default name of the application's directory */
    private static final String DEFAULT_DIRECTORY = "motion-data";

    /** CSV extension */
    private static final String CSV_EXTENSION = ".csv";

    /**
     * Returns a root directory where the logging takes place
     * @return file of the root directory
     */
    private static File getStorageLocation(){
        File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DEFAULT_DIRECTORY);
        if(!root.exists())
            if (!root.mkdir()){
                Log.w(TAG, "Failed to create directory! It may already exist");
            }
        return root;
    }

    /**
     * Returns a file writer for a device
     * @param filename file name (without extension!)
     * @return the file writer for the particular .csv file
     */
    public static BufferedWriter getFileWriter(String filename){
        File rootDir = getStorageLocation();
        String fullFileName = filename+CSV_EXTENSION;

        BufferedWriter out = null;
        try{
            out = new BufferedWriter(new FileWriter(new File(rootDir,fullFileName)));
        }catch(IOException e){
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Write the log to the specified file writer
     * @param s log to write
     * @param out file writer
     */
    public static void writeToFile(String s, final BufferedWriter out) {
        try{
            out.write(s+"\n");
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Close and flush the given log writer. Flushing ensures that the data in the buffer is first save to the file
     * @param out file writer
     */
    public static void closeWriter(final BufferedWriter out) {
        try{
            out.flush();
            out.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Deletes all the data from the log directory
     * @return true if successfully deleted
     */
    public static boolean deleteData(){
        boolean deleted = false;
        File root = getStorageLocation();
        if(root!=null){
            File files[] = root.listFiles();
            if(files!=null){
                for(File file : files) {
                    if (!file.delete())
                        Log.d(TAG, "Deleting file failed: " + file.getName());
                }
            }
            deleted = root.delete();
        }
        return deleted;
    }
}
