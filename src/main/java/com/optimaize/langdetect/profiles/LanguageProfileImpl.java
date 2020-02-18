/*
 * Copyright 2011 Fabian Kessler
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

package com.optimaize.langdetect.profiles;

import com.optimaize.langdetect.i18n.LdLocale;

import java.util.*;

/**
 * <p>This class is immutable.</p>
 *
 * @author Fabian Kessler
 */
public final class LanguageProfileImpl implements LanguageProfile {

    private final LdLocale locale;
    private final Map<Integer, Map<String,Integer>> ngrams;
    private final Stats stats;

    private static class Stats {
        /**
         * Key = gram length (1-3 or so).
         * Value = number of all occurrences of these grams combined.
         */
        private final Map<Integer, Long> numOccurrences;

        /**
         * Key = gram length (1-3 or so).
         * Value = number of occurrences of the n-gram that occurs the least often.
         * this can be 1, or larger if a cutoff was applied to remove infrequent grams.
         */
        private final Map<Integer, Long> minGramCounts;

        /**
         * Key = gram length (1-3 or so).
         * Value = number of occurrences of the n-gram that occurs the most often.
         */
        private final Map<Integer, Long> maxGramCounts;

        public Stats(Map<Integer, Long> numOccurrences,
                     Map<Integer, Long> minGramCounts,
                     Map<Integer, Long> maxGramCounts) {
            this.numOccurrences = Collections.unmodifiableMap(numOccurrences);
            this.minGramCounts  = Collections.unmodifiableMap(minGramCounts);
            this.maxGramCounts  = Collections.unmodifiableMap(maxGramCounts);
        }
    }


    /**
     * Use the builder.
     */
    LanguageProfileImpl(LdLocale locale,
                        Map<Integer, Map<String, Integer>> ngrams) {
        this.locale = locale;
        this.ngrams = Collections.unmodifiableMap(ngrams);
        this.stats  = makeStats(ngrams);
    }

    private static Stats makeStats(Map<Integer, Map<String, Integer>> ngrams) {
        Map<Integer, Long> numOccurrences = new HashMap<>(6);
        Map<Integer, Long> minGramCounts = new HashMap<>(6);
        Map<Integer, Long> maxGramCounts = new HashMap<>(6);
        for (Map.Entry<Integer, Map<String, Integer>> entry : ngrams.entrySet()) {
            long count = 0;
            Long min = null;
            Long max = null;
            for (Integer integer : entry.getValue().values()) {
                count += integer;
                if (min==null || min > integer) {
                    min = (long)integer;
                }
                if (max==null || max < integer) {
                    max = (long)integer;
                }
            }
            numOccurrences.put(entry.getKey(), count);
            minGramCounts.put(entry.getKey(), min);
            maxGramCounts.put(entry.getKey(), max);
        }
        return new Stats(numOccurrences, minGramCounts, maxGramCounts);
    }


    @Override
    public LdLocale getLocale() {
        return locale;
    }

    @Override
    public List<Integer> getGramLengths() {
        List<Integer> lengths = new ArrayList<>(ngrams.keySet());
        Collections.sort(lengths);
        return lengths;
    }

    @Override
    public int getFrequency(String gram) {
        Map<String, Integer> map = ngrams.get(gram.length());
        if (map==null) return 0;
        Integer freq = map.get(gram);
        if (freq==null) return 0;
        return freq;
    }

    @Override
    public int getNumGrams(int gramLength) {
        if (gramLength<1) throw new IllegalArgumentException(""+gramLength);
        Map<String, Integer> map = ngrams.get(gramLength);
        if (map==null) return 0;
        return map.size();
    }

    @Override
    public int getNumGrams() {
        int ret = 0;
        for (Map<String, Integer> stringIntegerMap : ngrams.values()) {
            ret += stringIntegerMap.size();
        }
        return ret;
    }

    @Override
    public long getNumGramOccurrences(int gramLength) {
        Long aLong = stats.numOccurrences.get(gramLength);
        if (aLong==null) return 0;
        return aLong;
    }

    @Override
    public long getMinGramCount(int gramLength) {
        Long aLong = stats.minGramCounts.get(gramLength);
        if (aLong==null) return 0;
        return aLong;
    }

    @Override
    public long getMaxGramCount(int gramLength) {
        Long aLong = stats.maxGramCounts.get(gramLength);
        if (aLong==null) return 0;
        return aLong;
    }


    @Override
    public Iterable<Map.Entry<String,Integer>> iterateGrams() {
        List<Map.Entry<String,Integer>> arr = new LinkedList<>();
        for (Map<String, Integer> stringIntegerMap : ngrams.values()) {
            arr.addAll(stringIntegerMap.entrySet());
        }
        return arr;
    }

    @Override
    public Iterable<Map.Entry<String, Integer>> iterateGrams(int gramLength) {
        return ngrams.get(gramLength).entrySet();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LanguageProfile{locale=");
        sb.append(locale);
        for (Integer integer : getGramLengths()) {
            sb.append(",");
            sb.append(integer);
            sb.append("-grams=");
            sb.append(getNumGrams(integer));
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LanguageProfileImpl that = (LanguageProfileImpl) o;

        if (!locale.equals(that.locale)) return false;
        return ngrams.equals(that.ngrams);
    }
    @Override
    public int hashCode() {
        int result = locale.hashCode();
        result = 31 * result + ngrams.hashCode();
        return result;
    }
}
