package io.github.budgetninja.fairwellandroid;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;


public class CustomBroadcastReceiver extends ParsePushBroadcastReceiver {

    int NOTIFY_ID_FRIEND_REQUEST=1;
    int NOTIFY_ID_FRIEND_REQUEST_ACCEPTED=2;

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {
            JSONObject json = new JSONObject(intent.getExtras().getString("com.parse.Data"));
            final String notificationKey = json.getString("key");
            if (notificationKey.equals("FRIEND_REQUEST")){
                final String userOneUsername = json.getString("userOneUsername");

                //Customize your notification - sample code
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                builder.setContentTitle("Fairwell");
                builder.setSmallIcon(R.mipmap.ic_launcher);
                builder.setContentText(userOneUsername + " sent you a friend request");
                builder.setAutoCancel(true);

                Intent go_onClick = new Intent(context, ContentActivity.class);
                go_onClick.putExtra("notificationKey", "FRIEND_REQUEST");
                PendingIntent onClick_wrapper = PendingIntent.getActivity(context, 0, go_onClick,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(onClick_wrapper);

                NotificationManager mNotifyMgr =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotifyMgr.notify(NOTIFY_ID_FRIEND_REQUEST, builder.build());
            } else if (notificationKey.equals("FRIEND_REQUEST_ACCEPTED")){
                final String userTwoUsername = json.getString("userTwoUsername");
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                builder.setContentTitle("Fairwell");
                builder.setSmallIcon(R.mipmap.ic_launcher);
                builder.setContentText(userTwoUsername + " has accepted your friend request");
                builder.setAutoCancel(true);

                Intent go_onClick = new Intent(context, ContentActivity.class);
                go_onClick.putExtra("notificationKey", "FRIEND_REQUEST_ACCEPTED");
                PendingIntent onClick_wrapper = PendingIntent.getActivity(context, 0, go_onClick, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.setContentIntent(onClick_wrapper);

                NotificationManager mNotifyMgr =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotifyMgr.notify(NOTIFY_ID_FRIEND_REQUEST_ACCEPTED, builder.build());
            }
        } catch (JSONException e) {
            Log.d("error", e.getMessage());
        }
    }
}