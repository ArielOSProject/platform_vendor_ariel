package ariel.app;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

/**
 * Created by mikalackis on 15.5.17..
 */

public class SystemContextHelper {

    public static void startService(final Context context, final Intent intent){
        context.startServiceAsUser(intent, UserHandle.OWNER);
    }

}
