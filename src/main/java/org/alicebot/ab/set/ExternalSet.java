package org.alicebot.ab.set;

import org.alicebot.ab.Sraix;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Set whose content need to be fetched on an external system */
public class ExternalSet extends AIMLSet {

    public static final String SET_MEMBER_STRING = "ISA";

    private final String host;
    private final String botId;
    private final int maxLength;

    private final Set<String> inCache = new HashSet<>();
    private final Set<String> outCache = new HashSet<>();

    public ExternalSet(String name, String host, String botId, int maxLength) {
        super(name);
        this.host = host;
        this.botId = botId;
        this.maxLength = maxLength;
    }

    public boolean contains(String s) {
        if (inCache.contains(s)) { return true; }
        if (outCache.contains(s)) { return true; }
        if (s.split(" ").length > maxLength) { return false; }
        String query = SET_MEMBER_STRING + name().toUpperCase() + " " + s;
        String response = Sraix.sraix(null, query, "false", null, host, botId, null, "0");
        //System.out.println("External "+name+" contains "+s+"? "+response);
        if ("true".equals(response)) {
            inCache.add(s);
            return true;
        } else {
            outCache.add(s);
            return false;
        }
    }

    @Override
    public Set<String> values() {
        return Collections.emptySet();
    }

    @Override
    public int maxLength() {
        return maxLength;
    }
}
