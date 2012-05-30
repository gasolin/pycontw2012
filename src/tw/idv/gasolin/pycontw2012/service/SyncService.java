package tw.idv.gasolin.pycontw2012.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.format.DateUtils;
import android.util.Log;

import tw.idv.gasolin.pycontw2012.R;
import tw.idv.gasolin.pycontw2012.io.LocalExecutor;
import tw.idv.gasolin.pycontw2012.io.LocalRoomsHandler;
import tw.idv.gasolin.pycontw2012.io.LocalSearchSuggestHandler;
import tw.idv.gasolin.pycontw2012.io.LocalTracksHandler;
import tw.idv.gasolin.pycontw2012.io.RemoteExecutor;
import tw.idv.gasolin.pycontw2012.io.RemoteSessionsHandler;
import tw.idv.gasolin.pycontw2012.io.RemoteSponsorsHandler;
import tw.idv.gasolin.pycontw2012.util.Pref;

public class SyncService extends IntentService {

    private static final String LOG_TAG = SyncService.class.getSimpleName();

    public static final String EXTRA_STATUS_RECEIVER = "tw.idv.gasolin.pycontw2012.extra.STATUS_RECEIVER";

    public static final int STATUS_RUNNING = 0x1;
    public static final int STATUS_ERROR = 0x2;
    public static final int STATUS_FINISHED = 0x3;

    private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;

//    private static final String ROOMS_URL = "http://192.168.1.131:9006/api/program/rooms/";
//    private static final String TRACKS_URL = "http://192.168.1.131:9006/api/program/types/";
//    private static final String SESSIONS_URL = "http://192.168.1.131:9006/api/program/";
//    private static final String SPONSORS_URL = "http://192.168.1.131:9006/api/sponsors/";

    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String ENCODING_GZIP = "gzip";

    private static final int VERSION_NONE = 0;
    private static final int VERSION_CURRENT = 1;

    private LocalExecutor mLocalExecutor;
    private RemoteExecutor mRemoteExecutor;

    public SyncService() {
        super(LOG_TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final HttpClient httpClient = getHttpClient(this);
        final ContentResolver resolver = getContentResolver();

        mLocalExecutor = new LocalExecutor(getResources(), resolver);
        mRemoteExecutor = new RemoteExecutor(
            httpClient,
            resolver,
            getResources());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOG_TAG, "onHandleIntent(intent=" + intent.toString() + ")");

        final ResultReceiver receiver = intent.getParcelableExtra(EXTRA_STATUS_RECEIVER);
        if ( receiver != null )
            receiver.send(STATUS_RUNNING, Bundle.EMPTY);

        final Context context = this;
        final SharedPreferences prefs = getSharedPreferences(Prefs.SCHED_SYNC,
            Context.MODE_PRIVATE);
        final int localVersion = prefs.getInt(Prefs.LOCAL_VERSION, VERSION_NONE);

        try {
            // Bulk of sync work, performed by executing several fetches from
            // local and online sources.

            final long startLocal = System.currentTimeMillis();
            final boolean localParse = localVersion < VERSION_CURRENT;
            Log.d(LOG_TAG, "found localVersion=" + localVersion
                + " and VERSION_CURRENT=" + VERSION_CURRENT);
            if ( localParse ) {
                // Load static local data
                mLocalExecutor.execute(R.raw.rooms, new LocalRoomsHandler());
                mLocalExecutor.execute(R.raw.tracks, new LocalTracksHandler());
                mLocalExecutor.execute(R.xml.search_suggest,
                    new LocalSearchSuggestHandler());

                // Save local parsed version
                prefs.edit()
                    .putInt(Prefs.LOCAL_VERSION, VERSION_CURRENT)
                    .commit();
            }
            Log.d(LOG_TAG, "local sync took "
                + ( System.currentTimeMillis() - startLocal ) + "ms");

            // Always hit remote spreadsheet for any updates
            final long startRemote = System.currentTimeMillis();

            mRemoteExecutor.executeGet(Pref.getRoomUrl(context), new LocalRoomsHandler());
            mRemoteExecutor.executeGet(Pref.getTracksUrl(context), new LocalTracksHandler());
            mRemoteExecutor.executeGet(Pref.getSessionsUrl(context),
                new RemoteSessionsHandler());
            mRemoteExecutor.executeGet(Pref.getSponsorUrl(context),
                new RemoteSponsorsHandler());
            Log.d(LOG_TAG, "remote sync took "
                + ( System.currentTimeMillis() - startRemote ) + "ms");

        } catch ( Exception e ) {
            Log.e(LOG_TAG, "Problem while syncing", e);

            if ( receiver != null ) {
                // Pass back error to surface listener
                final Bundle bundle = new Bundle();
                bundle.putString(Intent.EXTRA_TEXT, e.toString());
                receiver.send(STATUS_ERROR, bundle);
            }
        }

        // Announce success to any surface listener
        Log.d(LOG_TAG, "sync finished");
        if ( receiver != null )
            receiver.send(STATUS_FINISHED, Bundle.EMPTY);
    }

    /**
     * Generate and return a {@link HttpClient} configured for general use,
     * including setting an application-specific user-agent string.
     */
    public static HttpClient getHttpClient(Context context) {
        final HttpParams params = new BasicHttpParams();

        // Use generous timeouts for slow mobile networks
        HttpConnectionParams.setConnectionTimeout(params, 20 * SECOND_IN_MILLIS);
        HttpConnectionParams.setSoTimeout(params, 20 * SECOND_IN_MILLIS);

        HttpConnectionParams.setSocketBufferSize(params, 8192);
        HttpProtocolParams.setUserAgent(params, buildUserAgent(context));

        final DefaultHttpClient client = new DefaultHttpClient(params);

        client.addRequestInterceptor(new HttpRequestInterceptor() {
            public void process(HttpRequest request, HttpContext context) {
                // Add header to accept gzip content
                if ( !request.containsHeader(HEADER_ACCEPT_ENCODING) ) {
                    request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
                }
            }
        });

        client.addResponseInterceptor(new HttpResponseInterceptor() {
            public void process(HttpResponse response, HttpContext context) {
                // Inflate any responses compressed with gzip
                final HttpEntity entity = response.getEntity();
                final Header encoding = entity.getContentEncoding();
                if ( encoding != null ) {
                    for ( HeaderElement element : encoding.getElements() ) {
                        if ( element.getName()
                            .equalsIgnoreCase(ENCODING_GZIP) ) {
                            response.setEntity(new InflatingEntity(
                                response.getEntity()));
                            break;
                        }
                    }
                }
            }
        });

        return client;
    }

    /**
     * Build and return a user-agent string that can identify this application
     * to remote servers. Contains the package name and version code.
     */
    private static String buildUserAgent(Context context) {
        try {
            final PackageManager manager = context.getPackageManager();
            final PackageInfo info = manager.getPackageInfo(
                context.getPackageName(), 0);

            // Some APIs require "(gzip)" in the user-agent string.
            return info.packageName + "/" + info.versionName + " ("
                + info.versionCode + ") (gzip)";
        } catch ( NameNotFoundException e ) {
            return null;
        }
    }

    /**
     * Simple {@link HttpEntityWrapper} that inflates the wrapped
     * {@link HttpEntity} by passing it through {@link GZIPInputStream}.
     */
    private static class InflatingEntity extends HttpEntityWrapper {
        public InflatingEntity(HttpEntity wrapped) {
            super(wrapped);
        }

        @Override
        public InputStream getContent() throws IOException {
            return new GZIPInputStream(wrappedEntity.getContent());
        }

        @Override
        public long getContentLength() {
            return -1;
        }
    }

    private interface Prefs {
        String SCHED_SYNC = "sched_sync";
        String LOCAL_VERSION = "local_version";
    }

}
