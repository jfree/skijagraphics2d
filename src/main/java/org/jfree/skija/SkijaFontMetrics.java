/* ===============
 * SkijaGraphics2D
 * ===============
 *
 * (C)opyright 2021-present, by David Gilbert.
 *
 * The SkijaGraphics2D class has been developed by David Gilbert for
 * use with Orson Charts (https://github.com/jfree/orson-charts) and
 * JFreeChart (https://www.jfree.org/jfreechart).  It may be useful for other
 * code that uses the Graphics2D API provided by Java2D.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   - Neither the name of the Object Refinery Limited nor the
 *     names of its contributors may be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL OBJECT REFINERY LIMITED BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.jfree.skija;

import io.github.humbleui.skija.FontMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Returns font metrics.
 */
public class SkijaFontMetrics extends java.awt.FontMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(SkijaFontMetrics.class);

    /** Skija font. */
    private io.github.humbleui.skija.Font skijaFont;

    /** Skija font metrics. */
    private FontMetrics metrics;

    /**
     * Creates a new instance.
     *
     * @param skijaFont  the Skija font ({@code null} not permitted).
     * @param awtFont  the AWT font ({@code null} not permitted).
     */
    public SkijaFontMetrics(io.github.humbleui.skija.Font skijaFont, Font awtFont) {
        super(awtFont);
        this.metrics = skijaFont.getMetrics();
        this.skijaFont = skijaFont;
    }

    /**
     * Returns the leading.
     *
     * @return The leading.
     */
    @Override
    public int getLeading() {
        int result = (int) this.metrics.getLeading();
        LOGGER.debug("getLeading() -> {}", result);
        return result;
    }

    /**
     * Returns the ascent for the font.
     *
     * @return The ascent.
     */
    @Override
    public int getAscent() {
        int result = (int) -this.metrics.getAscent();
        LOGGER.debug("getAscent() -> {}", result);
        return result;
    }

    /**
     * Returns the descent for the font.
     *
     * @return The descent.
     */
    @Override
    public int getDescent() {
        int result = (int) this.metrics.getDescent();
        LOGGER.debug("getDescent() -> {}", result);
        return result;
    }

    /**
     * Returns the width of the specified character.
     *
     * @param ch  the character.
     *
     * @return The width.
     */
    @Override
    public int charWidth(char ch) {
        int result = (int) this.skijaFont.measureTextWidth(Character.toString(ch));
        LOGGER.debug("charWidth({}) -> {}", ch, result);
        return result;
    }

    /**
     * Returns the width of a character sequence.
     *
     * @param data  the characters.
     * @param off  the offset.
     * @param len  the length.
     *
     * @return The width of the character sequence.
     */
    @Override
    public int charsWidth(char[] data, int off, int len) {
        int result = (int) this.skijaFont.measureTextWidth(new String(data, off, len));
        LOGGER.debug("charsWidth({}, {}, {}) -> {}", data, off, len, result);
        return result;
    }
}
