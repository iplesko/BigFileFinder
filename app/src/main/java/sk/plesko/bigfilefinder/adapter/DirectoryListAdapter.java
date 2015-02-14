package sk.plesko.bigfilefinder.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import sk.plesko.bigfilefinder.R;

/**
 * Created by Ivan on 14. 2. 2015.
 */
public class DirectoryListAdapter extends ArrayAdapter<String> {

    public DirectoryListAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        view.findViewById(R.id.remove).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remove(getItem(position));
            }
        });

        return view;
    }

}
