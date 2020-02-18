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

package com.optimaize.langdetect.i18n;

import org.junit.Test;

import static org.junit.Assert.*;

public class LdLocaleTest {

    @Test
    public void justLanguage() {
        expectJustLanguage("en");
        expectJustLanguage("gsw");
    }
    private void expectJustLanguage(String lang) {
        LdLocale locale = LdLocale.fromString(lang);
        assertEquals(locale.toString(), lang);
        assertEquals(locale.getLanguage(), lang);
        assertNull(locale.getScript());
        assertNull(locale.getRegion());
    }

    @Test
    public void languageAndScript() {
        expectLanguageAndScript("en", "Latn");
        expectLanguageAndScript("gsw", "Latn");
        expectLanguageAndScript("zh", "Hans");
    }
    private void expectLanguageAndScript(String lang, String script) {
        LdLocale locale = LdLocale.fromString(lang+'-'+script);
        assertEquals(locale.toString(), lang+'-'+script);
        assertEquals(locale.getLanguage(), lang);
        assertEquals(locale.getScript(), script);
        assertNull(locale.getRegion());
    }

    @Test
    public void languageAndRegion() {
        expectLanguageAndRegion("en", "UK");
        expectLanguageAndRegion("zh", "CN");
    }
    private void expectLanguageAndRegion(String lang, String region) {
        LdLocale locale = LdLocale.fromString(lang+'-'+region);
        assertEquals(locale.toString(), lang+'-'+region);
        assertEquals(locale.getLanguage(), lang);
        assertNull(locale.getScript());
        assertEquals(locale.getRegion(), region);
    }

    @Test
    public void all() {
        expectAll("en", "Latn", "UK");
        expectAll("zh", "Hant", "CN");
    }
    private void expectAll(String lang, String script, String region) {
        LdLocale locale = LdLocale.fromString(lang+'-'+script+'-'+region);
        assertEquals(locale.toString(), lang+'-'+script+'-'+region);
        assertEquals(locale.getLanguage(), lang);
        assertEquals(locale.getScript(), script);
        assertEquals(locale.getRegion(), region);
    }



    @Test
    public void equalsYes() {
        expectEqualsYes("en");
        expectEqualsYes("en-Latn-UK");
    }
    private void expectEqualsYes(String s) {
        LdLocale locale1 = LdLocale.fromString(s);
        LdLocale locale2 = LdLocale.fromString(locale1.toString());
        assertEquals(locale1, locale2);
    }




    @Test
    public void invalid() {
        //language required
        expectInvalid("");
        expectInvalid(null);

        //invalid syntax
        expectInvalid("-");
        expectInvalid("--");
        expectInvalid("xx-");
        expectInvalid("-xx");
        expectInvalid("-xx-");
        expectInvalid("de--CH");
        expectInvalid("de--Latn");

        //invalid language: too short or too long
        expectInvalid("x");
        expectInvalid("xxxx");

        //wrong order
        expectInvalid("de-CH-Latn");

        //missing language
        expectInvalid("Latn");
        expectInvalid("CH");
        expectInvalid("CH-Latn");

        //incorrect case
        expectInvalid("JA");
        expectInvalid("ja-jp");
        expectInvalid("ja-jpan");
        expectInvalid("ja-JPAN");

        //incorrect separator
        expectInvalid("de_CH");
        expectInvalid("de CH");
    }
    public void expectInvalid(String s) {
        try {
            LdLocale.fromString(s);
            fail("Expected failure for: "+s);
        } catch (IllegalArgumentException e) {
            //ok, expected that
        }
    }

}
