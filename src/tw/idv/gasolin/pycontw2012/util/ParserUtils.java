/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tw.idv.gasolin.pycontw2012.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.json.JSONTokener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;

import tw.idv.gasolin.pycontw2012.io.XmlHandler;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Blocks;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.SyncColumns;

/**
 * Various utility methods used by {@link XmlHandler} implementations.
 */
public class ParserUtils {
    
    private static final String LOG_TAG = ParserUtils.class.getSimpleName();

    public static final String BLOCK_TITLE_BREAKOUT_SESSIONS = "Breakout sessions";
    public static final String BLOCK_TITLE_KEYWORD_KEYNOTE = "Keynote";

    public static final String BLOCK_TYPE_FOOD = "food";
    public static final String BLOCK_TYPE_SESSION = "session";

    /** Used to sanitize a string to be {@link Uri} safe. */
    private static final Pattern sSanitizePattern = Pattern.compile("[^a-z0-9-_]");
    private static final Pattern sParenPattern = Pattern.compile("\\(.*?\\)");

    /** Used to split a comma-separated string. */
    private static final Pattern sCommaPattern = Pattern.compile("\\s*,\\s*");

    private static Time sTime = new Time();
    private static XmlPullParserFactory sFactory;
    private static final int JSON_LENGTH_LIMIT = 650000;

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input) {
        return sanitizeId(input, false);
    }

    /**
     * Sanitize the given string to be {@link Uri} safe for building
     * {@link ContentProvider} paths.
     */
    public static String sanitizeId(String input, boolean stripParen) {
        if ( input == null )
            return null;
        if ( stripParen ) {
            // Strip out all parenthetical statements when requested.
            input = sParenPattern.matcher(input)
                .replaceAll("");
        }
        String encodedInput = input.toLowerCase();
        try {
            encodedInput = URLEncoder.encode(encodedInput, "UTF-8");
        } catch ( UnsupportedEncodingException e ) {
            Log.e(LOG_TAG, "sanitizeId failed!", e);
        }
        return sSanitizePattern.matcher(encodedInput)
            .replaceAll("");
    }

    /**
     * Split the given comma-separated string, returning all values.
     */
    public static String[] splitComma(CharSequence input) {
        if ( input == null )
            return new String[0];
        return sCommaPattern.split(input);
    }

    /**
     * Parse the given string as a RFC 3339 timestamp, returning the value as
     * milliseconds since the epoch.
     */
    public static long parseTime(String time) {
        sTime.parse3339(time);
        return sTime.toMillis(false);
    }

    public static JSONTokener newJsonTokener(InputStream input)
        throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048]; // TODO: Make this static?
        try {
            int bytesRead;
            int bytesReceived = 0;
            while ( ( bytesRead = input.read(buffer, 0, buffer.length) ) != -1 ) {
                output.write(buffer, 0, bytesRead);
                bytesReceived += bytesRead;
                if ( bytesReceived > JSON_LENGTH_LIMIT ) {
                    throw new ContentLengthIOException("Content too large: "
                        + bytesReceived);
                }
            }
        } finally {
            try {
                if ( input != null ) {
                    input.close();
                }
                output.flush();
                output.close();
            } catch ( IOException ioe ) {
            }
        }

        final String json = new String(output.toByteArray());
        return new JSONTokener(json);
    }

    public static class ContentLengthIOException extends IOException {

        public ContentLengthIOException(String detailMessage) {
            super(detailMessage);
        }

    }

    /**
     * Build and return a new {@link XmlPullParser} with the given
     * {@link InputStream} assigned to it.
     */
    public static XmlPullParser newPullParser(InputStream input)
        throws XmlPullParserException {
        if ( sFactory == null ) {
            sFactory = XmlPullParserFactory.newInstance();
        }
        final XmlPullParser parser = sFactory.newPullParser();
        parser.setInput(input, null);
        return parser;
    }

    /**
     * Return a {@link Blocks#BLOCK_ID} matching the requested arguments.
     */
    public static String findBlock(String title, long startTime, long endTime) {
        // TODO: in future we might check provider if block exists
        return Blocks.generateBlockId(startTime, endTime);
    }

    /**
     * Return a {@link Blocks#BLOCK_ID} matching the requested arguments,
     * inserting a new {@link Blocks} entry as a
     * {@link ContentProviderOperation} when none already exists.
     */
    public static String findOrCreateBlock(String title, String type,
        long startTime, long endTime,
        ArrayList<ContentProviderOperation> batch, ContentResolver resolver) {
        // TODO: check for existence instead of always blindly creating. it's
        // okay for now since the database replaces on conflict.
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Blocks.CONTENT_URI);
        final String blockId = Blocks.generateBlockId(startTime, endTime);
        builder.withValue(Blocks.BLOCK_ID, blockId);
        builder.withValue(Blocks.BLOCK_TITLE, title);
        builder.withValue(Blocks.BLOCK_START, startTime);
        builder.withValue(Blocks.BLOCK_END, endTime);
        builder.withValue(Blocks.BLOCK_TYPE, type);
        batch.add(builder.build());
        return blockId;
    }

    /**
     * Query and return the {@link SyncColumns#UPDATED} time for the requested
     * {@link Uri}. Expects the {@link Uri} to reference a single item.
     */
    public static long queryItemUpdated(Uri uri, ContentResolver resolver) {
        final String[] projection = { SyncColumns.UPDATED };
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        try {
            if ( cursor.moveToFirst() ) {
                return cursor.getLong(0);
            } else {
                return CoscupContract.UPDATED_NEVER;
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Query and return the newest {@link SyncColumns#UPDATED} time for all
     * entries under the requested {@link Uri}. Expects the {@link Uri} to
     * reference a directory of several items.
     */
    public static long queryDirUpdated(Uri uri, ContentResolver resolver) {
        final String[] projection = { "MAX(" + SyncColumns.UPDATED + ")" };
        final Cursor cursor = resolver.query(uri, projection, null, null, null);
        try {
            cursor.moveToFirst();
            return cursor.getLong(0);
        } finally {
            cursor.close();
        }
    }

}
