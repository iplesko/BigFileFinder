package sk.plesko.bigfilefinder;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserFragment;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import sk.plesko.bigfilefinder.adapter.DirectoryListAdapter;

public class MainActivity extends ActionBarActivity implements DirectoryChooserFragment.OnFragmentInteractionListener {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private DirectoryChooserFragment mDialog;
    private DirectoryListAdapter mDirectoryListAdapter;
    private ListView mListView;
    private ViewGroup mSearchCriteriaView;
    private ViewGroup mProgressView;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private ConcurrentNavigableMap<Long, List<String>> fileMap;
    private int finished = 0;
    private int filesSearched = 0;
    private int totalFileCount = 0;
    private FileTraverseAsyncTask traverseTask1;
    private FileTraverseAsyncTask traverseTask2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSearchCriteriaView = (RelativeLayout) findViewById(R.id.searchCriteria);
        mProgressView = (RelativeLayout) findViewById(R.id.searchProgress);
        mProgressText = (TextView) findViewById(R.id.progressText);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        mListView = (ListView) findViewById(R.id.directoryList);
        mDirectoryListAdapter = new DirectoryListAdapter(this, R.layout.directory_list_item, R.id.pathName);
        mListView.setAdapter(mDirectoryListAdapter);

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

    }

    private void search() {

        mSearchCriteriaView.setVisibility(View.GONE);
        mProgressView.setVisibility(View.VISIBLE);

        CountFilesAsyncTask countFilesAsyncTask = new CountFilesAsyncTask(new CountFilesAsyncTask.OnCountFilesFinished() {
            @Override
            public void filesCounted(int count) {

                totalFileCount = count;
                mProgressBar.setMax(count);

                OnTraversingEventsListener onTraversingFinishedListener = new OnTraversingEventsListener();
                finished = 0;
                fileMap.clear();
                traverseTask1 = new FileTraverseAsyncTask(fileMap);
                traverseTask2 = new FileTraverseAsyncTask(fileMap);

                traverseTask1.setOnTraversingEventsListener(onTraversingFinishedListener);
                traverseTask1.setIdentifier("TR1");
                traverseTask1.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new File(mDirectoryListAdapter.getItem(0)));

                traverseTask2.setOnTraversingEventsListener(onTraversingFinishedListener);
                traverseTask2.setIdentifier("TR2");
                traverseTask2.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new File(mDirectoryListAdapter.getItem(1)));

            }
        });

        countFilesAsyncTask.execute(new File(mDirectoryListAdapter.getItem(0)), new File(mDirectoryListAdapter.getItem(1)));
    }

    private class OnTraversingEventsListener implements FileTraverseAsyncTask.OnTraversingEventsListener {
        @Override
        public synchronized void progressUpdate(int fileCount) {
            filesSearched += fileCount;
            mProgressBar.setProgress(filesSearched);
            mProgressText.setText(getString(R.string.progress_info, (int)(filesSearched * 100.0f / totalFileCount), filesSearched, totalFileCount));
        }

        @Override
        public synchronized void traversingFinished() {
            finished++;
            Log.d(LOG_TAG, "FINISHED: " + finished);
            if (finished == 2) {
                int i = 1;
                for (Map.Entry<Long, List<String>> fileMapEntry : fileMap.entrySet()) {
                    Long fileSize = fileMapEntry.getKey();
                    List<String> fileList = fileMapEntry.getValue();
                    for (String file : fileList ) {
                        Log.d(LOG_TAG, fileSize + ": " + file);
                        i++;
                        if (i > 3) {
                            break;
                        }
                    }
                    if (i > 3) {
                        break;
                    }
                }
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
