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
public class FileTraverseAsyncTask extends AsyncTask<File, FileTraverseAsyncTask.Progress, Void> {

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
        // for all root directories start the tree traversing with the TraverseCallback class
        TraverserCallback traverserCallback = new TraverserCallback();
        for (File file : params) {
            FileHelper.traverseTree(file, traverserCallback);
        }

        return null;
    }

    private class TraverserCallback implements FileHelper.TraverserCallback {

        private long time;
        private long time2;
        private int i = 0;
        private int j = 0;

        public TraverserCallback() {
            time = time2 = System.currentTimeMillis();
        }

        @Override
        public synchronized void fileFound(File file) {
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
        }

        @Override
        public void subDirectoriesFound(int count) {
            long now = System.currentTimeMillis();
            i += count;
            // notify no more often than 100ms, because the app would be slow
            if (now - time > 100) {
                publishProgress(new Progress(Progress.PROGRESS_TYPE_DIRECTORY_FOUND, i));
                i = 0;
                time = now;
            }
        }

        @Override
        public void directorySearchFinished(File file) {
            long now = System.currentTimeMillis();
            j++;
            // notify no more often than 100ms, because the app would be slow
            if (now - time2 > 100) {
                publishProgress(new Progress(Progress.PROGRESS_TYPE_DIRECTORY_FINISHED, j));
                j = 0;
                time2 = now;
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
    protected void onProgressUpdate(Progress... progresses) {
        for (Progress progress : progresses) {
            onTraversingEventsListener.progressUpdate(progress);
        }
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public interface OnTraversingEventsListener {
        public void traversingFinished();
        public void progressUpdate(Progress progress);
    }

    // a class representing progress. because we update progress bar MAX value "on the fly", there are two kinds of progress
    // PROGRESS_TYPE_DIRECTORY_FINISHED - directory search was finished, getProgress() returns how many directories were finished
    // PROGRESS_TYPE_DIRECTORY_FOUND - new directory found, getProgress() returns subdirectory count
    public class Progress {
        public static final int PROGRESS_TYPE_DIRECTORY_FINISHED = 1;
        public static final int PROGRESS_TYPE_DIRECTORY_FOUND = 2;

        private int progressType;
        private int progress;

        public Progress(int progressType, int progress) {
            this.progressType = progressType;
            this.progress = progress;
        }

        public int getProgressType() {
            return progressType;
        }

        public int getProgress() {
            return progress;
        }
    }
}
