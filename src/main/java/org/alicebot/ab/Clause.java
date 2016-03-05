package org.alicebot.ab;

public class Clause {
    public String subj;
    public String pred;
    public String obj;
    public final boolean affirm;

    public Clause(String s, String p, String o) {
        this(s, p, o, true);
    }

    public Clause(String s, String p, String o, boolean affirm) {
        subj = s;
        pred = p;
        obj = o;
        this.affirm = affirm;
    }

    public Clause copy() {
        return new Clause(subj, pred, obj, affirm);
    }
}
