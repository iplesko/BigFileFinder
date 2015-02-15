package sk.plesko.bigfilefinder;

import android.os.AsyncTask;
import android.util.Log;

import java.io.File;

import sk.plesko.bigfilefinder.helper.FileHelper;

/**
 * Created by Ivan on 15. 2. 2015.
 */
public class CountFilesAsyncTask extends AsyncTask<File, Void, Integer> {

    private final String LOG_TAG = CountFilesAsyncTask.class.getSimpleName();
    private OnCountFilesFinished onCountFilesFinished;

    public CountFilesAsyncTask(OnCountFilesFinished onCountFilesFinished) {
        this.onCountFilesFinished = onCountFilesFinished;
    }

    @Override
    protected Integer doInBackground(File... files) {
        int count = 0;
        for (File file : files) {
            count += FileHelper.getFileCount(file);
        }

        return count;
    }

    @Override
    protected void onPostExecute(Integer fileCount) {
        onCountFilesFinished.filesCounted(fileCount);

    }

    public interface OnCountFilesFinished {
        public void filesCounted(int count);
    }
}
