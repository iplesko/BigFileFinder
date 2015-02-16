package sk.plesko.bigfilefinder;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import sk.plesko.bigfilefinder.adapter.DirectoryListAdapter;
import sk.plesko.bigfilefinder.helper.FileHelper;

public class MainActivity extends ActionBarActivity implements DirectoryChooserFragment.OnFragmentInteractionListener {

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private DirectoryChooserFragment mDialog;
    private DirectoryListAdapter mDirectoryListAdapter;
    private ListView mListView;
    private ListView mSearchResultListView;
    private ViewGroup mSearchCriteriaView;
    private ViewGroup mProgressView;
    private ViewGroup mSearchResults;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private EditText mNumberOfResults;
    private ConcurrentNavigableMap<Long, List<String>> fileMap;
    private int finished = 0;
    private int finishedDirectories = 0;
    private int totalDirectories = 0;
    private int resultCount;
    private FileTraverseAsyncTask traverseInternalStorageTask;
    private FileTraverseAsyncTask traverseExternalStorageTask;
    private NotificationHelper mNotificationHelper;

    // standard onCreate stuff.. finding views, assigning UI elements listeners and options, etc.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSearchCriteriaView = (ViewGroup) findViewById(R.id.searchCriteria);
        mProgressView = (ViewGroup) findViewById(R.id.searchProgress);
        mSearchResults = (ViewGroup) findViewById(R.id.searchResults);
        mProgressText = (TextView) findViewById(R.id.progressText);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mNumberOfResults = (EditText) findViewById(R.id.numberOfResults);

        mProgressBar.setMax(100);

        resultCount = Integer.valueOf(mNumberOfResults.getText().toString());

        mListView = (ListView) findViewById(R.id.directoryList);
        mDirectoryListAdapter = new DirectoryListAdapter(this, R.layout.directory_list_item, R.id.pathName);
        mListView.setAdapter(mDirectoryListAdapter);

        mSearchResultListView = (ListView) findViewById(R.id.searchResultListView);

        fileMap = new ConcurrentSkipListMap<>(new Comparator<Long>() {
            @Override
            public int compare(Long lhs, Long rhs) {
                return rhs.intValue() - lhs.intValue();
            }
        });

        Button addDirectoryButton = (Button) findViewById(R.id.add_directory);
        addDirectoryButton.setOnClickListener(new AddDirectoryButtonClickListener());

        Button startSearchButton = (Button) findViewById(R.id.start_search);
        startSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });

        findViewById(R.id.newSearchButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                init();
            }
        });

        mNotificationHelper = new NotificationHelper(this);

        init();
    }

    // initialize view groups visibilities, variables and progerss bar
    private void init() {
        mSearchCriteriaView.setVisibility(View.VISIBLE);
        mProgressView.setVisibility(View.GONE);
        mSearchResults.setVisibility(View.GONE);
        finished = 0;
        fileMap.clear();
        finishedDirectories = 0;
        totalDirectories = 0;
        resultCount = 0;
        mDirectoryListAdapter.clear();

        mProgressBar.setProgress(0);
        mProgressText.setText(R.string.preparing_search);
    }

    private void search() {

        // make number of results a required field
        if ("".equals(mNumberOfResults.getText().toString().trim())) {
            mNumberOfResults.setError(getString(R.string.required_field));
            return;
        }

        // check if there is a decimal in the number field
        try {
            resultCount = Integer.valueOf(mNumberOfResults.getText().toString());
        } catch (NumberFormatException e) {
            mNumberOfResults.setError(getString(R.string.must_be_numeric));
            return;
        }

        // display notification in the system notification area
        mNotificationHelper.show();

        // hide software keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

        // set visibilities of view groups, hide search criteria dialog, show progress dialog
        mSearchCriteriaView.setVisibility(View.GONE);
        mProgressView.setVisibility(View.VISIBLE);

        // devide directories to internal and external groups. Each group will be searched by its own thread
        final List<File> internalStorageDirectoryList = new ArrayList<>();
        final List<File> externalStorageDirectoryList = new ArrayList<>();

        int selectedDirCount = mDirectoryListAdapter.getCount();
        for (int i = 0; i < selectedDirCount; i++) {
            String item = mDirectoryListAdapter.getItem(i);
            File file = new File(item);
            if (FileHelper.isOnExternalStorage(item)) {
                externalStorageDirectoryList.add(file);
            } else {
                internalStorageDirectoryList.add(file);
            }
        }

        OnTraversingEventsListener onTraversingFinishedListener = new OnTraversingEventsListener();

        // initialize and start the tasks using thread pool executor
        traverseInternalStorageTask = new FileTraverseAsyncTask(fileMap);
        traverseExternalStorageTask = new FileTraverseAsyncTask(fileMap);

        traverseInternalStorageTask.setOnTraversingEventsListener(onTraversingFinishedListener);
        traverseInternalStorageTask.setIdentifier("INTERNAL");
        traverseInternalStorageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, internalStorageDirectoryList.toArray(new File[internalStorageDirectoryList.size()]));

        traverseExternalStorageTask.setOnTraversingEventsListener(onTraversingFinishedListener);
        traverseExternalStorageTask.setIdentifier("EXTERNAL");
        traverseExternalStorageTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, externalStorageDirectoryList.toArray(new File[externalStorageDirectoryList.size()]));
    }

    private class OnTraversingEventsListener implements FileTraverseAsyncTask.OnTraversingEventsListener {

        // search progress update
        @Override
        public synchronized void progressUpdate(FileTraverseAsyncTask.Progress progress) {

            if (progress.getProgressType() == FileTraverseAsyncTask.Progress.PROGRESS_TYPE_DIRECTORY_FINISHED) {
                // if search of a directory finished, increment the progress
                finishedDirectories += progress.getProgress();
            } else {
                // if we found new directories, increment total directory count
                totalDirectories += progress.getProgress();
            }

            // calculate percentual progress
            int progressPercent;
            if (totalDirectories == 0) {
                progressPercent = 0;
            } else {
                progressPercent = (int) (finishedDirectories * 100.0f / totalDirectories);
            }

            // if progress is higher than before, update progress information in notification and in the app
            if (mProgressBar.getProgress() < progressPercent && progressPercent <= 100) {
                mProgressBar.setProgress(progressPercent);
                String progressText = getString(R.string.progress_info, progressPercent);
                mProgressText.setText(progressText);
                mNotificationHelper.show(100, progressPercent, progressText);
            }
        }

        @Override
        public synchronized void traversingFinished() {
            finished++;
            Log.d(LOG_TAG, "FINISHED: " + finished);

            // if both of the threads finished, output the results to the result list
            if (finished == 2) {

                ArrayAdapter<String> resultsAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1);
                mSearchResultListView.setAdapter(resultsAdapter);

                int i = 1;
                for (Map.Entry<Long, List<String>> fileMapEntry : fileMap.entrySet()) {
                    Long fileSize = fileMapEntry.getKey();
                    List<String> fileList = fileMapEntry.getValue();
                    for (String file : fileList) {
                        resultsAdapter.add(file + " (" + FileHelper.humanReadableByteCount(fileSize, false) + ")");
                        i++;
                        if (i > resultCount) {
                            break;
                        }
                    }
                    if (i > resultCount) {
                        break;
                    }
                }

                // update view group visibilities, hide progress bar, show result screen
                mProgressView.setVisibility(View.GONE);
                mSearchResults.setVisibility(View.VISIBLE);

                // make the notification cancellable
                mNotificationHelper.finish();
            }
        }
    }

    private class AddDirectoryButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            mDialog = DirectoryChooserFragment.newInstance("DialogSample", null);
            mDialog.show(getFragmentManager(), null);
        }
    }

    @Override
    public void onSelectDirectory(@NonNull String s) {
        mDialog.dismiss();

        int itemCount = mDirectoryListAdapter.getCount();

        // detect if the list doesn't contain parent of new directory
        // if it does, it is not necessary to add it
        for (int i = 0; i < itemCount; i++) {
            if (s.startsWith(mDirectoryListAdapter.getItem(i))) {
                Toast.makeText(this, R.string.parent_already_in_the_list, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // detect if the list doesn't contain children of new directory
        // if it does, remove them, they are redundant
        String item;
        boolean removed = false;
        // NOTE: iterating from end to the beginning because we remove items from the list we are iterating
        for (int i = itemCount - 1; i >= 0; i--) {
            item = mDirectoryListAdapter.getItem(i);
            if (item.startsWith(s)) {
                mDirectoryListAdapter.remove(mDirectoryListAdapter.getItem(mDirectoryListAdapter.getPosition(item)));
                removed = true;
            }
        }
        if (removed) {
            Toast.makeText(this, R.string.subdirectories_removed, Toast.LENGTH_SHORT).show();
        }

        if (mDirectoryListAdapter.getPosition(s) == -1) {
            mDirectoryListAdapter.add(s);
        } else {
            Toast.makeText(this, R.string.directory_already_in_list, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCancelChooser() {
        mDialog.dismiss();
    }
}
