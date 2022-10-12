package arielos.statusbar;

import android.content.Context;
import com.arielos.internal.statusbar.ArielStatusBarInterfaceImpl;

public final class ArielStatusBarManager {

    private static ArielStatusBarInterface sStatusBarInterface;

    public static ArielStatusBarInterface getInstance(Context context) {
       if (sStatusBarInterface == null) {
        sStatusBarInterface = new ArielStatusBarInterfaceImpl(context);
       }
       return sStatusBarInterface;
    }

}