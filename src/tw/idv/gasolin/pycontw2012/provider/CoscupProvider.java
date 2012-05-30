package tw.idv.gasolin.pycontw2012.provider;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.util.Log;

import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Blocks;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Rooms;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.SearchSuggest;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Sessions;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Speakers;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Sponsors;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.SyncColumns;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Tracks;
import tw.idv.gasolin.pycontw2012.provider.CoscupDatabase.SessionsSearchColumns;
import tw.idv.gasolin.pycontw2012.provider.CoscupDatabase.SessionsSpeakers;
import tw.idv.gasolin.pycontw2012.provider.CoscupDatabase.SessionsTracks;
import tw.idv.gasolin.pycontw2012.provider.CoscupDatabase.Tables;
import tw.idv.gasolin.pycontw2012.util.ParserUtils;
import tw.idv.gasolin.pycontw2012.util.SelectionBuilder;

public class CoscupProvider extends ContentProvider {

    private static final String TAG = CoscupProvider.class.getSimpleName();

    public static final String EXTRA_UNNOTIFY = "unnotify";

    private CoscupDatabase mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int BLOCKS = 100;
    private static final int BLOCKS_BETWEEN = 101;
    private static final int BLOCKS_ID = 102;
    private static final int BLOCKS_ID_SESSIONS = 103;
    private static final int BLOCKS_UNSYNCED = 104;

    private static final int TRACKS = 200;
    private static final int TRACKS_ID = 201;
    private static final int TRACKS_ID_SESSIONS = 202;

    private static final int ROOMS = 300;
    private static final int ROOMS_ID = 301;
    private static final int ROOMS_ID_SESSIONS = 302;

    private static final int SESSIONS = 400;
    private static final int SESSIONS_STARRED = 401;
    private static final int SESSIONS_SEARCH = 402;
    private static final int SESSIONS_AT = 403;
    private static final int SESSIONS_ID = 404;
    private static final int SESSIONS_ID_SPEAKERS = 405;
    private static final int SESSIONS_ID_TRACKS = 406;
    private static final int SESSIONS_UNSYNCED = 407;

    private static final int SPEAKERS = 500;
    private static final int SPEAKERS_ID = 501;
    private static final int SPEAKERS_ID_SESSIONS = 502;

    private static final int SPONSORS = 600;
    private static final int SPONSORS_ID = 601;
    private static final int SPONSORS_LEVEL_SPONSORS = 602;

    private static final int SEARCH_SUGGEST = 800;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = CoscupContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "blocks", BLOCKS);
        matcher.addURI(authority, "blocks/unsynced", BLOCKS_UNSYNCED);
        matcher.addURI(authority, "blocks/between/*/*", BLOCKS_BETWEEN);
        matcher.addURI(authority, "blocks/*", BLOCKS_ID);
        matcher.addURI(authority, "blocks/*/sessions", BLOCKS_ID_SESSIONS);

        matcher.addURI(authority, "tracks", TRACKS);
        matcher.addURI(authority, "tracks/*", TRACKS_ID);
        matcher.addURI(authority, "tracks/*/sessions", TRACKS_ID_SESSIONS);

        matcher.addURI(authority, "rooms", ROOMS);
        matcher.addURI(authority, "rooms/*", ROOMS_ID);
        matcher.addURI(authority, "rooms/*/sessions", ROOMS_ID_SESSIONS);

        matcher.addURI(authority, "sessions", SESSIONS);
        matcher.addURI(authority, "sessions/starred", SESSIONS_STARRED);
        matcher.addURI(authority, "sessions/search/*", SESSIONS_SEARCH);
        matcher.addURI(authority, "sessions/at/*", SESSIONS_AT);
        matcher.addURI(authority, "sessions/unsynced/*", SESSIONS_UNSYNCED);
        matcher.addURI(authority, "sessions/*", SESSIONS_ID);
        matcher.addURI(authority, "sessions/*/speakers", SESSIONS_ID_SPEAKERS);
        matcher.addURI(authority, "sessions/*/tracks", SESSIONS_ID_TRACKS);

        matcher.addURI(authority, "speakers", SPEAKERS);
        matcher.addURI(authority, "speakers/*", SPEAKERS_ID);
        matcher.addURI(authority, "speakers/*/sessions", SPEAKERS_ID_SESSIONS);

        matcher.addURI(authority, "sponsors", SPONSORS);
        matcher.addURI(authority, "sponsors/*", SPONSORS_ID);
        matcher.addURI(authority, "sponsors/*/sponsors",
            SPONSORS_LEVEL_SPONSORS);

        matcher.addURI(authority, "search_suggest_query", SEARCH_SUGGEST);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate");
        final Context context = getContext();
        mOpenHelper = new CoscupDatabase(context);

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.execSQL("PRAGMA recursive_triggers=TRUE");

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch ( match ) {
        case BLOCKS:
            return Blocks.CONTENT_TYPE;
        case BLOCKS_BETWEEN:
            return Blocks.CONTENT_TYPE;
        case BLOCKS_ID:
            return Blocks.CONTENT_ITEM_TYPE;
        case BLOCKS_ID_SESSIONS:
            return Sessions.CONTENT_TYPE;
        case TRACKS:
            return Tracks.CONTENT_TYPE;
        case TRACKS_ID:
            return Tracks.CONTENT_ITEM_TYPE;
        case TRACKS_ID_SESSIONS:
            return Sessions.CONTENT_TYPE;
        case ROOMS:
            return Rooms.CONTENT_TYPE;
        case ROOMS_ID:
            return Rooms.CONTENT_ITEM_TYPE;
        case ROOMS_ID_SESSIONS:
            return Sessions.CONTENT_TYPE;
        case SESSIONS:
            return Sessions.CONTENT_TYPE;
        case SESSIONS_STARRED:
            return Sessions.CONTENT_TYPE;
        case SESSIONS_SEARCH:
            return Sessions.CONTENT_TYPE;
        case SESSIONS_AT:
            return Sessions.CONTENT_TYPE;
        case SESSIONS_ID:
            return Sessions.CONTENT_ITEM_TYPE;
        case SESSIONS_ID_SPEAKERS:
            return Speakers.CONTENT_TYPE;
        case SESSIONS_ID_TRACKS:
            return Tracks.CONTENT_TYPE;
        case SPEAKERS:
            return Speakers.CONTENT_TYPE;
        case SPEAKERS_ID:
            return Speakers.CONTENT_ITEM_TYPE;
        case SPEAKERS_ID_SESSIONS:
            return Sessions.CONTENT_TYPE;
        case SPONSORS_ID:
            return Sponsors.CONTENT_ITEM_TYPE;
        case SPONSORS:
        case SPONSORS_LEVEL_SPONSORS:
            return Sponsors.CONTENT_TYPE;
        default:
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
        String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        final int match = sUriMatcher.match(uri);
        Log.v(TAG, "query(uri=" + uri + ", match=" + match + ", proj="
            + Arrays.toString(projection) + ")");
        switch ( match ) {
        default: {
            // Most cases are handled with simple SelectionBuilder
            final SelectionBuilder builder = buildExpandedSelection(uri, match);
            return builder.where(selection, selectionArgs)
                .query(db, projection, sortOrder);
        }
        case SEARCH_SUGGEST: {
            final SelectionBuilder builder = new SelectionBuilder();

            // Adjust incoming query to become SQL text match
            selectionArgs[0] = selectionArgs[0] + "%";
            builder.table(Tables.SEARCH_SUGGEST);
            builder.where(selection, selectionArgs);
            builder.map(SearchManager.SUGGEST_COLUMN_QUERY,
                SearchManager.SUGGEST_COLUMN_TEXT_1);

            projection = new String[] { BaseColumns._ID,
                SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_QUERY };

            final String limit = uri.getQueryParameter(SearchManager.SUGGEST_PARAMETER_LIMIT);
            return builder.query(db, projection, null, null,
                SearchSuggest.DEFAULT_SORT, limit);
        }
        }
    }

    /** {@inheritDoc} */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final boolean unnotify = ( values == null || !values.containsKey(EXTRA_UNNOTIFY) )
            ? false : values.getAsBoolean(EXTRA_UNNOTIFY);
        if ( values != null ) {
            values.remove(EXTRA_UNNOTIFY);
        }
        final int match = sUriMatcher.match(uri);
        Log.v(TAG, "insert(uri=" + uri + ", match=" + match + ", unnotify="
            + unnotify + ", values=" + values.toString() + ")");
        switch ( match ) {
        case BLOCKS: {
            db.insertOrThrow(Tables.BLOCKS, null, values);
            if ( !unnotify ) {
                getContext().getContentResolver()
                    .notifyChange(uri, null);
            }
            return Blocks.buildBlockUri(values.getAsString(Blocks.BLOCK_ID));
        }
        case TRACKS: {
            db.insertOrThrow(Tables.TRACKS, null, values);
            if ( !unnotify ) {
                getContext().getContentResolver()
                    .notifyChange(uri, null);
            }
            return Tracks.buildTrackUri(values.getAsString(Tracks.TRACK_ID));
        }
        case ROOMS: {
            db.insertOrThrow(Tables.ROOMS, null, values);
            if ( !unnotify ) {
                getContext().getContentResolver()
                    .notifyChange(uri, null);
            }
            return Rooms.buildRoomUri(values.getAsString(Rooms.ROOM_ID));
        }
        case SESSIONS: {
            db.insertOrThrow(Tables.SESSIONS, null, values);
            if ( !unnotify ) {
                getContext().getContentResolver()
                    .notifyChange(uri, null);
            }
            return Sessions.buildSessionUri(values.getAsString(Sessions.SESSION_ID));
        }
        case SESSIONS_ID_SPEAKERS: {
            db.insertOrThrow(Tables.SESSIONS_SPEAKERS, null, values);
            if ( !unnotify ) {
                getContext().getContentResolver()
                    .notifyChange(uri, null);
            }
            return Speakers.buildSpeakerUri(values.getAsString(SessionsSpeakers.SPEAKER_ID));
        }
        case SESSIONS_ID_TRACKS: {
            db.insertOrThrow(Tables.SESSIONS_TRACKS, null, values);
            if ( !unnotify ) {
                getContext().getContentResolver()
                    .notifyChange(uri, null);
            }
            return Tracks.buildTrackUri(values.getAsString(SessionsTracks.TRACK_ID));
        }
        case SPEAKERS: {
            db.insertOrThrow(Tables.SPEAKERS, null, values);
            if ( !unnotify ) {
                getContext().getContentResolver()
                    .notifyChange(uri, null);
            }
            return Speakers.buildSpeakerUri(values.getAsString(Speakers.SPEAKER_ID));
        }
        case SPONSORS: {
            db.insertOrThrow(Tables.SPONSORS, null, values);
            if ( !unnotify ) {
                getContext().getContentResolver()
                    .notifyChange(uri, null);
            }
            return Sponsors.buildSponsorUri(values.getAsString(Sponsors.SPONSOR_ID));
        }
        case SEARCH_SUGGEST: {
            db.insertOrThrow(Tables.SEARCH_SUGGEST, null, values);
            if ( !unnotify ) {
                getContext().getContentResolver()
                    .notifyChange(uri, null);
            }
            return SearchSuggest.CONTENT_URI;
        }
        default: {
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int update(Uri uri, ContentValues values, String selection,
        String[] selectionArgs) {
        final boolean unnotify = ( values == null || !values.containsKey(EXTRA_UNNOTIFY) )
            ? false : values.getAsBoolean(EXTRA_UNNOTIFY);
        Log.v(TAG, "update(uri=" + uri + ", unnotify=" + unnotify + ", values="
            + values.toString() + ")");
        if ( values != null ) {
            values.remove(EXTRA_UNNOTIFY);
        }
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs)
            .update(db, values);
        if ( !unnotify ) {
            getContext().getContentResolver()
                .notifyChange(uri, null);
        }
        return retVal;
    }

    /** {@inheritDoc} */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.v(TAG, "delete(uri=" + uri + ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final SelectionBuilder builder = buildSimpleSelection(uri);
        int retVal = builder.where(selection, selectionArgs)
            .delete(db);
        getContext().getContentResolver()
            .notifyChange(uri, null);
        return retVal;
    }

    /**
     * Apply the given set of {@link ContentProviderOperation}, executing inside
     * a {@link SQLiteDatabase} transaction. All changes will be rolled back if
     * any single one fails.
     */
    @Override
    public ContentProviderResult[] applyBatch(
        ArrayList<ContentProviderOperation> operations)
        throws OperationApplicationException {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            final int numOperations = operations.size();
            final ContentProviderResult[] results = new ContentProviderResult[numOperations];
            for ( int i = 0; i < numOperations; i++ ) {
                results[i] = operations.get(i)
                    .apply(this, results, i);
            }
            db.setTransactionSuccessful();
            return results;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Build a simple {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually enough to support {@link #insert},
     * {@link #update}, and {@link #delete} operations.
     */
    private SelectionBuilder buildSimpleSelection(Uri uri) {
        final SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        Log.d(TAG, "buildSimpleSelection: " + match + ", " + uri);
        switch ( match ) {
        case BLOCKS: {
            return builder.table(Tables.BLOCKS);
        }
        case BLOCKS_ID: {
            final String blockId = Blocks.getBlockId(uri);
            return builder.table(Tables.BLOCKS)
                .where(Blocks.BLOCK_ID + "=?", blockId);
        }
        case BLOCKS_UNSYNCED: {
            return builder.table(Tables.BLOCKS)
                .where(Blocks.BLOCK_TYPE + "=?", ParserUtils.BLOCK_TYPE_SESSION)
                .where(Subquery.BLOCK_SESSIONS_COUNT + "=0");
        }
        case TRACKS: {
            return builder.table(Tables.TRACKS);
        }
        case TRACKS_ID: {
            final String trackId = Tracks.getTrackId(uri);
            return builder.table(Tables.TRACKS)
                .where(Tracks.TRACK_ID + "=?", trackId);
        }
        case ROOMS: {
            return builder.table(Tables.ROOMS);
        }
        case ROOMS_ID: {
            final String roomId = Rooms.getRoomId(uri);
            return builder.table(Tables.ROOMS)
                .where(Rooms.ROOM_ID + "=?", roomId);
        }
        case SESSIONS: {
            return builder.table(Tables.SESSIONS);
        }
        case SESSIONS_ID: {
            final String sessionId = Sessions.getSessionId(uri);
            return builder.table(Tables.SESSIONS)
                .where(Sessions.SESSION_ID + "=?", sessionId);
        }
        case SESSIONS_ID_SPEAKERS: {
            final String sessionId = Sessions.getSessionId(uri);
            return builder.table(Tables.SESSIONS_SPEAKERS)
                .where(Sessions.SESSION_ID + "=?", sessionId);
        }
        case SESSIONS_ID_TRACKS: {
            final String sessionId = Sessions.getSessionId(uri);
            return builder.table(Tables.SESSIONS_TRACKS)
                .where(Sessions.SESSION_ID + "=?", sessionId);
        }
        case SESSIONS_UNSYNCED: {
            return builder.table(Tables.SESSIONS)
                .where(SyncColumns.UPDATED + "!=?", uri.getLastPathSegment());
        }
        case SPEAKERS: {
            return builder.table(Tables.SPEAKERS);
        }
        case SPEAKERS_ID: {
            final String speakerId = Speakers.getSpeakerId(uri);
            return builder.table(Tables.SPEAKERS)
                .where(Speakers.SPEAKER_ID + "=?", speakerId);
        }
        case SPONSORS: {
            return builder.table(Tables.SPONSORS);
        }
        case SPONSORS_ID: {
            final String sponsorId = Sponsors.getSponsorId(uri);
            return builder.table(Tables.SPONSORS)
                .where(Sponsors.SPONSOR_ID + "=?", sponsorId);
        }
        case SPONSORS_LEVEL_SPONSORS: {
            final String level = Sponsors.getLevel(uri);
            return builder.table(Tables.SPONSORS)
                .where(Sponsors.SPONSOR_LEVEL + "=?", level);
        }
        case SEARCH_SUGGEST: {
            return builder.table(Tables.SEARCH_SUGGEST);
        }
        default: {
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        }
    }

    /**
     * Build an advanced {@link SelectionBuilder} to match the requested
     * {@link Uri}. This is usually only used by {@link #query}, since it
     * performs table joins useful for {@link Cursor} data.
     */
    private SelectionBuilder buildExpandedSelection(Uri uri, int match) {
        final SelectionBuilder builder = new SelectionBuilder();
        switch ( match ) {
        case BLOCKS: {
            return builder.table(Tables.BLOCKS);
        }
        case BLOCKS_BETWEEN: {
            final List<String> segments = uri.getPathSegments();
            final String startTime = segments.get(2);
            final String endTime = segments.get(3);
            return builder.table(Tables.BLOCKS)
                .map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
                .map(Blocks.CONTAINS_STARRED, Subquery.BLOCK_CONTAINS_STARRED)
                .where(Blocks.BLOCK_START + ">=?", startTime)
                .where(Blocks.BLOCK_START + "<=?", endTime);
        }
        case BLOCKS_ID: {
            final String blockId = Blocks.getBlockId(uri);
            return builder.table(Tables.BLOCKS)
                .map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
                .map(Blocks.CONTAINS_STARRED, Subquery.BLOCK_CONTAINS_STARRED)
                .where(Blocks.BLOCK_ID + "=?", blockId);
        }
        case BLOCKS_ID_SESSIONS: {
            final String blockId = Blocks.getBlockId(uri);
            return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                .map(Blocks.SESSIONS_COUNT, Subquery.BLOCK_SESSIONS_COUNT)
                .map(Blocks.CONTAINS_STARRED, Subquery.BLOCK_CONTAINS_STARRED)
                .mapToTable(Sessions._ID, Tables.SESSIONS)
                .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                .map(Sessions.ROOM_ID, Qualified.SESSIONS_ROOM_ID)
                .where(Qualified.SESSIONS_BLOCK_ID + "=?", blockId);
        }
        case TRACKS: {
            return builder.table(Tables.TRACKS)
                .map(Tracks.SESSIONS_COUNT, Subquery.TRACK_SESSIONS_COUNT);
        }
        case TRACKS_ID: {
            final String trackId = Tracks.getTrackId(uri);
            return builder.table(Tables.TRACKS)
                .where(Tracks.TRACK_ID + "=?", trackId);
        }
        case TRACKS_ID_SESSIONS: {
            final String trackId = Tracks.getTrackId(uri);
            return builder.table(
                Tables.SESSIONS_TRACKS_JOIN_SESSIONS_BLOCKS_ROOMS)
                .mapToTable(Sessions._ID, Tables.SESSIONS)
                .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                .map(Sessions.ROOM_ID, Qualified.SESSIONS_ROOM_ID)
                .where(Qualified.SESSIONS_TRACKS_TRACK_ID + "=?", trackId);
        }
        case ROOMS: {
            return builder.table(Tables.ROOMS);
        }
        case ROOMS_ID: {
            final String roomId = Rooms.getRoomId(uri);
            return builder.table(Tables.ROOMS)
                .where(Rooms.ROOM_ID + "=?", roomId);
        }
        case ROOMS_ID_SESSIONS: {
            final String roomId = Rooms.getRoomId(uri);
            return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                .mapToTable(Sessions._ID, Tables.SESSIONS)
                .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                .map(Sessions.ROOM_ID, Qualified.SESSIONS_ROOM_ID)
                .where(Qualified.SESSIONS_ROOM_ID + "=?", roomId);
        }
        case SESSIONS: {
            return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                .mapToTable(Sessions._ID, Tables.SESSIONS)
                .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                .map(Sessions.ROOM_ID, Qualified.SESSIONS_ROOM_ID);
        }
        case SESSIONS_STARRED: {
            return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                .mapToTable(Sessions._ID, Tables.SESSIONS)
                .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                .map(Sessions.ROOM_ID, Qualified.SESSIONS_ROOM_ID)
                .where(Sessions.SESSION_STARRED + "=1");
        }
        case SESSIONS_SEARCH: {
            final String query = Sessions.getSearchQuery(uri);
            return builder.table(
                Tables.SESSIONS_SEARCH_JOIN_SESSIONS_BLOCKS_ROOMS)
                .map(Sessions.SEARCH_SNIPPET, Subquery.SESSIONS_SNIPPET)
                .mapToTable(Sessions._ID, Tables.SESSIONS)
                .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                .where(SessionsSearchColumns.BODY + " MATCH ?", query);
        }
        case SESSIONS_AT: {
            final List<String> segments = uri.getPathSegments();
            final String time = segments.get(2);
            return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                .mapToTable(Sessions._ID, Tables.SESSIONS)
                .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                .map(Sessions.ROOM_ID, Qualified.SESSIONS_ROOM_ID)
                .where(Sessions.BLOCK_START + "<=?", time)
                .where(Sessions.BLOCK_END + ">=?", time);
        }
        case SESSIONS_ID: {
            final String sessionId = Sessions.getSessionId(uri);
            return builder.table(Tables.SESSIONS_JOIN_BLOCKS_ROOMS)
                .mapToTable(Sessions._ID, Tables.SESSIONS)
                .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                .map(Sessions.ROOM_ID, Qualified.SESSIONS_ROOM_ID)
                .where(Qualified.SESSIONS_SESSION_ID + "=?", sessionId);
        }
        case SESSIONS_ID_SPEAKERS: {
            final String sessionId = Sessions.getSessionId(uri);
            return builder.table(Tables.SESSIONS_SPEAKERS_JOIN_SPEAKERS)
                .mapToTable(Speakers._ID, Tables.SPEAKERS)
                .mapToTable(Speakers.SPEAKER_ID, Tables.SPEAKERS)
                .where(Qualified.SESSIONS_SPEAKERS_SESSION_ID + "=?", sessionId);
        }
        case SESSIONS_ID_TRACKS: {
            final String sessionId = Sessions.getSessionId(uri);
            return builder.table(Tables.SESSIONS_TRACKS_JOIN_TRACKS)
                .mapToTable(Tracks._ID, Tables.TRACKS)
                .mapToTable(Tracks.TRACK_ID, Tables.TRACKS)
                .where(Qualified.SESSIONS_TRACKS_SESSION_ID + "=?", sessionId);
        }
        case SESSIONS_UNSYNCED: {
            return builder.table(Tables.SESSIONS)
                .where(SyncColumns.UPDATED + "!=?", uri.getLastPathSegment());
        }
        case SPEAKERS: {
            return builder.table(Tables.SPEAKERS);
        }
        case SPEAKERS_ID: {
            final String speakerId = Speakers.getSpeakerId(uri);
            return builder.table(Tables.SPEAKERS)
                .where(Speakers.SPEAKER_ID + "=?", speakerId);
        }
        case SPEAKERS_ID_SESSIONS: {
            final String speakerId = Speakers.getSpeakerId(uri);
            return builder.table(
                Tables.SESSIONS_SPEAKERS_JOIN_SESSIONS_BLOCKS_ROOMS)
                .mapToTable(Sessions._ID, Tables.SESSIONS)
                .mapToTable(Sessions.SESSION_ID, Tables.SESSIONS)
                .mapToTable(Sessions.BLOCK_ID, Tables.SESSIONS)
                .mapToTable(Sessions.ROOM_ID, Tables.SESSIONS)
                .where(Qualified.SESSIONS_SPEAKERS_SPEAKER_ID + "=?", speakerId);
        }
        case SPONSORS: {
            return builder.table(Tables.SPONSORS);
        }
        case SPONSORS_ID: {
            final String sponsorId = Sponsors.getSponsorId(uri);
            return builder.table(Tables.SPONSORS)
                .where(Sponsors.SPONSOR_ID + "=?", sponsorId);
        }
        case SPONSORS_LEVEL_SPONSORS: {
            final String level = Sponsors.getLevel(uri);
            return builder.table(Tables.SPONSORS)
                .where(Sponsors.SPONSOR_LEVEL + "=?", level);
        }
        default: {
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
        throws FileNotFoundException {
        final int match = sUriMatcher.match(uri);
        switch ( match ) {
        default: {
            throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        }
    }

    private interface Subquery {
        String BLOCK_SESSIONS_COUNT = "(SELECT COUNT("
            + Qualified.SESSIONS_SESSION_ID + ") FROM " + Tables.SESSIONS
            + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
            + Qualified.BLOCKS_BLOCK_ID + ")";

        String BLOCK_CONTAINS_STARRED = "(SELECT MAX("
            + Qualified.SESSIONS_STARRED + ") FROM " + Tables.SESSIONS
            + " WHERE " + Qualified.SESSIONS_BLOCK_ID + "="
            + Qualified.BLOCKS_BLOCK_ID + ")";

        String TRACK_SESSIONS_COUNT = "(SELECT COUNT("
            + Qualified.SESSIONS_TRACKS_SESSION_ID + ") FROM "
            + Tables.SESSIONS_TRACKS + " WHERE "
            + Qualified.SESSIONS_TRACKS_TRACK_ID + "="
            + Qualified.TRACKS_TRACK_ID + ")";

        String SESSIONS_SNIPPET = "snippet(" + Tables.SESSIONS_SEARCH
            + ",'{','}','\u2026')";
    }

    /**
     * {@link ScheduleContract} fields that are fully qualified with a specific
     * parent {@link Tables}. Used when needed to work around SQL ambiguity.
     */
    private interface Qualified {
        String SESSIONS_SESSION_ID = Tables.SESSIONS + "."
            + Sessions.SESSION_ID;
        String SESSIONS_BLOCK_ID = Tables.SESSIONS + "." + Sessions.BLOCK_ID;
        String SESSIONS_ROOM_ID = Tables.SESSIONS + "." + Sessions.ROOM_ID;

        String SESSIONS_TRACKS_SESSION_ID = Tables.SESSIONS_TRACKS + "."
            + SessionsTracks.SESSION_ID;
        String SESSIONS_TRACKS_TRACK_ID = Tables.SESSIONS_TRACKS + "."
            + SessionsTracks.TRACK_ID;

        String SESSIONS_SPEAKERS_SESSION_ID = Tables.SESSIONS_SPEAKERS + "."
            + SessionsSpeakers.SESSION_ID;
        String SESSIONS_SPEAKERS_SPEAKER_ID = Tables.SESSIONS_SPEAKERS + "."
            + SessionsSpeakers.SPEAKER_ID;

        @SuppressWarnings("hiding")
        String SESSIONS_STARRED = Tables.SESSIONS + "."
            + Sessions.SESSION_STARRED;

        String TRACKS_TRACK_ID = Tables.TRACKS + "." + Tracks.TRACK_ID;
        String BLOCKS_BLOCK_ID = Tables.BLOCKS + "." + Blocks.BLOCK_ID;
    }

}
