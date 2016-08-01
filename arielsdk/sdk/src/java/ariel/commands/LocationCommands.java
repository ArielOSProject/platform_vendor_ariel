package ariel.commands;

import android.os.Bundle;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by mikalackis on 29.7.16..
 */
public final class LocationCommands {

    // location commands
    public static final String LOCATE_NOW_COMMAND = "locate";
    public static final String TRACKING_START_COMMAND = "track_start";
    public static final String TRACKING_STOP_COMMAND = "track_stop";

    // command parameters
    public static final String PARAM_SMS_LOCATION_REPORT = "sms_location_report";

    /**
     * Napraviti tako da ova klasa moze da se instancira, tako da se definise
     * koja je komanda u pitanju kao i parametri koji idu uz tu komandu a pomocu
     * builder paterna.
     */

    public static class LocationParamBuilder{
        private ArrayList<Param> mParams;

        public LocationParamBuilder() {
            mParams = new ArrayList<>();
        }

        public LocationParamBuilder smsLocationReport(final boolean report){
            mParams.add(new Param(PARAM_SMS_LOCATION_REPORT, ""+report));
            return this;
        }

        public ArrayList<Param> build(){
            return mParams;
        }

    }

}
