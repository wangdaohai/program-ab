package org.alicebot.ab;
/* Program AB Reference AIML 2.0 implementation
        Copyright (C) 2013 ALICE A.I. Foundation
        Contact: info@alicebot.org

        This library is free software; you can redistribute it and/or
        modify it under the terms of the GNU Library General Public
        License as published by the Free Software Foundation; either
        version 2 of the License, or (at your option) any later version.

        This library is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
        Library General Public License for more details.

        You should have received a copy of the GNU Library General Public
        License along with this library; if not, write to the
        Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
        Boston, MA  02110-1301, USA.
*/

/**
 * ParseState is a helper class for AIMLProcessor
 */
public class ParseState {
    /** node containing the category processed */
    public final Nodemapper leaf;
    /** client input */
    public final String input;
    /** bot's last sentence */
    public final String that;
    /** current topic */
    public final String topic;
    /** client session */
    public final Chat chatSession;
    /** depth in the parse tree, to prevent runaway recursion */
    public final int depth;
    public final Predicates vars;
    public final StarBindings starBindings;

    public ParseState(int depth, Chat chatSession, String input, String that, String topic, Nodemapper leaf) {
        this.chatSession = chatSession;
        this.input = input;
        this.that = that;
        this.topic = topic;
        this.leaf = leaf;
        this.depth = depth;  // to prevent runaway recursion
        this.vars = new Predicates();
        this.starBindings = leaf.starBindings;
    }
}
