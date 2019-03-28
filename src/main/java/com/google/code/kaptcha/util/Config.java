package com.google.code.kaptcha.util;

import java.util.*;
import org.hibernate.validator.internal.util.privilegedactions.*;
import java.awt.*;
import java.io.*;
import com.google.code.kaptcha.text.*;
import com.google.code.kaptcha.text.impl.*;
import com.google.code.kaptcha.*;
import com.google.code.kaptcha.impl.*;

public class Config
{
    private Properties properties;
    private ConfigHelper helper;
    
    public Config(final Properties properties) {
        this.properties = properties;
        this.helper = new ConfigHelper();
    }
    
    public boolean isBorderDrawn() {
        final String paramName = "kaptcha.border";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getBoolean(paramName, paramValue, true);
    }
    
    public Color getBorderColor() {
        final String paramName = "kaptcha.border.color";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getColor(paramName, paramValue, Color.BLACK);
    }
    
    public int getBorderThickness() {
        final String paramName = "kaptcha.border.thickness";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getPositiveInt(paramName, paramValue, 1);
    }
    
    public Producer getProducerImpl() {
        final String paramName = "kaptcha.producer.impl";
        final String paramValue = this.properties.getProperty(paramName);
        final Producer producer = (Producer)this.helper.getClassInstance(paramName, paramValue, new DefaultKaptcha(), this);
        return producer;
    }
    
    public TextProducer getTextProducerImpl() {
        final String paramName = "kaptcha.textproducer.impl";
        final String paramValue = this.properties.getProperty(paramName);
        final TextProducer textProducer = (TextProducer)this.helper.getClassInstance(paramName, paramValue, new DefaultTextCreator(), this);
        return textProducer;
    }
    
    public char[] getTextProducerCharString() {
        final String paramName = "kaptcha.textproducer.char.string";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getChars(paramName, paramValue, "abcde2345678gfynmnpwx".toCharArray());
    }
    
    public int getTextProducerCharLength() {
        final String paramName = "kaptcha.textproducer.char.length";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getPositiveInt(paramName, paramValue, 5);
    }
    
    public Font[] getTextProducerFonts(int fontSize) {
        final String path = (fontSize % 10 == 0) ? GetResource.class.getClassLoader().getResource("font/Goffik-O.ttf").getPath() : GetResource.class.getClassLoader().getResource("font/Choplin-Medium-DEMO.otf").getPath();
        try {
            Font font = Font.createFont(0, new File(path));
            font = font.deriveFont(Float.parseFloat(fontSize + ""));
            return new Font[] { font };
        }
        catch (FontFormatException | IOException ex2) {
//            final Exception ex;
//            final Exception e = ex;
            ex2.printStackTrace();
            fontSize = 80;
            final String paramName = "kaptcha.textproducer.font.names";
            final String paramValue = this.properties.getProperty(paramName);
            return this.helper.getFonts(paramName, paramValue, fontSize, new Font[] { new Font("Arial", 0, fontSize), new Font("Courier", 0, fontSize) });
        }
    }
    
    public int getTextProducerFontSize() {
        final String paramName = "kaptcha.textproducer.font.size";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getPositiveInt(paramName, paramValue, 40);
    }
    
    public Color getTextProducerFontColor() {
        final String paramName = "kaptcha.textproducer.font.color";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getColor(paramName, paramValue, Color.BLACK);
    }
    
    public int getTextProducerCharSpace() {
        final String paramName = "kaptcha.textproducer.char.space";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getPositiveInt(paramName, paramValue, 2);
    }
    
    public NoiseProducer getNoiseImpl() {
        final String paramName = "kaptcha.noise.impl";
        final String paramValue = this.properties.getProperty(paramName);
        final NoiseProducer noiseProducer = (NoiseProducer)this.helper.getClassInstance(paramName, paramValue, new DefaultNoise(), this);
        return noiseProducer;
    }
    
    public Color getNoiseColor() {
        final String paramName = "kaptcha.noise.color";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getColor(paramName, paramValue, Color.RED);
    }
    
    public GimpyEngine getObscurificatorImpl() {
        final String paramName = "kaptcha.obscurificator.impl";
        final String paramValue = this.properties.getProperty(paramName);
        final GimpyEngine gimpyEngine = (GimpyEngine)this.helper.getClassInstance(paramName, paramValue, new WaterRipple(), this);
        return gimpyEngine;
    }
    
    public WordRenderer getWordRendererImpl() {
        final String paramName = "kaptcha.word.impl";
        final String paramValue = this.properties.getProperty(paramName);
        final WordRenderer wordRenderer = (WordRenderer)this.helper.getClassInstance(paramName, paramValue, new DefaultWordRenderer(), this);
        return wordRenderer;
    }
    
    public BackgroundProducer getBackgroundImpl() {
        final String paramName = "kaptcha.background.impl";
        final String paramValue = this.properties.getProperty(paramName);
        final BackgroundProducer backgroundProducer = (BackgroundProducer)this.helper.getClassInstance(paramName, paramValue, new DefaultBackground(), this);
        return backgroundProducer;
    }
    
    public Color getBackgroundColorFrom() {
        final String paramName = "kaptcha.background.clear.from";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getColor(paramName, paramValue, Color.WHITE);
    }
    
    public Color getBackgroundColorTo() {
        final String paramName = "kaptcha.background.clear.to";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getColor(paramName, paramValue, Color.WHITE);
    }
    
    public int getWidth() {
        final String paramName = "kaptcha.image.width";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getPositiveInt(paramName, paramValue, 200);
    }
    
    public int getHeight() {
        final String paramName = "kaptcha.image.height";
        final String paramValue = this.properties.getProperty(paramName);
        return this.helper.getPositiveInt(paramName, paramValue, 50);
    }
    
    public String getSessionKey() {
        return this.properties.getProperty("kaptcha.session.key", "KAPTCHA_SESSION_KEY");
    }
    
    public String getSessionDate() {
        return this.properties.getProperty("kaptcha.session.date", "KAPTCHA_SESSION_DATE");
    }
    
    public Properties getProperties() {
        return this.properties;
    }
}
