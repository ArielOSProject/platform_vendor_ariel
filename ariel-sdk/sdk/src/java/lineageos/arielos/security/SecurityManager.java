package lineageos.arielos.security;

import android.content.Context;
import org.lineageos.internal.arielos.security.SecurityInterfaceImpl;

public final class SecurityManager {

    private static SecurityInterface sSecurityInterface;

    public static SecurityInterface getInstance(Context context) {
       if (sSecurityInterface == null) {
        sSecurityInterface = new SecurityInterfaceImpl(context);
       }
       return sSecurityInterface;
    }

}