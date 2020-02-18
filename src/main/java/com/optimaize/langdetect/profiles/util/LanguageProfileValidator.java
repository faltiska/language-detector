/*
 * Copyright 2011 Robert Erdin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.optimaize.langdetect.profiles.util;

import com.optimaize.langdetect.DetectedLanguage;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileBuilder;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Performs k-fold cross-validation.
 * See https://en.wikipedia.org/wiki/Cross-validation_(statistics)#k-fold_cross-validation
 *
 * This is meant to be run as a maintenance program, or for debugging. It's not used in production by this library.
 * Use the unit test.
 *
 * @author Robert Erdin
 */
public class LanguageProfileValidator {

    private final TextObjectFactory textObjectFactory = CommonTextObjectFactories.forIndexingCleanText();

    private int k = 10;
    private boolean breakWords = false;

    /**
     * All loaded language profiles.
     */
    private final List<LanguageProfile> languageProfiles = new ArrayList<>();
    private LanguageProfileBuilder languageProfileBuilder;
    private TextObject inputSample;


    /**
     * Set the k parameter to select into how many parts to partition the original sample. Default is 10.
     * @param k Minimum: 3
     */
    public LanguageProfileValidator setK(int k) {
        if( k <= 2 ) {
            throw new IllegalArgumentException("k hast to be at least 3 but was: "+k);
        }
        this.k = k;
        return this;
    }

    /**
     * Adds all {@link LanguageProfile}s that are available when calling {@link LanguageProfileReader#readAllBuiltIn()}.
     */
    public LanguageProfileValidator loadAllBuiltInLanguageProfiles() throws IOException {
        this.languageProfiles.addAll(new LanguageProfileReader().readAllBuiltIn());
        return this;
    }

    /**
     * Load the given {@link LanguageProfile}.
     */
    public LanguageProfileValidator loadLanguageProfile(LanguageProfile languageProfile) {
        this.languageProfiles.add(languageProfile);
        return this;
    }

    /**
     * Load the given {@link LanguageProfile}s.
     */
    public LanguageProfileValidator loadLanguageProfiles(Collection<LanguageProfile> languageProfiles) {
        this.languageProfiles.addAll(languageProfiles);
        return this;
    }

    /**
     * Sets the {@link LanguageProfileBuilder} which will be used to create the {@link LanguageProfile} during the validation.
     */
    public LanguageProfileValidator setLanguageProfileBuilder(LanguageProfileBuilder languageProfileBuilder) {
        this.languageProfileBuilder = languageProfileBuilder;
        return this;
    }

    /**
     * The sample to be used in the validation.
     */
    public LanguageProfileValidator loadInputSample(TextObject inputSample) {
        this.inputSample = inputSample;
        return this;
    }

    /**
     * Use for languages that don't use whitespaces to denominate word boundaries.
     * Default is false.
     * @param breakWords set true is you want to break sample into truly equal sizes.
     */
    public LanguageProfileValidator setBreakWords(boolean breakWords) {
        this.breakWords = breakWords;
        return this;
    }

    /**
     * Remove potential LanguageProfiles, e.g. in combination with {@link #loadAllBuiltInLanguageProfiles()}.
     * @param isoString the ISO string of the LanguageProfile to be removed.
     */
    public LanguageProfileValidator removeLanguageProfile(final String isoString) {
        languageProfiles.removeIf(languageProfile -> languageProfile.getLocale().getLanguage().equals(isoString));
        return this;
    }

    /**
     * Run the n-fold validation.
     * @return the average probability over all runs.
     */
    public float validate() {
        // remove a potential duplicate LanguageProfile
        this.removeLanguageProfile(this.languageProfileBuilder.build().getLocale().getLanguage());

        List<TextObject> partitionedInput = partition();
        List<Float> probabilities = new ArrayList<>(this.k);

        for (int i = 0; i < this.k; i++) {
            LanguageProfileBuilder lpb = new LanguageProfileBuilder(this.languageProfileBuilder);
            TextObject testSample = partitionedInput.get(i);

            List<TextObject> trainingSamples = new ArrayList<>(partitionedInput);
            trainingSamples.remove(i); //TODO this will throw a concurrent modification exception
            for (TextObject token : trainingSamples) {
                lpb.addText(token);
            }
            final LanguageProfile languageProfile = lpb.build();

            this.languageProfiles.add(languageProfile);

            final LanguageDetector languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(this.languageProfiles)
                    .build();

            // remove the newly created LanguageProfile for the next round
            this.languageProfiles.remove(this.languageProfiles.size() - 1);

            List<DetectedLanguage> detectedLanguages = languageDetector.getProbabilities(testSample);

            try{
                Iterator<DetectedLanguage> iterator = detectedLanguages.iterator();
                DetectedLanguage kResult = null;
                while(iterator.hasNext()) {
                    DetectedLanguage language = iterator.next();
                    if(language.getLocale().getLanguage().equals(languageProfile.getLocale().getLanguage())) {
                        kResult = language;
                        break;
                    }
                }
                if(kResult != null) {
                    probabilities.add(kResult.getProbability());
                }
            } catch (NoSuchElementException e){
                probabilities.add(0f);
            }
        }

        float sum = 0f;
        for (Float token : probabilities) {
            sum += token;
        }
        float avg = sum / this.k;

        return avg;
    }

    private List<TextObject> partition() {
        List<TextObject> result = new ArrayList<>(this.k);
        if (!breakWords) {
            int maxLength = this.inputSample.length() / (this.k - 1);
            StringBuilder regexBuilder = new StringBuilder().append("\\G\\s*(.{1,").append(maxLength).append("})(?=\\s|$)");
            Pattern p = Pattern.compile(regexBuilder.toString(), Pattern.DOTALL);
            Matcher m = p.matcher(this.inputSample);
            while (m.find())
                result.add(textObjectFactory.create().append(m.group(1)));
        } else {
            StringBuilder regexBuilder = new StringBuilder().append("(?<=\\G.{").append(this.k).append("})");
            String[] split = this.inputSample.toString().split(regexBuilder.toString());
            for (String token : split) {
                result.add(textObjectFactory.create().append(token));
            }
        }
        return result;
    }
}
