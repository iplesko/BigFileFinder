package sk.plesko.bigfilefinder.adapter;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Created by Ivan on 14. 2. 2015.
 */
public class FileTraverseAsyncTask extends AsyncTask<File, Void, Void> {

    private final String LOG_TAG = FileTraverseAsyncTask.class.getSimpleName();
    private String identifier;
    private OnTraversingFinishedListener onTraversingFinishedListener = null;
    private ConcurrentSkipListMap<Long, String> fileMap;

    public FileTraverseAsyncTask(ConcurrentSkipListMap<Long, String> fileMap) {
        this.fileMap = fileMap;
    }

    public void setOnTraversingFinishedListener(OnTraversingFinishedListener onTraversingFinishedListener) {
        this.onTraversingFinishedListener = onTraversingFinishedListener;
    }

    @Override
    protected Void doInBackground(File... params) {

        for (File file : params) {
            traverse(file);
        }

        return null;
    }

    private void traverse (File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (file.isDirectory()) {
                    traverse(file);
                } else {
                    fileMap.put(file.length(), file.getAbsolutePath());
                    Log.d(LOG_TAG, "FILE ("+identifier+"): " + file.getAbsolutePath());
                }
            }
        }
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
