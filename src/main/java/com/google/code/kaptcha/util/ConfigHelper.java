package com.google.code.kaptcha.util;

import java.lang.reflect.*;
import java.awt.*;

public class ConfigHelper
{
    public Color getColor(final String paramName, final String paramValue, final Color defaultColor) {
        Color color;
        if ("".equals(paramValue) || paramValue == null) {
            color = defaultColor;
        }
        else if (paramValue.indexOf(",") > 0) {
            color = this.createColorFromCommaSeparatedValues(paramName, paramValue);
        }
        else {
            color = this.createColorFromFieldValue(paramName, paramValue);
        }
        return color;
    }
    
    public Color createColorFromCommaSeparatedValues(final String paramName, final String paramValue) {
        final String[] colorValues = paramValue.split(",");
        Color color;
        try {
            final int r = Integer.parseInt(colorValues[0]);
            final int g = Integer.parseInt(colorValues[1]);
            final int b = Integer.parseInt(colorValues[2]);
            if (colorValues.length == 4) {
                final int a = Integer.parseInt(colorValues[3]);
                color = new Color(r, g, b, a);
            }
            else {
                if (colorValues.length != 3) {
                    throw new ConfigException(paramName, paramValue, "Color can only have 3 (RGB) or 4 (RGB with Alpha) values.");
                }
                color = new Color(r, g, b);
            }
        }
        catch (NumberFormatException nfe) {
            throw new ConfigException(paramName, paramValue, nfe);
        }
        catch (ArrayIndexOutOfBoundsException aie) {
            throw new ConfigException(paramName, paramValue, aie);
        }
        catch (IllegalArgumentException iae) {
            throw new ConfigException(paramName, paramValue, iae);
        }
        return color;
    }
    
    public Color createColorFromFieldValue(final String paramName, final String paramValue) {
        Color color;
        try {
            final Field field = Class.forName("java.awt.Color").getField(paramValue);
            color = (Color)field.get(null);
        }
        catch (NoSuchFieldException nsfe) {
            throw new ConfigException(paramName, paramValue, nsfe);
        }
        catch (ClassNotFoundException cnfe) {
            throw new ConfigException(paramName, paramValue, cnfe);
        }
        catch (IllegalAccessException iae) {
            throw new ConfigException(paramName, paramValue, iae);
        }
        return color;
    }
    
    public Object getClassInstance(final String paramName, final String paramValue, final Object defaultInstance, final Config config) {
        Object instance;
        if ("".equals(paramValue) || paramValue == null) {
            instance = defaultInstance;
        }
        else {
            try {
                instance = Class.forName(paramValue).newInstance();
            }
            catch (IllegalAccessException iae) {
                throw new ConfigException(paramName, paramValue, iae);
            }
            catch (ClassNotFoundException cnfe) {
                throw new ConfigException(paramName, paramValue, cnfe);
            }
            catch (InstantiationException ie) {
                throw new ConfigException(paramName, paramValue, ie);
            }
        }
        this.setConfigurable(instance, config);
        return instance;
    }
    
    public Font[] getFonts(final String paramName, final String paramValue, final int fontSize, final Font[] defaultFonts) {
        Font[] fonts;
        if ("".equals(paramValue) || paramValue == null) {
            fonts = defaultFonts;
        }
        else {
            final String[] fontNames = paramValue.split(",");
            fonts = new Font[fontNames.length];
            for (int i = 0; i < fontNames.length; ++i) {
                fonts[i] = new Font(fontNames[i], 1, fontSize);
            }
        }
        return fonts;
    }
    
    public int getPositiveInt(final String paramName, final String paramValue, final int defaultInt) {
        int intValue;
        if ("".equals(paramValue) || paramValue == null) {
            intValue = defaultInt;
        }
        else {
            try {
                intValue = Integer.parseInt(paramValue);
                if (intValue < 1) {
                    throw new ConfigException(paramName, paramValue, "Value must be greater than or equals to 1.");
                }
            }
            catch (NumberFormatException nfe) {
                throw new ConfigException(paramName, paramValue, nfe);
            }
        }
        return intValue;
    }
    
    public char[] getChars(final String paramName, final String paramValue, final char[] defaultChars) {
        char[] chars;
        if ("".equals(paramValue) || paramValue == null) {
            chars = defaultChars;
        }
        else {
            chars = paramValue.toCharArray();
        }
        return chars;
    }
    
    public boolean getBoolean(final String paramName, final String paramValue, final boolean defaultValue) {
        boolean booleanValue;
        if ("yes".equals(paramValue) || "".equals(paramValue) || paramValue == null) {
            booleanValue = defaultValue;
        }
        else {
            if (!"no".equals(paramValue)) {
                throw new ConfigException(paramName, paramValue, "Value must be either yes or no.");
            }
            booleanValue = false;
        }
        return booleanValue;
    }
    
    private void setConfigurable(final Object object, final Config config) {
        if (object instanceof Configurable) {
            ((Configurable)object).setConfig(config);
        }
    }
}
