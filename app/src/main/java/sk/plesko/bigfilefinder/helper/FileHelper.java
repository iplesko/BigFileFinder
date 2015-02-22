package sk.plesko.bigfilefinder.helper;

import android.os.Environment;

import java.io.File;

/**
 * Created by Ivan on 15. 2. 2015.
 */
public class FileHelper {

    /**
     * Recursively traverses tree from the root, uses callback class to notify about results
     * @param rootDir can't be null, must be a directory, if not, a IllegalArgumentException exception is thrown
     * @param traverserCallback the callback class implementing FileHelper.TraverserCallback interface
     */
    public static void traverseTree(File rootDir, TraverserCallback traverserCallback) {
        if (rootDir == null || !rootDir.isDirectory()) {
            throw new IllegalArgumentException("The rootDir parameter has to be a directory");
        }

        if (rootDir.exists()) {
            // get all files in current directory
            File[] files = rootDir.listFiles();

            // count how many subdirectories there are
            int subDirCount = 0;
            for (File file : files) {
                if (!file.exists()) {
                    continue;
                }
                if (file.isDirectory()) {
                    subDirCount++;
                }
            }
            // notify about subdirectories found
            traverserCallback.subDirectoriesFound(subDirCount);

            // traverse
            for (File file : files) {
                if (!file.exists()) {
                    continue;
                }
                if (file.isDirectory()) {
                    traverseTree(file, traverserCallback); // recursion
                } else {
                    // notify about file found
                    traverserCallback.fileFound(file);
                }
            }
            // notify about directory searched finished
            traverserCallback.directorySearchFinished(rootDir);
        }
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
        public void subDirectoriesFound(int count);
        public void directorySearchFinished(File file);
    }

}
