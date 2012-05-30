package tw.idv.gasolin.pycontw2012.io;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONTokener;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.os.RemoteException;

public abstract class JSONHandler {

    final String mAuthority;

    public JSONHandler(String authority) {
        mAuthority = authority;
    }

    public void parseAndApply(JSONTokener tokener, ContentResolver resolver,
        Resources res) throws HandlerException {
        try {

            final ArrayList<ContentProviderOperation> batch = parse(tokener,
                resolver, res);
            resolver.applyBatch(mAuthority, batch);

        } catch ( HandlerException e ) {
            throw e;
        } catch ( JSONException e ) {
            throw new HandlerException("Problem parsing JSON response", e);
        } catch ( IOException e ) {
            throw new HandlerException("Problem reading response", e);
        } catch ( RemoteException e ) {
            // Failed binder transactions aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        } catch ( OperationApplicationException e ) {
            // Failures like constraint violation aren't recoverable
            // TODO: write unit tests to exercise full provider
            // TODO: consider catching version checking asserts here, and then
            // wrapping around to retry parsing again.
            throw new RuntimeException("Problem applying batch operation", e);
        }
    }

    public abstract ArrayList<ContentProviderOperation> parse(
        JSONTokener tokener, ContentResolver resolver, Resources res)
        throws JSONException, IOException;

    public static class HandlerException extends IOException {
        public HandlerException(String message) {
            super(message);
        }

        public HandlerException(String message, Throwable cause) {
            super(message);
            initCause(cause);
        }

        @Override
        public String toString() {
            if ( getCause() != null ) {
                return getLocalizedMessage() + ": " + getCause();
            } else {
                return getLocalizedMessage();
            }
        }
    }

}
