package sk.plesko.bigfilefinder;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

/**
 * Created by Ivan on 15. 2. 2015.
 */
public class NotificationHelper {

    private final int NOTIFICATION_ID = 1;

    private NotificationCompat.Builder mBuilder;
    private Context mContext;
    android.app.NotificationManager mNotifyManager;

    public NotificationHelper(Context context) {
        mContext = context;

        mNotifyManager = (android.app.NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(mContext);
        mBuilder.setContentTitle(mContext.getString(R.string.searching))
                .setContentText(mContext.getString(R.string.preparing_search))
                .setSmallIcon(R.drawable.ic_in_progress);
        mBuilder.setProgress(100, 0, false);

        Intent toLaunch = new Intent(mContext, MainActivity.class);
        toLaunch.setAction("android.intent.action.MAIN");
        toLaunch.addCategory("android.intent.category.LAUNCHER");

        PendingIntent notificationIntent = PendingIntent.getActivity(mContext, 0, toLaunch, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(notificationIntent);
    }

    public void show() {
        mBuilder.setOngoing(true)
                .setAutoCancel(false);

        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    public void show(int max, int progress, String text) {
        mBuilder.setProgress(max, progress, false)
                .setContentText(text);

        show();
    }

    public void finish() {
        mBuilder.setContentText(mContext.getString(R.string.search_complete))
                .setProgress(0,0,false)
                .setOngoing(false)
                .setAutoCancel(true);

        mNotifyManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

}
