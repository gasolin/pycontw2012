package tw.idv.gasolin.pycontw2012.io;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;

/**
 * General {@link IOException} that indicates a problem occured while parsing or
 * applying an {@link XmlPullParser}.
 */
public class HandlerException extends IOException {

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
