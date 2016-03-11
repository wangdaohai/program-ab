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

/** Linked list representation of Pattern Path and Input Path. */
public final class Path {

    public final String word;
    public final Path next;
    public final int length;

    private Path(String word, Path next) {
        this.word = word;
        this.next = next;
        this.length = next == null ? 1 : (next.length + 1);
    }

    /** Convert a sentence (a string consisting of words separated by single spaces) into a Path. */
    public static Path fromSentence(String sentence) {
        String[] array = sentence.trim().split(" ");
        Path head = null;
        for (int i = array.length - 1; i >= 0; i--) {
            head = new Path(array[i], head);
        }
        return head;
    }

    /** The inverse of {@link #fromSentence}. */
    public static String toSentence(Path path) {
        StringBuilder result = new StringBuilder();
        for (Path p = path; p != null; p = p.next) {
            result.append(" ").append(p.word);
        }
        return result.toString().trim();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Path p = this; p != null; p = p.next) {
            result.append(p.word).append(",");
        }
        if (result.length() != 0) {result.deleteCharAt(result.length() - 1);}
        return result.toString();
    }

}
