package tw.idv.gasolin.pycontw2012.util;

import android.text.TextUtils;

public class SparseStringArray extends SparseArray<String> {

    @Override
    public int indexOfValue(String value) {
        if ( mGarbage ) {
            gc();
        }

        for ( int i = 0; i < mSize; i++ )
            if ( TextUtils.equals((String) mValues[i], value) )
                return i;

        return -1;
    }

}
