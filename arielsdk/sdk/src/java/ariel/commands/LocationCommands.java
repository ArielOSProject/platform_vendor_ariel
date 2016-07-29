package ariel.commands;

import java.util.ArrayList;

/**
 * Created by mikalackis on 29.7.16..
 */
public final class LocationCommands {

    // location commands
    public static final String LOCATE_NOW_COMMAND = "locate";
    public static final String TRACKING_START_COMMAND = "track_start";
    public static final String TRACKING_STOP_COMMAND = "track_stop";

    public enum PARAMS{
        SMS_REPORT("sms_location_report");

        private String param;
        PARAMS(String param){
            this.param = param;
        }

        public String getParam(){
            return param;
        }
    }

//    public static class LocationParamBuilder{
//        private ArrayList<Param> params;
//
//        public LocationParamBuilder(){
//            params = new ArrayList<Param>();
//        }
//
//        public LocationParamBuilder smsLocationReport(final boolean report){
//            params.add(new Param(PARAMS.SMS_REPORT.getParam(), report));
//            return this;
//        }
//
//        public ArrayList<Param> build(){
//            return params;
//        }
//
//    }

}
