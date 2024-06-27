package arielos.firewall;

import android.content.Context;
import com.arielos.internal.firewall.FirewallInterfaceImpl;

/**
 * Provides access to [FirewallInterface] which controls network access
 * for applications.
 */
public final class FirewallManager {

    private static FirewallInterface sFirewallInterface;

    public static FirewallInterface getInstance(Context context) {
        if (sFirewallInterface == null) {
            sFirewallInterface = new FirewallInterfaceImpl(context);
        }
        return sFirewallInterface;
    }

}