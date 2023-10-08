package com.example.appmobile;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import androidx.annotation.NonNull;

/**
 * Created by SAUL on 07/04/2020.
**/

public class MyFirebaseMessaging extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static SessionManager session = null;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "onNewToken: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Map<String, String> data = remoteMessage.getData();
        session = new SessionManager(this);
        int notify = session.getNotify();
        Log.i("Firebase", String.valueOf(notify));
        if(notify == 1){
            if (data.size() > 0) {
                Log.d(TAG, "data: " + data);
                String title = data.get("title");
                String msg = data.get("message");
                sendNotification(title, msg);

            } else{
                RemoteMessage.Notification notification = remoteMessage.getNotification();
                String title = notification.getTitle();
                String msg = notification.getBody();

                sendNotification(title, msg);
            }
        }

    }

    private void sendNotification(String title, String msg) {
        Intent intent = new Intent(this, MinhasReservas.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, MyNotification.NOTIFICATION_ID, intent, PendingIntent.FLAG_ONE_SHOT);

        MyNotification notification = new MyNotification(this, MyNotification.CHANNEL_ID_NOTIFICATIONS);
        notification.build(R.drawable.app_logo_s, title, msg, pendingIntent);
        notification.addChannel("Notificaciones", NotificationManager.IMPORTANCE_DEFAULT);
        notification.createChannelGroup(MyNotification.CHANNEL_GROUP_GENERAL, R.string.notification_channel_group_general);
        notification.show(MyNotification.NOTIFICATION_ID);
    }
}