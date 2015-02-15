package sk.plesko.bigfilefinder.helper;

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

    public interface TraverserCallback {
        public void fileFound(File file);
    }

}
