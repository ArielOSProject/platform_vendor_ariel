package arielos.restrictions;

import android.content.Context;
import com.arielos.internal.restrictions.RestrictionsInterfaceImpl;

public final class RestrictionsManager {

    private static RestrictionsInterface sRestrictionsInterface;

    public static RestrictionsInterface getInstance(Context context) {
       if (sRestrictionsInterface == null) {
        sRestrictionsInterface = new RestrictionsInterfaceImpl(context);
       }
       return sRestrictionsInterface;
    }

}