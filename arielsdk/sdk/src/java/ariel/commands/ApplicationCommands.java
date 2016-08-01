package ariel.commands;

import java.util.ArrayList;

/**
 * Created by mikalackis on 29.7.16..
 */
public final class ApplicationCommands {

    // application commands
    public static final String APPLICATION_UPDATE_COMMAND = "app_update";

    // application params
    public static final String PARAM_PACKAGE_NAME = "package_name";

    public static class ApplicationParamBuilder{
        private ArrayList<Param> params;

        public ApplicationParamBuilder(final String packageName){
            params = new ArrayList<Param>();
            params.add(new Param(PARAM_PACKAGE_NAME, packageName));
        }

        public ArrayList<Param> build(){
            return params;
        }

    }

}
