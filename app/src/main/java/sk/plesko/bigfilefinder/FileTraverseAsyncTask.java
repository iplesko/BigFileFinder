package sk.plesko.bigfilefinder;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentNavigableMap;

import sk.plesko.bigfilefinder.helper.FileHelper;

/**
 * Created by Ivan on 14. 2. 2015.
 */
public class FileTraverseAsyncTask extends AsyncTask<File, Void, Void> {

    private final String LOG_TAG = FileTraverseAsyncTask.class.getSimpleName();
    private String identifier;
    private OnTraversingFinishedListener onTraversingFinishedListener = null;
    private ConcurrentNavigableMap<Long, List<String>> fileMap;

    public FileTraverseAsyncTask( ConcurrentNavigableMap<Long, List<String>> fileMap) {
        this.fileMap = fileMap;
    }

    public void setOnTraversingFinishedListener(OnTraversingFinishedListener onTraversingFinishedListener) {
        this.onTraversingFinishedListener = onTraversingFinishedListener;
    }

    @Override
    protected Void doInBackground(File... params) {

        for (File file : params) {
            FileHelper.traverseTree(file, new FileHelper.TraverserCallback() {
                @Override
                public void fileFound(File file) {
                    long fileSize = file.length();
                    List<String> list;
                    if (fileMap.containsKey(fileSize)) {
                        list = fileMap.get(fileSize);
                    } else {
                        list = new ArrayList<String>();
                    }
                    list.add(file.getAbsolutePath());
                    fileMap.put(file.length(), list);
                    Log.d(LOG_TAG, "FILE (" + identifier + "): " + file.getAbsolutePath());
                }
            });
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (onTraversingFinishedListener != null) {
            onTraversingFinishedListener.traversingFinished();
        }
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public interface OnTraversingFinishedListener {
        public void traversingFinished();
    }
}
