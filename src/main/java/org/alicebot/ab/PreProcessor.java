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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.stream.Stream;

/**
 * AIML Preprocessor and substitutions
 */
public class PreProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PreProcessor.class);

    private final SubstitutionList normalSubstitutions;
    private final SubstitutionList denormalSubstitutions;
    private final SubstitutionList personSubstitutions;
    private final SubstitutionList person2Substitutions;
    private final SubstitutionList genderSubstitutions;

    /**
     * Constructor given bot
     *
     * @param bot AIML bot
     */
    public PreProcessor(Bot bot) {
        normalSubstitutions = new SubstitutionList(bot.config_path, "normal.txt");
        denormalSubstitutions = new SubstitutionList(bot.config_path, "denormal.txt");
        personSubstitutions = new SubstitutionList(bot.config_path, "person.txt");
        person2Substitutions = new SubstitutionList(bot.config_path, "person2.txt");
        genderSubstitutions = new SubstitutionList(bot.config_path, "gender.txt");
    }

    /**
     * apply normalization substitutions to a request
     *
     * @param request client input
     * @return normalized client input
     */
    public String normalize(String request) {
        logger.trace("PreProcessor.normalize(request: {})", request);
        String result = normalSubstitutions.substitute(request);
        result = result.replaceAll("(\r\n|\n\r|\r|\n)", " ");
        logger.trace("PreProcessor.normalize() returning: {}", result);
        return result;
    }

    /**
     * apply denormalization substitutions to a request
     *
     * @param request client input
     * @return normalized client input
     */
    public String denormalize(String request) {
        return denormalSubstitutions.substitute(request);
    }

    /**
     * personal pronoun substitution for {@code <person></person>} tag
     *
     * @param input sentence
     * @return sentence with pronouns swapped
     */
    public String person(String input) {
        return personSubstitutions.substitute(input);

    }

    /**
     * personal pronoun substitution for {@code <person2></person2>} tag
     *
     * @param input sentence
     * @return sentence with pronouns swapped
     */
    public String person2(String input) {
        return person2Substitutions.substitute(input);

    }

    /**
     * personal pronoun substitution for {@code <gender>} tag
     *
     * @param input sentence
     * @return sentence with pronouns swapped
     */
    public String gender(String input) {
        return genderSubstitutions.substitute(input);

    }

    /**
     * Split an input into an array of sentences based on sentence-splitting characters.
     *
     * @param line input text
     * @return array of sentences
     */
    public String[] sentenceSplit(String line) {
        line = line.replace("。", ".");
        line = line.replace("？", "?");
        line = line.replace("！", "!");
        //System.out.println("Sentence split "+line);
        String[] result = line.split("[\\.!\\?]");
        for (int i = 0; i < result.length; i++) { result[i] = result[i].trim(); }
        return result;
    }

    /**
     * normalize a file consisting of sentences, one sentence per line.
     *
     * @param infile  input file
     * @param outfile output file to write results
     */
    public void normalizeFile(String infile, String outfile) {
        try {
            Stream<String> sentenceStream = Files.lines(new File(infile).toPath())
                .map(String::trim).filter(l -> !l.isEmpty())
                .flatMap(strLine -> {
                    String norm = normalize(strLine).toUpperCase();
                    String[] sentences = sentenceSplit(norm);
                    if (sentences.length > 1) {
                        for (String s : sentences) {
                            logger.info("{}-->{}", norm, s);
                        }
                    }
                    return Stream.of(sentences);
                }).map(String::trim).filter(s -> !s.isEmpty());

            Files.write(new File(outfile).toPath(), (Iterable<String>) sentenceStream::iterator);
        } catch (Exception ex) {
            logger.error("normalizeFile error", ex);
        }
    }
}
