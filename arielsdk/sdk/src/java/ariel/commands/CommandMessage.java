package ariel.commands;

import java.util.List;

/**
 * Created by mikalackis on 6.7.16..
 */
public class CommandMessage {

    private String action;

    private List<Param> params;

    public String getAction() {
        return action;
    }

    public List<Param> getParams() {
        return params;
    }

}
