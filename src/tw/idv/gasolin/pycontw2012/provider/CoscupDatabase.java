package tw.idv.gasolin.pycontw2012.provider;

import android.app.SearchManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Blocks;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.BlocksColumns;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Rooms;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.RoomsColumns;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Sessions;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.SessionsColumns;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Speakers;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.SpeakersColumns;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.SponsorsColumns;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.SyncColumns;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Tracks;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.TracksColumns;

public class CoscupDatabase extends SQLiteOpenHelper {

    private static final String LOG_TAG = CoscupDatabase.class.getName();

    private static final String DATABASE_NAME = "sched.db";

    private static final int VER_LAUNCH = 1;
    private static final int VER_RECURSIVE_TRIGGERS = 2;

    private static final int DATABASE_VERSION = VER_RECURSIVE_TRIGGERS;

    interface Tables {
        String BLOCKS = "blocks";
        String TRACKS = "tracks";
        String ROOMS = "rooms";
        String SESSIONS = "sessions";
        String SPEAKERS = "speakers";
        String SESSIONS_SPEAKERS = "sessions_speakers";
        String SESSIONS_TRACKS = "sessions_tracks";
        String SPONSORS = "sponsors";

        String SESSIONS_SEARCH = "sessions_search";

        String SEARCH_SUGGEST = "search_suggest";

        String SESSIONS_JOIN_BLOCKS_ROOMS = "sessions "
            + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
            + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

        String SESSIONS_SPEAKERS_JOIN_SPEAKERS = "sessions_speakers "
            + "LEFT OUTER JOIN speakers ON sessions_speakers.speaker_id=speakers.speaker_id";

        String SESSIONS_SPEAKERS_JOIN_SESSIONS_BLOCKS_ROOMS = "sessions_speakers "
            + "LEFT OUTER JOIN sessions ON sessions_speakers.session_id=sessions.session_id "
            + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
            + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

        String SESSIONS_TRACKS_JOIN_TRACKS = "sessions_tracks "
            + "LEFT OUTER JOIN tracks ON sessions_tracks.track_id=tracks.track_id";

        String SESSIONS_TRACKS_JOIN_SESSIONS_BLOCKS_ROOMS = "sessions_tracks "
            + "LEFT OUTER JOIN sessions ON sessions_tracks.session_id=sessions.session_id "
            + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
            + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";

        String SESSIONS_SEARCH_JOIN_SESSIONS_BLOCKS_ROOMS = "sessions_search "
            + "LEFT OUTER JOIN sessions ON sessions_search.session_id=sessions.session_id "
            + "LEFT OUTER JOIN blocks ON sessions.block_id=blocks.block_id "
            + "LEFT OUTER JOIN rooms ON sessions.room_id=rooms.room_id";
    }

    private interface Triggers {
        String SESSIONS_SEARCH_INSERT = "sessions_search_insert";
        String SESSIONS_SEARCH_DELETE = "sessions_search_delete";
        String SESSIONS_SEARCH_UPDATE = "sessions_search_update";
        String SESSION_DELETE = "session_delete";
    }

    public interface SessionsSpeakers {
        String SESSION_ID = "session_id";
        String SPEAKER_ID = "speaker_id";
    }

    public interface SessionsTracks {
        String SESSION_ID = "session_id";
        String TRACK_ID = "track_id";
    }

    interface SessionsSearchColumns {
        String SESSION_ID = "session_id";
        String BODY = "body";
    }

    /** Fully-qualified field names. */
    private interface Qualified {
        String SESSIONS_SEARCH_SESSION_ID = Tables.SESSIONS_SEARCH + "."
            + SessionsSearchColumns.SESSION_ID;

        String SESSIONS_SEARCH = Tables.SESSIONS_SEARCH + "("
            + SessionsSearchColumns.SESSION_ID + ","
            + SessionsSearchColumns.BODY + ")";

        String SESSIONS_SPEAKERS_SESSION_ID = Tables.SESSIONS_SPEAKERS + "."
            + SessionsSpeakers.SESSION_ID;

        String SESSIONS_TRACKS_SESSION_ID = Tables.SESSIONS_TRACKS + "."
            + SessionsTracks.SESSION_ID;
    }

    /** {@code REFERENCES} clauses. */
    private interface References {
        String BLOCK_ID = "REFERENCES " + Tables.BLOCKS + "(" + Blocks.BLOCK_ID
            + ")";
        String TRACK_ID = "REFERENCES " + Tables.TRACKS + "(" + Tracks.TRACK_ID
            + ")";
        String ROOM_ID = "REFERENCES " + Tables.ROOMS + "(" + Rooms.ROOM_ID
            + ")";
        String SESSION_ID = "REFERENCES " + Tables.SESSIONS + "("
            + Sessions.SESSION_ID + ")";
        String SPEAKER_ID = "REFERENCES " + Tables.SPEAKERS + "("
            + Speakers.SPEAKER_ID + ")";
    }

    private interface Subquery {
        /**
         * Subquery used to build the {@link SessionsSearchColumns#BODY} string
         * used for indexing {@link Sessions} content.
         */
        String SESSIONS_BODY = "(new." + Sessions.SESSION_TITLE
            + "||'; '||new." + Sessions.SESSION_ABSTRACT + "||'; '||"
            + "coalesce(new." + Sessions.SESSION_KEYWORDS + ", '')" + ")";
    }

    public CoscupDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA recursive_triggers=TRUE");
        
        db.execSQL("CREATE TABLE " + Tables.BLOCKS + " (" + BaseColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + BlocksColumns.BLOCK_ID
            + " TEXT NOT NULL," + BlocksColumns.BLOCK_TITLE + " TEXT NOT NULL,"
            + BlocksColumns.BLOCK_START + " INTEGER NOT NULL,"
            + BlocksColumns.BLOCK_END + " INTEGER NOT NULL,"
            + BlocksColumns.BLOCK_TYPE + " TEXT," + "UNIQUE ("
            + BlocksColumns.BLOCK_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.TRACKS + " (" + BaseColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + TracksColumns.TRACK_ID
            + " TEXT NOT NULL," + TracksColumns.TRACK_NAME + " TEXT,"
            + TracksColumns.TRACK_COLOR + " INTEGER,"
            + TracksColumns.TRACK_ABSTRACT + " TEXT," + "UNIQUE ("
            + TracksColumns.TRACK_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.ROOMS + " (" + BaseColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + RoomsColumns.ROOM_ID
            + " TEXT NOT NULL," + RoomsColumns.ROOM_NAME + " TEXT,"
            + RoomsColumns.ROOM_FLOOR + " TEXT," + RoomsColumns.ROOM_NAME_ZH_TW
            + " TEXT," + RoomsColumns.ROOM_NAME_ZH_CN + " TEXT," + "UNIQUE ("
            + RoomsColumns.ROOM_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS + " (" + BaseColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + SyncColumns.UPDATED
            + " INTEGER NOT NULL," + SessionsColumns.SESSION_ID
            + " TEXT NOT NULL," + Sessions.BLOCK_ID + " TEXT "
            + References.BLOCK_ID + "," + Sessions.ROOM_ID + " TEXT "
            + References.ROOM_ID + "," + SessionsColumns.SESSION_LANGUAGE
            + " TEXT," + SessionsColumns.SESSION_TITLE + " TEXT,"
            + SessionsColumns.SESSION_ABSTRACT + " TEXT,"
            + SessionsColumns.SESSION_KEYWORDS + " TEXT,"
            + SessionsColumns.SESSION_HASHTAG + " TEXT,"
            + SessionsColumns.SESSION_URL + " TEXT,"
            + SessionsColumns.SESSION_MODERATOR_URL + " TEXT,"
            + SessionsColumns.SESSION_PDF_URL + " TEXT,"
            + SessionsColumns.SESSION_YOUTUBE_URL + " TEXT,"
            + SessionsColumns.SESSION_STARRED + " INTEGER NOT NULL DEFAULT 0,"
            + "UNIQUE (" + SessionsColumns.SESSION_ID
            + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SPEAKERS + " (" + BaseColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + SyncColumns.UPDATED
            + " INTEGER NOT NULL," + SpeakersColumns.SPEAKER_ID
            + " TEXT NOT NULL," + SpeakersColumns.SPEAKER_NAME + " TEXT,"
            + SpeakersColumns.SPEAKER_IMAGE_URL + " TEXT,"
            + SpeakersColumns.SPEAKER_COMPANY + " TEXT,"
            + SpeakersColumns.SPEAKER_ABSTRACT + " TEXT,"
            + SpeakersColumns.SPEAKER_URL + " TEXT," + "UNIQUE ("
            + SpeakersColumns.SPEAKER_ID + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS_SPEAKERS + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + SessionsSpeakers.SESSION_ID + " TEXT NOT NULL "
            + References.SESSION_ID + "," + SessionsSpeakers.SPEAKER_ID
            + " TEXT NOT NULL " + References.SPEAKER_ID + "," + "UNIQUE ("
            + SessionsSpeakers.SESSION_ID + "," + SessionsSpeakers.SPEAKER_ID
            + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SESSIONS_TRACKS + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + SessionsTracks.SESSION_ID + " TEXT NOT NULL "
            + References.SESSION_ID + "," + SessionsTracks.TRACK_ID
            + " TEXT NOT NULL " + References.TRACK_ID + "," + "UNIQUE ("
            + SessionsTracks.SESSION_ID + "," + SessionsTracks.TRACK_ID
            + ") ON CONFLICT REPLACE)");

        db.execSQL("CREATE TABLE " + Tables.SPONSORS + " (" + BaseColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + SyncColumns.UPDATED
            + " INTEGER NOT NULL," + SponsorsColumns.SPONSOR_ID
            + " TEXT NOT NULL," + SponsorsColumns.SPONSOR_LEVEL + " INTEGER,"
            + SponsorsColumns.SPONSOR_NAME + " TEXT,"
            + SponsorsColumns.SPONSOR_NAME_ZH_TW + " TEXT,"
            + SponsorsColumns.SPONSOR_NAME_ZH_CN + " TEXT,"
            + SponsorsColumns.SPONSOR_URL + " TEXT,"
            + SponsorsColumns.SPONSOR_LOGO_URL + " TEXT,"
            + SponsorsColumns.SPONSOR_DESC + " TEXT,"
            + SponsorsColumns.SPONSOR_DESC_ZH_TW + " TEXT,"
            + SponsorsColumns.SPONSOR_DESC_ZH_CN + " TEXT," + "UNIQUE ("
            + SponsorsColumns.SPONSOR_ID + ") ON CONFLICT REPLACE)");

        createSessionsSearch(db);

        db.execSQL("CREATE TABLE " + Tables.SEARCH_SUGGEST + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + SearchManager.SUGGEST_COLUMN_TEXT_1 + " TEXT NOT NULL)");
    }

    /**
     * Create triggers that automatically build {@link Tables#SESSIONS_SEARCH}
     * as values are changed in {@link Tables#SESSIONS}.
     */
    private static void createSessionsSearch(SQLiteDatabase db) {
        // Using the "porter" tokenizer for simple stemming, so that
        // "frustration" matches "frustrated."

        db.execSQL("CREATE VIRTUAL TABLE " + Tables.SESSIONS_SEARCH
            + " USING fts3(" + BaseColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + SessionsSearchColumns.BODY + " TEXT NOT NULL,"
            + SessionsSearchColumns.SESSION_ID + " TEXT NOT NULL "
            + References.SESSION_ID + "," + "UNIQUE ("
            + SessionsSearchColumns.SESSION_ID + ") ON CONFLICT REPLACE,"
            + "tokenize=porter)");

        // TODO: handle null fields in body, which cause trigger to fail
        // TODO: implement update trigger, not currently exercised

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_INSERT
            + " AFTER INSERT ON " + Tables.SESSIONS + " BEGIN INSERT INTO "
            + Qualified.SESSIONS_SEARCH + " " + " VALUES(new."
            + Sessions.SESSION_ID + ", " + Subquery.SESSIONS_BODY + ");"
            + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_DELETE
            + " AFTER DELETE ON " + Tables.SESSIONS + " BEGIN DELETE FROM "
            + Tables.SESSIONS_SEARCH + " " + " WHERE "
            + Qualified.SESSIONS_SEARCH_SESSION_ID + "=old."
            + Sessions.SESSION_ID + ";" + " END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSIONS_SEARCH_UPDATE
            + " AFTER UPDATE ON " + Tables.SESSIONS
            + " BEGIN UPDATE sessions_search SET " + SessionsSearchColumns.BODY
            + " = " + Subquery.SESSIONS_BODY
            + " WHERE session_id = old.session_id" + "; END;");

        db.execSQL("CREATE TRIGGER " + Triggers.SESSION_DELETE
            + " AFTER DELETE ON " + Tables.SESSIONS + " BEGIN DELETE FROM "
            + Tables.SESSIONS_SPEAKERS + " " + " WHERE "
            + Qualified.SESSIONS_SPEAKERS_SESSION_ID + "=old."
            + Sessions.SESSION_ID + "; " + "DELETE FROM "
            + Tables.SESSIONS_TRACKS + " " + " WHERE "
            + Qualified.SESSIONS_TRACKS_SESSION_ID + "=old."
            + Sessions.SESSION_ID + "; " + " END;");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "onUpgrade() from " + oldVersion + " to " + newVersion);

        // NOTE: This switch statement is designed to handle cascading database
        // updates, starting at the current version and falling through to all
        // future upgrade cases. Only use "break;" when you want to drop and
        // recreate the entire database.
        int version = oldVersion;
        
        switch ( version ) {
        case VER_LAUNCH: {
            // VER_RECURSIVE_TRIGGERS sets recursive_triggers pragma to true
            db.execSQL("PRAGMA recursive_triggers=TRUE");
            version = VER_RECURSIVE_TRIGGERS;
        }
        }

        Log.d(LOG_TAG, "after upgrade logic, at version " + version);
        if ( version != DATABASE_VERSION ) {
            Log.w(LOG_TAG, "Destroying old data during upgrade");

            db.execSQL("DROP TABLE IF EXISTS " + Tables.BLOCKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.TRACKS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.ROOMS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SPEAKERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SPEAKERS);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_TRACKS);

            db.execSQL("DROP TRIGGER IF EXISTS "
                + Triggers.SESSIONS_SEARCH_INSERT);
            db.execSQL("DROP TRIGGER IF EXISTS "
                + Triggers.SESSIONS_SEARCH_DELETE);
            db.execSQL("DROP TABLE IF EXISTS " + Tables.SESSIONS_SEARCH);

            db.execSQL("DROP TABLE IF EXISTS " + Tables.SEARCH_SUGGEST);

            onCreate(db);
        }
    }

}
