package arielos.security;

import android.content.Context;
import com.arielos.internal.security.SecurityInterfaceImpl;

public final class SecurityManager {

    private static SecurityInterface sSecurityInterface;

    public static SecurityInterface getInstance(Context context) {
        if (sSecurityInterface == null) {
            sSecurityInterface = new SecurityInterfaceImpl(context);
        }
        return sSecurityInterface;
    }

}