package ariel.context;

import android.content.Context;
import android.content.Intent;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.app.backup.IBackupManager;
import android.os.RemoteException;

/**
 * Created by mikalackis on 15.5.17..
 */

public class SystemContextHelper {

    public static void startService(final Context context, final Intent intent){
        context.startServiceAsUser(intent, UserHandle.SYSTEM);
    }

}
