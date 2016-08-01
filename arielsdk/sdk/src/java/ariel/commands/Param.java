package ariel.commands;

/**
 * Created by mikalackis on 29.7.16..
 */
public class Param {
    final String paramName;
    final String value;

    public Param(String paramName, String value) {
        this.paramName = paramName;
        this.value = value;
    }

    public String getParamName() {
        return paramName;
    }

    public Object getValue() {
        return value;
    }

}