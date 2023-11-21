package arielos.firewall;

import android.content.Context;
import com.arielos.internal.firewall.IntentFirewallInterfaceImpl;

public final class IntentFirewallManager {

    private static IntentFirewallInterface sIntentFirewallInterface;

    public static IntentFirewallInterface getInstance(Context context) {
        if (sIntentFirewallInterface == null) {
            sIntentFirewallInterface = new IntentFirewallInterfaceImpl(context);
        }
        return sIntentFirewallInterface;
    }

}