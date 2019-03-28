package com.google.code.kaptcha.impl;

import com.google.code.kaptcha.util.*;
import java.awt.image.*;
import com.google.code.kaptcha.text.*;
import com.google.code.kaptcha.*;
import java.awt.geom.*;
import java.awt.*;

public class DefaultKaptcha extends Configurable implements Producer
{
    private int width;
    private int height;
    
    public DefaultKaptcha() {
        this.width = 200;
        this.height = 50;
    }
    
    @Override
    public BufferedImage createImage(final String text) {
        final WordRenderer wordRenderer = this.getConfig().getWordRendererImpl();
        final GimpyEngine gimpyEngine = this.getConfig().getObscurificatorImpl();
        final BackgroundProducer backgroundProducer = this.getConfig().getBackgroundImpl();
        final boolean isBorderDrawn = this.getConfig().isBorderDrawn();
        this.width = this.getConfig().getWidth();
        this.height = this.getConfig().getHeight();
        BufferedImage bi = wordRenderer.renderWord(text, this.width, this.height);
        bi = gimpyEngine.getDistortedImage(bi);
        bi = backgroundProducer.addBackground(bi);
        final Graphics2D graphics = bi.createGraphics();
        if (isBorderDrawn) {
            this.drawBox(graphics);
        }
        return bi;
    }
    
    private void drawBox(final Graphics2D graphics) {
        final Color borderColor = this.getConfig().getBorderColor();
        final int borderThickness = this.getConfig().getBorderThickness();
        graphics.setColor(borderColor);
        if (borderThickness != 1) {
            final BasicStroke stroke = new BasicStroke(borderThickness);
            graphics.setStroke(stroke);
        }
        final Line2D line1 = new Line2D.Double(0.0, 0.0, 0.0, this.width);
        graphics.draw(line1);
        Line2D line2 = new Line2D.Double(0.0, 0.0, this.width, 0.0);
        graphics.draw(line2);
        line2 = new Line2D.Double(0.0, this.height - 1, this.width, this.height - 1);
        graphics.draw(line2);
        line2 = new Line2D.Double(this.width - 1, this.height - 1, this.width - 1, 0.0);
        graphics.draw(line2);
    }
    
    @Override
    public String createText() {
        return this.getConfig().getTextProducerImpl().getText();
    }
}
