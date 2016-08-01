package ariel.commands;

import java.util.ArrayList;

/**
 * Created by mikalackis on 6.7.16..
 */
public class CommandMessage {

    private String action;

    private ArrayList<Param> params;

    public String getAction() {
        return action;
    }

    public ArrayList<Param> getParams() {
        return params;
    }

}
