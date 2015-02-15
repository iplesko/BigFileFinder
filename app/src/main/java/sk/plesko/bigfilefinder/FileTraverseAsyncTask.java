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
public class FileTraverseAsyncTask extends AsyncTask<File, Integer, Void> {

    private final String LOG_TAG = FileTraverseAsyncTask.class.getSimpleName();
    private String identifier;
    private OnTraversingEventsListener onTraversingEventsListener = null;
    private ConcurrentNavigableMap<Long, List<String>> fileMap;

    public FileTraverseAsyncTask( ConcurrentNavigableMap<Long, List<String>> fileMap) {
        this.fileMap = fileMap;
    }

    public void setOnTraversingEventsListener(OnTraversingEventsListener onTraversingEventsListener) {
        this.onTraversingEventsListener = onTraversingEventsListener;
    }

    @Override
    protected Void doInBackground(File... params) {
        for (File file : params) {
            FileHelper.traverseTree(file, new TraverserCallback());
        }

        return null;
    }

    private class TraverserCallback implements FileHelper.TraverserCallback {

        private long time;
        private int i = 0;

        public TraverserCallback() {
            time = System.currentTimeMillis();
        }

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
//            Log.d(LOG_TAG, "FILE (" + identifier + "): " + file.getAbsolutePath());

            i++;

            // publish progress every 100 milliseconds
            long now = System.currentTimeMillis();
            if (now - time > 100) {
                publishProgress(i);
                i = 0;
                time = now;
            }
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (onTraversingEventsListener != null) {
            onTraversingEventsListener.traversingFinished();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        for (int fileCount : values) {
            onTraversingEventsListener.progressUpdate(fileCount);
        }
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public interface OnTraversingEventsListener {
        public void traversingFinished();
        public void progressUpdate(int fileCount);
    }
}
