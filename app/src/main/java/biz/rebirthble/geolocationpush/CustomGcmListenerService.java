package biz.rebirthble.geolocationpush;


import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.GeofencingRequest;
import com.nifty.cloud.mb.core.NCMBGcmListenerService;
import com.nifty.cloud.mb.core.NCMBObject;

public class CustomGcmListenerService extends NCMBGcmListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ResultCallback
{

    protected static final String TAG = "CustomListenerService";

    protected static final int GEOFENCE_RADIUS_IN_METERS = 500;

    protected static final int GEOFENCE_EXPIRATION_IN_HOURS = 1;

    protected static final int GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;

    protected static final String PREFS_NAME = "GeolocationPush";

    protected static final String GEOFENCE_NAME = "GeofenceName";

    private GoogleApiClient mGoogleApiClient;

    private GeofencingRequest mGeofenceRequest;

    @Override
    public void onMessageReceived(String from, Bundle data) {

        //ペイロードデータの取得


        //デフォルトの受信処理
        super.onMessageReceived(from, data);
    }

    /**
     * GeofenceRequestの作成を行う
     * @param point GeofenceRequestの作成に使うロケーション情報
     */
    private void createGeofenceRequest(NCMBObject point) {

    }

    /**
     * Google API Clientのビルドと接続を行う
     */
    protected synchronized void connectGoogleApiClient() {

    }

    /**
     * Geofence用のPendingIntentを作成する
     * @return Geofence用のPendingIntent
     */
    private PendingIntent getGeofencePendingIntent() {

        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    /**
     * Google API Clientへの接続が行われた場合に呼び出されるコールバック
     * @param bundle
     */
    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection Failed");
    }

    @Override
    public void onResult(Result result) {
        Log.d(TAG, "onResult:" + result.toString());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Gcm service is destroyed...");
        super.onDestroy();
    }
}
