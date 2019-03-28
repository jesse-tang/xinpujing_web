package com.google.code.kaptcha.text.impl;

import com.google.code.kaptcha.util.*;
import com.google.code.kaptcha.text.*;
import java.awt.image.*;
import java.util.*;
import java.awt.*;
import java.awt.font.*;

public class DefaultWordRenderer extends Configurable implements WordRenderer
{
    @Override
    public BufferedImage renderWord(final String word, final int width, final int height) {
        final int fontSize = this.getConfig().getTextProducerFontSize();
        final Font[] fonts = this.getConfig().getTextProducerFonts(fontSize);
        final Color color = this.getConfig().getTextProducerFontColor();
        final int charSpace = this.getConfig().getTextProducerCharSpace();
        final BufferedImage image = new BufferedImage(width, height, 2);
        final Graphics2D g2D = image.createGraphics();
        g2D.setColor(color);
        final RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        hints.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
        g2D.setRenderingHints(hints);
        final FontRenderContext frc = g2D.getFontRenderContext();
        final Random random = new Random();
        final int startPosY = (height - fontSize) / 5 + fontSize;
        final char[] wordChars = word.toCharArray();
        final Font[] chosenFonts = new Font[wordChars.length];
        final int[] charWidths = new int[wordChars.length];
        int widthNeeded = 0;
        for (int i = 0; i < wordChars.length; ++i) {
            chosenFonts[i] = fonts[random.nextInt(fonts.length)];
            final char[] charToDraw = { wordChars[i] };
            final GlyphVector gv = chosenFonts[i].createGlyphVector(frc, charToDraw);
            charWidths[i] = (int)gv.getVisualBounds().getWidth();
            if (i > 0) {
                widthNeeded += 2;
            }
            widthNeeded += charWidths[i];
        }
        int startPosX = 16;
        for (int j = 0; j < wordChars.length; ++j) {
            g2D.setFont(chosenFonts[j]);
            final char[] charToDraw2 = { wordChars[j] };
            g2D.drawChars(charToDraw2, 0, charToDraw2.length, startPosX, startPosY);
            startPosX = startPosX + charWidths[j] + charSpace;
        }
        return image;
    }
}
