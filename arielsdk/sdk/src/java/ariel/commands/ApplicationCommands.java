package ariel.commands;

import java.util.ArrayList;

/**
 * Created by mikalackis on 29.7.16..
 */
public final class ApplicationCommands {

    // application commands
    public static final String APPLICATION_UPDATE_COMMAND = "app_update";

    public enum PARAMS{
        PACKAGE_NAME("package_name");

        private String param;
        PARAMS(String param){
            this.param = param;
        }

        public String getParam(){
            return param;
        }
    }

//    public static class ApplicationParamBuilder{
//        private ArrayList<Param> params;
//
//        public ApplicationParamBuilder(final String packageName){
//            params = new ArrayList<Param>();
//            params.add(new Param(PARAMS.PACKAGE_NAME.getParam(), packageName));
//        }
//
//        public ArrayList<Param> build(){
//            return params;
//        }
//
//    }

}
