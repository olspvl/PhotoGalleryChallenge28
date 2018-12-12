package com.bignerdranch.android.photogallery;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PollJobService extends JobService {
    private static String TAG = "PollJobService";

    private PollTask mCurrentTask;

    @Override
    public boolean onStartJob(JobParameters params) {
        mCurrentTask = new PollTask(this);
        mCurrentTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if(mCurrentTask != null) {
            mCurrentTask.cancel(true);
        }
        return true;
    }

    private class PollTask extends AsyncTask<JobParameters, Void, Void> {

        Context mContext;

        public PollTask(Context context) {
            mContext = context;
        }

        @Override
        protected Void doInBackground(JobParameters... jobParameters) {
            JobParameters jobParams = jobParameters[0];

            String query = QueryPreferences.getStoredQuery(mContext);
            String lastResultId = QueryPreferences.getLastResultId(mContext);
            List<GalleryItem> items;

            if(query == null) {
                items = new FlickrFetchr().fetchRecentPhotos();
            } else {
                items = new FlickrFetchr().searchPhotos(query);
            }

            if(items.size() == 0) {
                return null;
            }
            String resultId = items.get(0).getId();
            if(resultId.equals(lastResultId)) {
                Log.i(TAG, "Got an old result: " + resultId);
            } else {
                Log.i(TAG, "Got a new result: " + resultId);

                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(mContext);
                PendingIntent pi = PendingIntent.getActivity(mContext, 0, i,0);

                Notification notification = new NotificationCompat.Builder(mContext, TAG)
                        .setTicker(resources.getString(R.string.new_picture_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_picture_title))
                        .setContentText(resources.getString(R.string.new_picture_text))
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build();

                NotificationManagerCompat notificationManager =
                        NotificationManagerCompat.from(mContext);
                notificationManager.notify(0, notification);
            }
            QueryPreferences.setLastResultId(mContext, resultId);

            jobFinished(jobParams, false);
            return null;
        }
    }
}
