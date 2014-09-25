/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.pdmodel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.encoding.GlyphList;
import org.apache.pdfbox.pdmodel.common.COSDictionaryMap;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontFactory;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;

/**
 * This represents a set of resources available at the page/pages/stream level.
 * 
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 * 
 */
public class PDResources implements COSObjectable
{
    private COSDictionary resources;
    private Map<String, PDFont> fonts = null;
    private Map<PDFont, String> fontMappings = new HashMap<PDFont, String>();
    private Map<String, PDColorSpace> colorspaces = null;
    private Map<String, PDXObject> xobjects = null;
    private Map<PDXObject, String> xobjectMappings = null;
    private HashMap<String, PDImageXObject> images = null;
    private Map<String, PDExtendedGraphicsState> graphicsStates = null;
    private Map<String, PDAbstractPattern> patterns = null;
    private Map<String, PDShading> shadings = null;

    /**
     * Log instance.
     */
    private static final Log LOG = LogFactory.getLog(PDResources.class);

    /**
     * Default constructor.
     */
    public PDResources()
    {
        resources = new COSDictionary();
    }

    /**
     * Prepopulated resources.
     * 
     * @param resourceDictionary The cos dictionary for this resource.
     */
    public PDResources(COSDictionary resourceDictionary)
    {
        resources = resourceDictionary;
    }

    /**
     * This will get the underlying dictionary.
     * 
     * @return The dictionary for these resources.
     */
    public COSDictionary getCOSDictionary()
    {
        return resources;
    }

    /**
     * Convert this standard java object to a COS object.
     * 
     * @return The cos object that matches this Java object.
     */
    public COSBase getCOSObject()
    {
        return resources;
    }

    /**
     * Calling this will release all cached information.
     */
    public void clearCache()
    {
        fonts = null;
        fontMappings = null;
        colorspaces = null;
        xobjects = null;
        xobjectMappings = null;
        images = null;
        graphicsStates = null;
        patterns = null;
        shadings = null;
    }

    /**
     * This will get the map of fonts. This will never return null. The keys are string and the values are PDFont
     * objects.
     * 
     * @param fontCache A map of existing PDFont objects to reuse.
     * @return The map of fonts.
     * 
     * @throws IOException If there is an error getting the fonts.
     * 
     * @deprecated due to some side effects font caching is no longer supported, use {@link #getFonts()} instead
     */
    public Map<String, PDFont> getFonts(Map<String, PDFont> fontCache) throws IOException
    {
        return getFonts();
    }

    /**
     * This will get the map of fonts. This will never return null.
     *
     * @return The map of fonts.
     */
    public Map<String, PDFont> getFonts() throws IOException
    {
        return getFonts((GlyphList) null);
    }

    /**
     * This will get the map of fonts. This will never return null.
     *
     * @param glyphList A custom glyph list for Unicode mapping.
     * @return The map of fonts.
     */
    public Map<String, PDFont> getFonts(GlyphList glyphList) throws IOException
    {
        if (fonts == null)
        {
            // at least an empty map will be returned
            // TODO we should return null instead of an empty map
            fonts = new HashMap<String, PDFont>();
            COSDictionary fontsDictionary = (COSDictionary) resources.getDictionaryObject(COSName.FONT);
            if (fontsDictionary == null)
            {
                fontsDictionary = new COSDictionary();
                resources.setItem(COSName.FONT, fontsDictionary);
            }
            else
            {
                Map<COSDictionary, PDFont> seenFonts = new HashMap<COSDictionary, PDFont>();
                for (COSName fontName : fontsDictionary.keySet())
                {
                    COSBase font = fontsDictionary.getDictionaryObject(fontName);

                    // data-000174.pdf contains a font that is a COSArray, looks to be an error in the
                    // PDF, we will just ignore entries that are not dictionaries.
                    if (font instanceof COSDictionary)
                    {
                        // some fonts may appear many times (see test_1fd9a_test.pdf)
                        if (seenFonts.containsKey(font))
                        {
                            fonts.put(fontName.getName(), seenFonts.get(font));
                        }
                        else
                        {
                            PDFont newFont = PDFontFactory.createFont((COSDictionary)font, glyphList);
                            fonts.put(fontName.getName(), newFont);
                            seenFonts.put((COSDictionary) font, newFont);
                        }
                    }
                }
            }
            fontMappings = reverseMap(fonts, PDFont.class);
        }
        return fonts;
    }

    /**
     * This will get the map of PDXObjects that are in the resource dictionary. This will never return null.
     * 
     * @return The map of xobjects.
     */
    public Map<String, PDXObject> getXObjects()
    {
        if (xobjects == null)
        {
            // at least an empty map will be returned
            // TODO we should return null instead of an empty map
            xobjects = new HashMap<String, PDXObject>();

            COSDictionary dict = (COSDictionary) resources.getDictionaryObject(COSName.XOBJECT);

            if (dict == null)
            {
                dict = new COSDictionary();
                resources.setItem(COSName.XOBJECT, dict);
            }
            else
            {
                xobjects = new HashMap<String, PDXObject>();
                for (COSName objName : dict.keySet())
                {
                    PDXObject xobject = null;
                    try
                    {
                        String name = objName.getName();
                        COSBase item = dict.getItem(objName);
                        if (item instanceof COSObject)
                        {
                            COSObject cosObject = (COSObject) item;
                            // add the object number to create an unique identifier
                            name += "#" + cosObject.getObjectNumber().intValue();
                            xobject = PDXObject.createXObject(cosObject.getObject(), name, this);
                        }
                        else
                        {
                            xobject = PDXObject.createXObject(item, name, this);
                        }
                    }
                    catch (IOException exception)
                    {
                        LOG.error("error while creating a xobject", exception);
                    }
                    if (xobject != null)
                    {
                        xobjects.put(objName.getName(), xobject);
                    }
                }
            }
            xobjectMappings = reverseMap(xobjects, PDXObject.class);
        }
        return xobjects;
    }

    /**
     * This will set the map of fonts.
     * 
     * @param fontsValue The new map of fonts.
     */
    public void setFonts(Map<String, PDFont> fontsValue)
    {
        fonts = fontsValue;
        if (fontsValue != null)
        {
            resources.setItem(COSName.FONT, COSDictionaryMap.convert(fontsValue));
            fontMappings = reverseMap(fontsValue, PDFont.class);
        }
        else
        {
            resources.removeItem(COSName.FONT);
            fontMappings = null;
        }
    }

    /**
     * This will set the map of xobjects.
     * 
     * @param xobjectsValue The new map of xobjects.
     */
    public void setXObjects(Map<String, PDXObject> xobjectsValue)
    {
        xobjects = xobjectsValue;
        if (xobjectsValue != null)
        {
            resources.setItem(COSName.XOBJECT, COSDictionaryMap.convert(xobjectsValue));
            xobjectMappings = reverseMap(xobjects, PDXObject.class);
        }
        else
        {
            resources.removeItem(COSName.XOBJECT);
            xobjectMappings = null;
        }
    }

    /**
     * This will get the map of colorspaces. This will return null if the underlying resources dictionary does not have
     * a colorspace dictionary. The keys are string and the values are PDColorSpace objects.
     * 
     * @return The map of colorspaces.
     */
    public Map<String, PDColorSpace> getColorSpaces()
    {
        if (colorspaces == null)
        {
            COSDictionary csDictionary = (COSDictionary) resources.getDictionaryObject(COSName.COLORSPACE);
            if (csDictionary != null)
            {
                colorspaces = new HashMap<String, PDColorSpace>();
                for (COSName csName : csDictionary.keySet())
                {
                    COSBase cs = csDictionary.getDictionaryObject(csName);
                    PDColorSpace colorspace = null;
                    try
                    {
                        colorspace = PDColorSpace.create(cs, null, getPatterns());
                    }
                    catch (IOException exception)
                    {
                        LOG.error("error while creating a colorspace", exception);
                    }
                    if (colorspace != null)
                    {
                        colorspaces.put(csName.getName(), colorspace);
                    }
                }
            }
        }
        return colorspaces;
    }

    /**
     * This will set the map of colorspaces.
     * 
     * @param csValue The new map of colorspaces.
     */
    public void setColorSpaces(Map<String, PDColorSpace> csValue)
    {
        colorspaces = csValue;
        if (csValue != null)
        {
            resources.setItem(COSName.COLORSPACE, COSDictionaryMap.convert(csValue));
        }
        else
        {
            resources.removeItem(COSName.COLORSPACE);
        }
    }

    /**
     * This will get the map of graphic states. This will return null if the underlying resources dictionary does not
     * have a graphics dictionary. The keys are the graphic state name as a String and the values are
     * PDExtendedGraphicsState objects.
     * 
     * @return The map of extended graphic state objects.
     */
    public Map<String, PDExtendedGraphicsState> getGraphicsStates()
    {
        if (graphicsStates == null)
        {
            COSDictionary states = (COSDictionary) resources.getDictionaryObject(COSName.EXT_G_STATE);
            if (states != null)
            {
                graphicsStates = new HashMap<String, PDExtendedGraphicsState>();
                for (COSName name : states.keySet())
                {
                    COSDictionary dictionary = (COSDictionary) states.getDictionaryObject(name);
                    graphicsStates.put(name.getName(), new PDExtendedGraphicsState(dictionary));
                }
            }
        }
        return graphicsStates;
    }

    /**
     * This will set the map of graphics states.
     * 
     * @param states The new map of states.
     */
    public void setGraphicsStates(Map<String, PDExtendedGraphicsState> states)
    {
        graphicsStates = states;
        if (states != null)
        {
            Iterator<String> iter = states.keySet().iterator();
            COSDictionary dic = new COSDictionary();
            while (iter.hasNext())
            {
                String name = (String) iter.next();
                PDExtendedGraphicsState state = states.get(name);
                dic.setItem(COSName.getPDFName(name), state.getCOSObject());
            }
            resources.setItem(COSName.EXT_G_STATE, dic);
        }
        else
        {
            resources.removeItem(COSName.EXT_G_STATE);
        }
    }

    /**
     * Returns the dictionary mapping resource names to property list dictionaries for marked content.
     * 
     * @return the property list
     */
    public PDPropertyList getProperties()
    {
        PDPropertyList retval = null;
        COSDictionary props = (COSDictionary) resources.getDictionaryObject(COSName.PROPERTIES);

        if (props != null)
        {
            retval = new PDPropertyList(props);
        }
        return retval;
    }

    /**
     * Sets the dictionary mapping resource names to property list dictionaries for marked content.
     * 
     * @param props the property list
     */
    public void setProperties(PDPropertyList props)
    {
        resources.setItem(COSName.PROPERTIES, props.getCOSObject());
    }

    /**
     * This will get the map of patterns. This will return null if the underlying resources dictionary does not have a
     * patterns dictionary. The keys are the pattern name as a String and the values are PDAbstractPattern objects.
     * 
     * @return The map of pattern resources objects.
     * 
     * @throws IOException If there is an error getting the pattern resources.
     */
    public Map<String, PDAbstractPattern> getPatterns() throws IOException
    {
        if (patterns == null)
        {
            COSDictionary patternsDictionary = (COSDictionary) resources.getDictionaryObject(COSName.PATTERN);
            if (patternsDictionary != null)
            {
                patterns = new HashMap<String, PDAbstractPattern>();
                for (COSName name : patternsDictionary.keySet())
                {
                    COSDictionary dictionary = (COSDictionary) patternsDictionary.getDictionaryObject(name);
                    patterns.put(name.getName(), PDAbstractPattern.create(dictionary));
                }
            }
        }
        return patterns;
    }

    /**
     * This will set the map of patterns.
     * 
     * @param patternsValue The new map of patterns.
     */
    public void setPatterns(Map<String, PDAbstractPattern> patternsValue)
    {
        patterns = patternsValue;
        if (patternsValue != null)
        {
            Iterator<String> iter = patternsValue.keySet().iterator();
            COSDictionary dic = new COSDictionary();
            while (iter.hasNext())
            {
                String name = iter.next();
                PDAbstractPattern pattern = patternsValue.get(name);
                dic.setItem(COSName.getPDFName(name), pattern.getCOSObject());
            }
            resources.setItem(COSName.PATTERN, dic);
        }
        else
        {
            resources.removeItem(COSName.PATTERN);
        }
    }

    /**
     * This will get the map of shadings. This will return null if the underlying resources dictionary does not have a
     * shading dictionary. The keys are the shading name as a String and the values are PDShading objects.
     * 
     * @return The map of shading resources objects.
     * 
     * @throws IOException If there is an error getting the shading resources.
     */
    public Map<String, PDShading> getShadings() throws IOException
    {
        if (shadings == null)
        {
            COSDictionary shadingsDictionary = (COSDictionary) resources.getDictionaryObject(COSName.SHADING);
            if (shadingsDictionary != null)
            {
                shadings = new HashMap<String, PDShading>();
                for (COSName name : shadingsDictionary.keySet())
                {
                    COSDictionary dictionary = (COSDictionary) shadingsDictionary.getDictionaryObject(name);
                    shadings.put(name.getName(), PDShading.create(dictionary));
                }
            }
        }
        return shadings;
    }

    /**
     * This will set the map of shadings.
     * 
     * @param shadingsValue The new map of shadings.
     */
    public void setShadings(Map<String, PDShading> shadingsValue)
    {
        shadings = shadingsValue;
        if (shadingsValue != null)
        {
            Iterator<String> iter = shadingsValue.keySet().iterator();
            COSDictionary dic = new COSDictionary();
            while (iter.hasNext())
            {
                String name = iter.next();
                PDShading shading = shadingsValue.get(name);
                dic.setItem(COSName.getPDFName(name), shading.getCOSObject());
            }
            resources.setItem(COSName.SHADING, dic);
        }
        else
        {
            resources.removeItem(COSName.SHADING);
        }
    }

    /**
     * Adds the given font to the resources of the current page.
     * 
     * @param font the font to be added
     * @return the font name to be used within the content stream.
     */
    public String addFont(PDFont font) throws IOException
    {
        // use the getter to initialize a possible empty fonts map
        return addFont(font, getNextUniqueKey(getFonts(), "F"));
    }

    /**
     * Adds the given font to the resources of the current page using the given font key.
     * 
     * @param font the font to be added
     * @param fontKey key to used to map to the given font
     * @return the font name to be used within the content stream.
     */
    public String addFont(PDFont font, String fontKey) throws IOException
    {
        if (fonts == null)
        {
            // initialize fonts map
            getFonts();
        }

        String fontMapping = fontMappings.get(font);
        if (fontMapping == null)
        {
            fontMapping = fontKey;
            fontMappings.put(font, fontMapping);
            fonts.put(fontMapping, font);
            addFontToDictionary(font, fontMapping);
        }
        return fontMapping;
    }

    private void addFontToDictionary(PDFont font, String fontName)
    {
        COSDictionary fontsDictionary = (COSDictionary) resources.getDictionaryObject(COSName.FONT);
        fontsDictionary.setItem(fontName, font);
    }

    /**
     * Adds the given XObject to the resources of the current the page.
     * 
     * @param xobject the XObject to be added
     * @param prefix the prefix to be used for the name
     * 
     * @return the XObject name to be used within the content stream.
     */
    public String addXObject(PDXObject xobject, String prefix)
    {
        if (xobjects == null)
        {
            // initialize XObject map
            getXObjects();
        }
        String objMapping = xobjectMappings.get(xobject);
        if (objMapping == null)
        {
            objMapping = getNextUniqueKey(xobjects, prefix);
            xobjectMappings.put(xobject, objMapping);
            xobjects.put(objMapping, xobject);
            addXObjectToDictionary(xobject, objMapping);
        }
        return objMapping;
    }

    // generates unique IDs for new resource entries
    private final String getNextUniqueKey( Map<String,?> map, String prefix )
    {
        int counter = 0;
        while( map != null && map.get( prefix+counter ) != null )
        {
            counter++;
        }
        return prefix+counter;
    }

    /**
     * Remove the xobject with given name.
     * 
     * @param xobjectName the name of the xobject to be removed.
     */
    public void removeXObject(String xobjectName)
    {
        COSDictionary xobjectsDictionary = (COSDictionary) resources.getDictionaryObject(COSName.XOBJECT);
        xobjectsDictionary.removeItem(COSName.getPDFName(xobjectName));
        if (xobjects != null && xobjects.containsKey(xobjectName))
        {
        	xobjectMappings.remove(xobjects.get(xobjectName));
        	xobjects.remove(xobjectName);
        }
    }
    
    /**
     * Remove the font with given name.
     * 
     * @param fontName the name of the font to be removed.
     */
    public void removeFont(String fontName)
    {
        COSDictionary xobjectsDictionary = (COSDictionary) resources.getDictionaryObject(COSName.FONT);
        xobjectsDictionary.removeItem(COSName.getPDFName(fontName));
        if (fonts != null && fonts.containsKey(fontName))
        {
        	fontMappings.remove(fonts.get(fontName));
        	fonts.remove(fontName);
        }
    }

    private void addXObjectToDictionary(PDXObject xobject, String xobjectName)
    {
        COSDictionary xobjectsDictionary = (COSDictionary) resources.getDictionaryObject(COSName.XOBJECT);
        xobjectsDictionary.setItem(xobjectName, xobject);
    }

    private <T> Map<T, String> reverseMap(Map<String, T> map, Class<T> keyClass)
    {
        Map<T, String> reversed = new java.util.HashMap<T, String>();
        for (Map.Entry<String, T> entry : map.entrySet())
        {
            reversed.put(keyClass.cast(entry.getValue()), (String) entry.getKey());
        }
        return reversed;
    }

}
