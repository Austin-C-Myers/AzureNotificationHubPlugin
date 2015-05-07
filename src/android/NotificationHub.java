package msopentech.azure;

import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;
import android.app.NotificationManager;
import android.app.PendingIntent;

//import android.support.v4.app.TaskStackBuilder;
//import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.microsoft.windowsazure.messaging.NativeRegistration;


/**
 * Apache Cordova plugin for Windows Azure Notification Hub
 */
public class NotificationHub extends CordovaPlugin {

    /**
     * The callback context from which we were invoked.
     */
    protected static CallbackContext _callbackContext = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        _callbackContext = callbackContext;
        try {
            
            if (action.equals("registerApplication")) {   
                    String hubName = args.getString(0);
                    String connectionString = args.getString(1);
                    String senderId = args.getString(4);
                    registerApplication(hubName, connectionString, senderId);
                    return true;
            }
            
            if (action.equals("unregisterApplication")) {
                String hubName = args.getString(0);
                String connectionString = args.getString(1);
                unregisterApplication(hubName, connectionString);
                return true;
            } 
            
            return false; // invalid action            
        } catch (Exception e) {
            _callbackContext.error(e.getMessage());
        }
        return true;
    }

    /**
     * Asynchronously registers the device for native notifications.
     */
    @SuppressWarnings("unchecked")
    private void registerApplication(final String hubName, final String connectionString, final String senderId) {

        try {
            final GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(cordova.getActivity());
            final com.microsoft.windowsazure.messaging.NotificationHub hub = 
                    new com.microsoft.windowsazure.messaging.NotificationHub(hubName, connectionString, cordova.getActivity());

            new AsyncTask() {
                @Override
                protected Object doInBackground(Object... params) {
                   try {
                      String gcmId = gcm.register(senderId);
                      NativeRegistration registrationInfo = hub.register(gcmId);
                      
                      JSONObject registrationResult = new JSONObject();
                      registrationResult.put("registrationId", registrationInfo.getRegistrationId());
                      registrationResult.put("channelUri", registrationInfo.getGCMRegistrationId());
                      registrationResult.put("notificationHubPath", registrationInfo.getNotificationHubPath());
                      registrationResult.put("event", "registerApplication");
                      
                      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, registrationResult);
                      // keepKallback is used to continue using the same callback to notify about push notifications received
                      pluginResult.setKeepCallback(true); 
                      
                      NotificationHub.getCallbackContext().sendPluginResult(pluginResult);
                      
                   } catch (Exception e) {
                       NotificationHub.getCallbackContext().error(e.getMessage());
                   }
                   return null;
               }
             }.execute(null, null, null);
        } catch (Exception e) {
            NotificationHub.getCallbackContext().error(e.getMessage());
        }
    }

    /**
     * Unregisters the device for native notifications.
     */
    private void unregisterApplication(final String hubName, final String connectionString) {
        try {
            final com.microsoft.windowsazure.messaging.NotificationHub hub = 
                    new com.microsoft.windowsazure.messaging.NotificationHub(hubName, connectionString, cordova.getActivity());
            hub.unregister();
            NotificationHub.getCallbackContext().success();            
        } catch (Exception e) {
            NotificationHub.getCallbackContext().error(e.getMessage());
        }
    }
    
    /**
     * Handles push notifications received.
     */
    public static class PushNotificationReceiver extends android.content.BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            
            if (NotificationHub.getCallbackContext() == null){
                return;
            }                                    
            JSONObject json = new JSONObject();
            try {
                
                Set<String> keys = intent.getExtras().keySet();
                for (String key : keys) {
                    json.put(key, intent.getExtras().get(key));
                }
                PluginResult result = new PluginResult(PluginResult.Status.OK, json);
                result.setKeepCallback(true);
                NotificationHub.getCallbackContext().sendPluginResult(result);
				Toast.makeText(context, "Notification Received", Toast.LENGTH_LONG).show();
				/*
				//show notification on drop down
				NotificationCompat.Builder mBuilder =
						new NotificationCompat.Builder(context)
						//.setSmallIcon(R.drawable.notification_icon)
						.setContentTitle("My notification")
						.setContentText("Hello World!");
				// Creates an explicit intent for an Activity in your app
				Intent resultIntent = new Intent(context, NotificationHub.class);

				// The stack builder object will contain an artificial back stack for the
				// started Activity.
				// This ensures that navigating backward from the Activity leads out of
				// your application to the Home screen.
				TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
				// Adds the back stack for the Intent (but not the Intent itself)
				stackBuilder.addParentStack(NotificationHub.class);
				// Adds the Intent that starts the Activity to the top of the stack
				stackBuilder.addNextIntent(resultIntent);
				PendingIntent resultPendingIntent =
						stackBuilder.getPendingIntent(
							0,
							PendingIntent.FLAG_UPDATE_CURRENT
						);
				mBuilder.setContentIntent(resultPendingIntent);
				NotificationManager mNotificationManager =
					(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
				// mId allows you to update the notification later on.
				int mId = 354362435;//testing
				mNotificationManager.notify(mId, mBuilder.build());
				*/
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
    }
    
    /**
     * Returns plugin callback.
     */
    protected static CallbackContext getCallbackContext() {
        return _callbackContext;
    }
}