package tw.idv.gasolin.pycontw2012.io;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import tw.idv.gasolin.pycontw2012.provider.CoscupContract;
import tw.idv.gasolin.pycontw2012.provider.CoscupProvider;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Blocks;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Sessions;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.SyncColumns;
import tw.idv.gasolin.pycontw2012.provider.CoscupDatabase.SessionsSpeakers;
import tw.idv.gasolin.pycontw2012.provider.CoscupDatabase.SessionsTracks;
import tw.idv.gasolin.pycontw2012.util.ParserUtils;

public class RemoteSessionsHandler extends JSONArrayHandler {

    private static final String LOG_TAG = RemoteSessionsHandler.class.getSimpleName();

    public static final int INVALID_TRACK_ID = -1;

    private final RemoteBlocksHandler mBlocksHandler;
    private final RemoteSpeakersHandler mSpeakersHandler;

    public RemoteSessionsHandler() {
        super(CoscupContract.CONTENT_AUTHORITY);
        mBlocksHandler = new RemoteBlocksHandler(
            CoscupContract.CONTENT_AUTHORITY);
        mSpeakersHandler = new RemoteSpeakersHandler(
            CoscupContract.CONTENT_AUTHORITY);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONArray json,
        ContentResolver resolver, Resources res) throws JSONException,
        IOException {
        final long now = System.currentTimeMillis();
        final ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        final int size = json.length();

        for ( int i = 0; i < size; i++ ) {
            JSONObject obj = json.getJSONObject(i);

            final String sessionId = parseSessionId(obj);

            if ( TextUtils.isEmpty(sessionId) ) {
                throw new HandlerException("Invalid sessionId!\n" + obj);
            }

            final Uri sessionUri = Sessions.buildSessionUri(sessionId);
            final int trackId = obj.optInt(Tags.SESSION_TRACK, -1);
            final int roomId = obj.optInt(Tags.SESSION_ROOM);
            final boolean isBreak = obj.getBoolean(Tags.SESSION_IS_BREAK);

            // Check for existing details
            final ContentValues oldValues = querySessionDetails(sessionUri,
                resolver);

            batch.addAll(mBlocksHandler.parse(obj, resolver, res));
            final String blockId = RemoteBlocksHandler.parseBlockId(obj);

            batch.addAll(mSpeakersHandler.parse(obj, resolver, res));
            final String speakerId = RemoteSpeakersHandler.parseSpeakerId(obj);

            if ( !isBreak
                && ( roomId != 0 || obj.has(Tags.SESSION_ABSTRACT) || !TextUtils.isEmpty(speakerId) ) ) {

                batch.addAll(parseSession(obj, oldValues, resolver, res, now,
                    blockId));

                if ( trackId != INVALID_TRACK_ID ) {
                    // Assign tracks
                    final Uri sessionTracksUri = Sessions.buildTracksDirUri(sessionId);
                    batch.add(ContentProviderOperation.newInsert(
                        sessionTracksUri)
                        .withValue(SessionsTracks.SESSION_ID, sessionId)
                        .withValue(SessionsTracks.TRACK_ID,
                            Integer.toString(trackId))
                        .withValue(CoscupProvider.EXTRA_UNNOTIFY, Boolean.TRUE)
                        .build());
                }

                if ( !TextUtils.isEmpty(speakerId) ) {
                    // Assign speakers
                    final Uri sessionSpeakersUri = Sessions.buildSpeakersDirUri(sessionId);
                    batch.add(ContentProviderOperation.newInsert(
                        sessionSpeakersUri)
                        .withValue(SessionsSpeakers.SESSION_ID, sessionId)
                        .withValue(SessionsSpeakers.SPEAKER_ID, speakerId)
                        .withValue(CoscupProvider.EXTRA_UNNOTIFY, Boolean.TRUE)
                        .build());
                }

            }

        }

        // Delete outdated sessions
        batch.add(ContentProviderOperation.newDelete(
            Sessions.buildSessionsUnsyncedUri(now))
            .build());
        batch.add(ContentProviderOperation.newDelete(
            Blocks.CONTENT_UNSYNCED_URI)
            .build());

        return batch;
    }

    private static String parseSessionId(JSONObject json) throws JSONException {
        String sessionId = json.getString(Tags.SESSION_ID);
        if ( TextUtils.isEmpty(sessionId) ) {
            sessionId = ParserUtils.sanitizeId(json.getString(Tags.SESSION_TITLE));
        }
        return sessionId;
    }

    private static ArrayList<ContentProviderOperation> parseSession(
        JSONObject json, ContentValues oldValues, ContentResolver resolver,
        Resources res, long syncTime, String blockId) throws JSONException,
        IOException {
        final ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        final String sessionId = parseSessionId(json);

        // WARN: Not really necessary as a conflict replacement and a trigger
        // should be invoked
        /*
         * final Uri sessionUri = Sessions.buildSessionUri(sessionId);
         * batch.add(ContentProviderOperation.newDelete(sessionUri) .build());
         * final Uri sessionTracksUri = Sessions.buildTracksDirUri(sessionId);
         * final Uri sessionSpeakersUri =
         * Sessions.buildSpeakersDirUri(sessionId);
         * batch.add(ContentProviderOperation.newDelete(sessionTracksUri)
         * .build());
         * batch.add(ContentProviderOperation.newDelete(sessionSpeakersUri)
         * .build());
         */

        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Sessions.CONTENT_URI);

        builder.withValue(SyncColumns.UPDATED, syncTime);
        builder.withValue(Sessions.SESSION_ID, sessionId);
        builder.withValue(Sessions.SESSION_TITLE,
            json.getString(Tags.SESSION_TITLE));
        builder.withValue(Sessions.SESSION_ABSTRACT,
            json.optString(Tags.SESSION_ABSTRACT));
        builder.withValue(Sessions.SESSION_KEYWORDS, "");
        builder.withValue(Sessions.SESSION_HASHTAG, "");
        builder.withValue(Sessions.SESSION_URL, "");
        builder.withValue(Sessions.SESSION_MODERATOR_URL, "");
        builder.withValue(Sessions.SESSION_YOUTUBE_URL, "");
        builder.withValue(Sessions.SESSION_PDF_URL, "");

        // Inherit starred value from previous row
        if ( oldValues.containsKey(Sessions.SESSION_STARRED) ) {
            builder.withValue(Sessions.SESSION_STARRED,
                oldValues.getAsInteger(Sessions.SESSION_STARRED));
        }

        builder.withValue(Sessions.BLOCK_ID, blockId);

        // Assign room
        final String roomId = Integer.toString(json.getInt(Tags.SESSION_ROOM));
        builder.withValue(Sessions.ROOM_ID, roomId);

        // Normal session details ready, write to provider
        builder.withValue(CoscupProvider.EXTRA_UNNOTIFY, Boolean.TRUE);
        batch.add(builder.build());

        return batch;
    }

    private static ContentValues querySessionDetails(Uri uri,
        ContentResolver resolver) {
        final ContentValues values = new ContentValues();
        final Cursor cursor = resolver.query(uri, SessionsQuery.PROJECTION,
            null, null, null);
        try {
            if ( cursor.moveToFirst() ) {
                values.put(SyncColumns.UPDATED,
                    cursor.getLong(SessionsQuery.UPDATED));
                values.put(Sessions.SESSION_STARRED,
                    cursor.getInt(SessionsQuery.STARRED));
            } else {
                values.put(SyncColumns.UPDATED, CoscupContract.UPDATED_NEVER);
            }
        } finally {
            cursor.close();
        }
        return values;
    }

    private interface SessionsQuery {
        String[] PROJECTION = { SyncColumns.UPDATED, Sessions.SESSION_STARRED, };

        int UPDATED = 0;
        int STARRED = 1;
    }

    /** Tags coming from remote JSON. */
    private interface Tags {
        String SESSION_ID = "id";
        String SESSION_ROOM = "room";
        String SESSION_TRACK = "type";
        String SESSION_TITLE = "name";
        String SESSION_ABSTRACT = "abstract";
        String SESSION_SPEAKERS = "speaker";
        String SESSION_IS_BREAK = "isBreak";
        String SESSION_URL = "link";
        String SESSION_MODERATOR_URL = "question";
        String SESSION_YOUTUBE_URL = "video";
        String SESSION_PDF_URL = "pdf";
    }

}
