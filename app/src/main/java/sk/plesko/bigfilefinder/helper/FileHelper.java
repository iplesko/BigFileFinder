package sk.plesko.bigfilefinder.helper;

import android.os.Environment;

import java.io.File;

/**
 * Created by Ivan on 15. 2. 2015.
 */
public class FileHelper {

    public static void traverseTree(File rootDir, TraverserCallback traverserCallback) {
        if (rootDir == null || !rootDir.isDirectory()) {
            throw new IllegalArgumentException("The rootDir parameter has to be a directory");
        }

        if (rootDir.exists()) {
            File[] files = rootDir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    traverseTree(file, traverserCallback);
                } else {
                    traverserCallback.fileFound(file);
                }
            }
        }
    }

    public static int getFileCount(File rootDir) {
        if (rootDir == null || !rootDir.isDirectory()) {
            throw new IllegalArgumentException("The rootDir parameter has to be a directory");
        }

        int count = 0;

        if (rootDir.exists()) {
            File[] files = rootDir.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    count += getFileCount(file);
                } else {
                    count++;
                }
            }
        }

        return count;
    }

    // taken from http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java#answer-3758880
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    // TODO: check file for null value and if file exists
    public static boolean isOnExternalStorage(File file) {
        return isOnExternalStorage(file.getAbsolutePath());
    }

    // TODO: check filePath for null value and if file exists
    public static boolean isOnExternalStorage(String filePath) {
        String externalStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(externalStorageState) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(externalStorageState)) {
            String externalSotagePath = Environment.getExternalStorageDirectory().getAbsolutePath();
            return filePath.startsWith(externalSotagePath);
        }
        return false;
    }

    public interface TraverserCallback {
        public void fileFound(File file);
    }

}
