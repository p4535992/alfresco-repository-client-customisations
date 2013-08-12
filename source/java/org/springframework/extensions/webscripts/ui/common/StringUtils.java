/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.springframework.extensions.webscripts.ui.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.htmlparser.Attribute;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.PrototypicalNodeFactory;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.ParserException;

/**
 * Class containing misc helper methods for managing Strings.
 * 
 * NOTE: Extracted from org.alfresco.web.ui.common.Utils;
 * 
 * @author Kevin Roast
 */
public class StringUtils
{
	private static final Log logger = LogFactory.getLog(StringUtils.class);

	private static final String ATTR_STYLE = "STYLE";
	private static final String ATTR_SRC = "SRC";
    private static final String ATTR_DYNSRC = "DYNSRC";
    private static final String ATTR_LOWSRC = "LOWSRC";
	private static final String ATTR_HREF = "HREF";
    private static final String ATTR_BACKGROUND = "BACKGROUND";
	private static final String ATTR_ON_PREFIX = "ON";
    
    protected static final Set<String> unsafeAttrs = new HashSet<String>();
    static
    {
        unsafeAttrs.add(ATTR_SRC);
        unsafeAttrs.add(ATTR_DYNSRC);
        unsafeAttrs.add(ATTR_LOWSRC);
        unsafeAttrs.add(ATTR_HREF);
        unsafeAttrs.add(ATTR_BACKGROUND);
    }

	protected static final Set<String> safeTags = new HashSet<String>();
	static
	{
		safeTags.add("EM");
		safeTags.add("STRONG");
		safeTags.add("SUP");
		safeTags.add("SUB");
		safeTags.add("P");
		safeTags.add("B");
		safeTags.add("I");
		safeTags.add("BR");
		safeTags.add("UL");
		safeTags.add("OL");
		safeTags.add("LI");
		safeTags.add("H1");
		safeTags.add("H2");
		safeTags.add("H3");
		safeTags.add("H4");
		safeTags.add("H5");
		safeTags.add("H6");
		safeTags.add("SPAN");
		safeTags.add("DIV");
		safeTags.add("A");
		safeTags.add("IMG");
		safeTags.add("FONT");
		safeTags.add("TABLE");
		safeTags.add("THEAD");
		safeTags.add("TBODY");
		safeTags.add("TR");
		safeTags.add("TH");
		safeTags.add("TD");
		safeTags.add("HR");
		safeTags.add("DT");
		safeTags.add("DL");
		safeTags.add("DT");
		safeTags.add("PRE");
		safeTags.add("BLOCKQUOTE");
		safeTags.add("U");
		safeTags.add("STRIKE");
		safeTags.add("CAPTION");
	}

	/**
	 * Encodes the given string, so that it can be used within an HTML page.
	 * 
	 * @param string     the String to convert
	 */
    public static String encode(final String string)
	{
		if (string == null)
		{
			return "";
		}

		StringBuilder sb = null;      //create on demand
		String enc;
		char c;
		for (int i = 0; i < string.length(); i++)
		{
			enc = null;
			c = string.charAt(i);
			switch (c)
			{
			case '"': enc = "&quot;"; break;    //"
			case '&': enc = "&amp;"; break;     //&
			case '<': enc = "&lt;"; break;      //<
			case '>': enc = "&gt;"; break;      //>

			case '\u20AC': enc = "&euro;";  break;
			case '\u00AB': enc = "&laquo;"; break;
			case '\u00BB': enc = "&raquo;"; break;
			case '\u00A0': enc = "&nbsp;"; break;

			default:
				if (((int)c) >= 0x80)
				{
					//encode all non basic latin characters
					enc = "&#" + ((int)c) + ";";
				}
				break;
			}

			if (enc != null)
			{
				if (sb == null)
				{
					String soFar = string.substring(0, i);
					sb = new StringBuilder(i + 16);
					sb.append(soFar);
				}
				sb.append(enc);
			}
			else
			{
				if (sb != null)
				{
					sb.append(c);
				}
			}
		}

		if (sb == null)
		{
			return string;
		}
		else
		{
			return sb.toString();
		}
	}

	/**
	 * Crop a label within a SPAN element, using ellipses '...' at the end of label and
	 * and encode the result for HTML output. A SPAN will only be generated if the label
	 * is beyond the default setting of 32 characters in length.
	 * 
	 * @param text       to crop and encode
	 * 
	 * @return encoded and cropped resulting label HTML
	 */
	public static String cropEncode(String text)
	{
		return cropEncode(text, 32);
	}

	/**
	 * Crop a label within a SPAN element, using ellipses '...' at the end of label and
	 * and encode the result for HTML output. A SPAN will only be generated if the label
	 * is beyond the specified number of characters in length.
	 * 
	 * @param text       to crop and encode
	 * @param length     length of string to crop too
	 * 
	 * @return encoded and cropped resulting label HTML
	 */
	public static String cropEncode(String text, int length)
	{
		if (text.length() > length)
		{
			String label = text.substring(0, length - 3) + "...";
			StringBuilder buf = new StringBuilder(length + 32 + text.length());
			buf.append("<span title=\"")
			.append(StringUtils.encode(text))
			.append("\">")
			.append(StringUtils.encode(label))
			.append("</span>");
			return buf.toString();
		}
		else
		{
			return StringUtils.encode(text);
		}
	}

	/**
	 * Encode a string to the %AB hex style JavaScript compatible notation.
	 * Used to encode a string to a value that can be safely inserted into an HTML page and
	 * then decoded (and probably eval()ed) using the unescape() JavaScript method.
	 * 
	 * @param s      string to encode
	 * 
	 * @return %AB hex style encoded string
	 */
	public static String encodeJavascript(String s)
	{
		StringBuilder buf = new StringBuilder(s.length() * 3);
		for (int i=0; i<s.length(); i++)
		{
			char c = s.charAt(i);
			int iChar = (int)c;
			buf.append('%');
			buf.append(Integer.toHexString(iChar));
		}
		return buf.toString();
	}

	/**
	 * Strip unsafe HTML tags from a string - only leaves most basic formatting tags
	 * and encodes the remaining characters.
	 * 
	 * @param s HTML string to strip tags from
	 * 
	 * @return safe string
	 */
	public static String stripUnsafeHTMLTags(String s)
	{
		return stripUnsafeHTMLTags(s, true);
	}

	/**
	 * Strip unsafe HTML tags from a string - only leaves most basic formatting tags
	 * and optionally encodes or strips the remaining characters.
	 * 
	 * @param s         HTML string to strip tags from
	 * @param encode    if true then encode remaining html data
	 * 
	 * @return safe string
	 */
	public static String stripUnsafeHTMLTags(String s, boolean encode)
	{
		// Replace troublesome style attributes if there are any present.
		String result = null;

		result = replaceStyleAttributes(s);

		int strippedLength;

		// perform a multi-pass strip until the length of the result stays fixed
		try
		{
			do
			{
				strippedLength = result.length();

				StringBuilder buf = new StringBuilder(result.length());

				Parser parser = Parser.createParser(result, "UTF-8");
				PrototypicalNodeFactory factory = new PrototypicalNodeFactory();
				parser.setNodeFactory(factory);
				NodeIterator itr = parser.elements();
				processNodes(buf, itr, false);

				result = buf.toString();
			} while (strippedLength != result.length());

			// final text element encoding pass if required 
			if (encode)
			{
				StringBuilder buf = new StringBuilder(result.length());

				Parser parser = Parser.createParser(result, "UTF-8");
				PrototypicalNodeFactory factory = new PrototypicalNodeFactory();
				parser.setNodeFactory(factory);
				NodeIterator itr = parser.elements();
				processNodes(buf, itr, true);

				result = buf.toString();
			}
		}
		catch (ParserException e)
		{
			// return the only safe value if this occurs
			return "";
		}

		return result;
	}

	/**
	 * Pattern which will match something that looks like a CSS colour value
	 */
	private static final Pattern STYLE_EXTRACTOR_COLOUR = Pattern.compile("(?:^|\\s)((?:#[0-9a-f]{6})|(?:#[0-9a-f]{3})|" +
			"(?:rgb\\s*\\(\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*,\\s*\\d{1,3}\\s*\\))|(?:rgb\\s*\\(\\s*\\d{1,3}%\\s*,\\s*\\d{1,3}%\\s*,\\s*\\d{1,3}%\\s*\\))" +
			"(?:aliceblue)|(?:antiquewhite)|(?:aqua)|(?:aquamarine)|(?:azure)|(?:beige)|(?:bisque)|(?:black)|" +
			"(?:blanchedalmond)|(?:blue)|(?:blueviolet)|(?:brown)|(?:burlywood)|(?:cadetblue)|(?:chartreuse)|(?:chocolate)|" +
			"(?:coral)|(?:cornflowerblue)|(?:cornsilk)|(?:crimson)|(?:cyan)|(?:darkblue)|(?:darkcyan)|(?:darkgoldenrod)|(?:darkgray)|(?:darkgrey)|" +
			"(?:darkgreen)|(?:darkkhaki)|(?:darkmagenta)|(?:darkolivegreen)|(?:darkorange)|(?:darkorchid)|(?:darkred)|(?:darksalmon)|(?:darkseagreen)|" +
			"(?:darkslateblue)|(?:darkslategray)|(?:darkslategrey)|(?:darkturquoise)|(?:darkviolet)|(?:deeppink)|(?:deepskyblue)|(?:dimgray)|" +
			"(?:dimgrey)|(?:dodgerblue)|(?:firebrick)|(?:floralwhite)|(?:forestgreen)|(?:fuchsia)|(?:gainsboro)|(?:ghostwhite)|(?:gold)|(?:goldenrod)|" +
			"(?:gray)|(?:grey)|(?:green)|(?:greenyellow)|(?:honeydew)|(?:hotpink)|(?:indianred)|(?:indigo)|(?:ivory)|(?:khaki)|(?:lavender)|" +
			"(?:lavenderblush)|(?:lawngreen)|(?:lemonchiffon)|(?:lightblue)|(?:lightcoral)|(?:lightcyan)|(?:lightgoldenrodyellow)|(?:lightgray)|" +
			"(?:lightgrey)|(?:lightgreen)|(?:lightpink)|(?:lightsalmon)|(?:lightseagreen)|(?:lightskyblue)|(?:lightslategray)|(?:lightslategrey)|" +
			"(?:lightsteelblue)|(?:lightyellow)|(?:lime)|(?:limegreen)|(?:linen)|(?:magenta)|(?:maroon)|(?:mediumaquamarine)|(?:mediumblue)|" +
			"(?:mediumorchid)|(?:mediumpurple)|(?:mediumseagreen)|(?:mediumslateblue)|(?:mediumspringgreen)|(?:mediumturquoise)|(?:mediumvioletred)|" +
			"(?:midnightblue)|(?:mintcream)|(?:mistyrose)|(?:moccasin)|(?:navajowhite)|(?:navy)|(?:oldlace)|(?:olive)|(?:olivedrab)|(?:orange)|(?:orangered)|" +
			"(?:orchid)|(?:palegoldenrod)|(?:palegreen)|(?:paleturquoise)|(?:palevioletred)|(?:papayawhip)|(?:peachpuff)|(?:peru)|(?:pink)|(?:plum)|" +
			"(?:powderblue)|(?:purple)|(?:red)|(?:rosybrown)|(?:royalblue)|(?:saddlebrown)|(?:salmon)|(?:sandybrown)|(?:seagreen)|(?:seashell)|(?:sienna)|" +
			"(?:silver)|(?:skyblue)|(?:slateblue)|(?:slategray)|(?:slategrey)|(?:snow)|(?:springgreen)|(?:steelblue)|(?:tan)|(?:teal)|(?:thistle)|(?:tomato)|" +
			"(?:turquoise)|(?:violet)|(?:wheat)|(?:white)|(?:whitesmoke)|(?:yellow)|(?:yellowgreen)|(?:activeborder)|(?:activecaption)|" +
			"(?:appworkspace)|(?:background)|(?:buttonface)|(?:buttonhighlight)|(?:buttonshadow)|(?:buttontext)|(?:captiontext)|" +
			"(?:graytext)|(?:highlight)|(?:highlighttext)|(?:inactiveborder)|(?:inactivecaption)|(?:inactivecaptiontext)|" +
			"(?:infobackground)|(?:infotext)|(?:menu)|(?:menutext)|(?:scrollbar)|(?:threeddarkshadow)|(?:threedface)|(?:threedhighlight)|" +
			"(?:threedlightshadow)|(?:threedshadow)|(?:window)|(?:windowframe)|(?:windowtext))", Pattern.CASE_INSENSITIVE);	
	
	/**
	 * Pattern which will match a CSS length value
	 */
	private static final Pattern STYLE_EXTRACTOR_LENGTH = Pattern.compile("(?:^|\\s)((?:auto)|(?:[+-]?\\d+(?:\\.\\d+)?(px|em|ex|%|in|cm|mm|pt|pc)?))",
			Pattern.CASE_INSENSITIVE);
	
	private static final StyleReplacementDefinition[] TABLE_REPLACEMENTS = {
		new StyleReplacementDefinition("background-color", "bgcolor", STYLE_EXTRACTOR_COLOUR),
		new StyleReplacementDefinition("background", "bgcolor", STYLE_EXTRACTOR_COLOUR),
		new StyleReplacementDefinition("border-color", "bordercolor", STYLE_EXTRACTOR_COLOUR),
		new StyleReplacementDefinition("border", "bordercolor", STYLE_EXTRACTOR_COLOUR),
		new StyleReplacementDefinition("width", "width", STYLE_EXTRACTOR_LENGTH),
		new StyleReplacementDefinition("height", "height", STYLE_EXTRACTOR_LENGTH)
	};
	private static final String STYLE_ATTR_START = "style=";
	private static final String TABLE_TAG_START = "<table";
	
	private static int getStyleStart(String html, int startPoint)
	{
		int out = html.indexOf(STYLE_ATTR_START+"\"", startPoint);
		if (out==-1)
		{
			out=html.indexOf(STYLE_ATTR_START+"'", startPoint);
		}
		return out;
	}
	
	/**
	 * Get the end of a style definition.  We end a style according to the following preferences:
	 * <ol>
	 * <li> ;" or  ;' whichever comes first</li>
	 * <li> The second double quote or single quote found</li>
	 * <li> > </li>
	 * <li> The end of the String </li>
	 * </ol>
	 * The algorithm is greedy, so it prefers to consume 100 characters and find a ;" than consume 10 to find ;'
	 * @param html HTML String to inspect
	 * @param fromIndex Start looking for the end of a style definition from this point.  This is usually the start of the style definition
	 * @return Index into HTML
	 */
	private static int getStyleEnd(String html, int fromIndex)
	{
		int outDouble = html.indexOf(";\"", fromIndex);
		int outSingle=html.indexOf(";'", fromIndex);
		int out=outDouble;
		if (outSingle<outDouble)
		{
			out=outSingle;
		}
		//Because the two previous patterns are 2 chars long, if we matched either we need to add one to the index
		if (out>-1)
		{
			return ++out; //We know that we'll return at this point because out>1
		}
		//else
		if (out==-1)
		{
			int dblQuoteIdx = html.indexOf("\"", fromIndex);
			int singleQuoteIdx = html.indexOf("'", fromIndex);
			int idx=dblQuoteIdx;
			if (dblQuoteIdx==-1 || (singleQuoteIdx<dblQuoteIdx && singleQuoteIdx!=-1))
			{
				idx=singleQuoteIdx;
			}
			out=html.indexOf("\"", idx+1);
			if (out==-1)
			{
				out=html.indexOf("'", idx+1);
			}
		}
		if (out==-1)
		{
			out=html.indexOf(">",fromIndex);
		}
		if (out==-1)
		{
			out=html.length()-1;
		}
		return out;
	}

	/**
	 * This method is only required because of a deficiency in the implementation of tinymce, which inserts
	 * inline style attributes for some formatting (background colours on tables and text) even when it is 
	 * configured not to.  When tinymce fixes itself, or up versions, whatever, this method should be removed
	 * as this is not the ideal place for this implementation.
	 * 
	 * Note that this method will strip any part of text that looks like a style attribute, even if it's part 
	 * of the body of an HTML element.  This is presently judged to be the safest option as it prevents an attacker
	 * from hiding executable code inside a confluence of angle brackets we can't parse.  However, this is a limitation
	 * and should ideally be revisited.
	 * 
	 * @param toRemoveStyleAttr         HTML string to replace style attributes from
	 * 
	 * @return safe string
	 */
	public static String replaceStyleAttributes(String stringToBeSanitised) {

		// The style attribute has to be present, as does the table tag, without either nothing needs to
		// be done with the string.
		
		//Do a quick replacement so that standard tables with borders where the colour isn't specified render with black borders in IE6
		stringToBeSanitised=stringToBeSanitised.replaceAll("<[tT][aA][Bb][Ll][eE] [bB][oO][rR][Dd][eE][rR]=.1.>", "<table border='1' bordercolor='black'>");
		
		StringBuffer sb = null;
		
		if (stringToBeSanitised.indexOf("<p style=\"text-align: ")!=-1)
		{
			stringToBeSanitised=stringToBeSanitised.replaceAll("<p style=\"text-align: (.*?);\">", "<p align=\"$1\">");
		}

		if (getStyleStart(stringToBeSanitised,0) != -1 && stringToBeSanitised.toLowerCase().indexOf(TABLE_TAG_START) != -1) {

			// There is at least one table with a style attribute.
			int startIndex = 0;
			int endIndex = 0;
			int progressThroughString = 0;
			String afterStyle=null;

			sb = new StringBuffer();

			while ((startIndex = getStyleStart(stringToBeSanitised, progressThroughString)) != -1) {

				
				// Append the token up to the latest style attribute
				sb.append(stringToBeSanitised.substring(progressThroughString, startIndex));

				// Now set the end index to be the end of the latest style attribute.
				endIndex = getStyleEnd(stringToBeSanitised, startIndex) + 1;
				if (endIndex<startIndex)
				{
					logger.warn("endIndex was "+endIndex+" when startIndex was "+startIndex+" for "+stringToBeSanitised);
					throw new StringIndexOutOfBoundsException("Could not parse style String (indexes: "+startIndex+","+endIndex+") for "+stringToBeSanitised);
				}
				afterStyle=stringToBeSanitised.substring(endIndex + 1, stringToBeSanitised.length());

				// The style string is contained within the input string - deal with it.
				final String styleAttribute = stringToBeSanitised.substring(startIndex, endIndex);
				final String styleAttributeLC=styleAttribute.toLowerCase();
				
				for (int i = 0; i < TABLE_REPLACEMENTS.length; i++)
				{
					StyleReplacementDefinition replacement = TABLE_REPLACEMENTS[i];
				
					if ((styleAttributeLC.indexOf(replacement.getStyle())) != -1) 
					{
	
						if (sb == null) {
							sb = new StringBuffer();
						}
	
						try {
							String value = getStyleValue(styleAttribute, replacement.getStyle(), replacement.getStyleExtractor());
							if(value != null) {
								sb.append(replacement.getAttribute() + "=\""+value+"\" ");
							}
						}
						//Depending on the HTML passed in, we can't guarantee something won't go wrong during style attribute
						//processing.  If something _does_ go wrong, then we just stop processing, which will cause the rest of
						//the style attribute to be safely ignored
						catch (StringIndexOutOfBoundsException e)
						{
							logger.warn("Could not process the following String: "+styleAttribute
									+"\nRoot Cause: "+e);
						}
	
					} 

					progressThroughString = endIndex + 1;
				}
				//If there isn't a close open angle bracket, or, if there is, it's preceded by an open angle bracket, close the angle bracket
				if (afterStyle!=null && (afterStyle.indexOf('>')==-1 || (afterStyle.indexOf('<')!=-1 && afterStyle.indexOf('<') < afterStyle.indexOf('>'))))
				{
					sb.append(">");
				}
			}

			sb.append(afterStyle);

		}

		// If the stringbuffer has been initialised then its contents should be returned, otherwise the original string.
		if (sb != null) {
			return sb.toString();
		}
		else {
			return stringToBeSanitised;		
		}

	}


	/**
	 * Given a String representing a style attribute, use a given {@link Pattern} to extract a certain style value from
	 * the given attribute.
	 * @param styleAttribute String representing the style attribute of an HTML element, usually a table
	 * @param styleName name of the style for whom we are trying to extract the value
	 * @param styleExtractor A regex pattern to be used to extract a part of the style from the attribute. The pattern should contain (at least) one capturing group as the first capturing group match will be returned.
	 * @return String indicating the specified value of that style Attribute, or null if no value is specified.
	 * Behaviour for invalid attribute values is generally undefined, but will often result in the return of a value that
	 * has been truncated such that it is now syntactically valid 
	 */
	protected static String getStyleValue(String styleAttribute, String styleName, final Pattern styleExtractor)
	{
		int startOfValue = styleAttribute.toLowerCase().indexOf(styleName.toLowerCase());
		if (startOfValue==-1)
		{
			return null; //If not present in style, return null
		}
		
		startOfValue += styleName.length();
		
		// Find the next semi-colon. This will determine the end of the css value
		int endOfValue = styleAttribute.indexOf(';', startOfValue);
		
		if(-1 == endOfValue) {
			// If we haven't found a semicolon, then set the end to the end of the string
			endOfValue = styleAttribute.length();
		}

		Matcher matcher = styleExtractor.matcher(styleAttribute);
		matcher.region(startOfValue, endOfValue);
		
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return null;
		}
	}

	/**
	 * Recursively process HTML nodes to strip unsafe HTML.
	 * 
	 * @param buf       Buffer to write to
	 * @param itr       Node iterator to process
	 * @param encode    True to HTML encode characters within text nodes
	 * 
	 * @throws ParserException
	 */
	private static void processNodes(StringBuilder buf, NodeIterator itr, boolean encode) throws ParserException
	{

		if (logger.isDebugEnabled())
		{
			logger.debug("Entered processNodes");
		}
		while (itr.hasMoreNodes())
		{
			Node node = itr.nextNode();
			if (node instanceof Tag)
			{
				// get the tag and process it and its attributes
				Tag tag = (Tag)node;

				// get the tag name - automatically converted to upper case
				String tagname = tag.getTagName();

				// only allow a whitelist of safe tags i.e. remove SCRIPT etc.
				if (safeTags.contains(tagname))
				{
					if (logger.isDebugEnabled())
					{
						logger.debug(tagname + " is safe");
					}
					// process each attribute name - removing:
					// all "on*" javascript event handlers
					// all "style" attributes - as could contain 'expression' javascript for IE
					Vector<Attribute> attrs = tag.getAttributesEx();

					// tag attributes contain the tag name at a minimum
					if (attrs.size() > 1)
					{
						buf.append('<').append(tagname);
						for (Attribute attr : attrs)
						{
							String name = attr.getName();
							if (name != null)
							{
								String nameUpper = name.toUpperCase();

								//Remove leading /s from onXXX attributes
								while (nameUpper.startsWith("/ON"))
								{
									nameUpper=nameUpper.replaceFirst("/ON", "ON");
								}

								if (logger.isDebugEnabled())
								{
									logger.debug("Examining attr: "+nameUpper);
								}
								if (!tagname.equals(nameUpper)) // ignore tag name itself
								{
									// strip character ignored by some browsers - can be used to form XSS attacks
									// i.e. allow "onclick=" by using "/onclick=" or even "//onclick="
									String safeName = nameUpper.replaceAll("/", "");
									// found a tag attribute for output
									// test for known attributes to remove
									if (!safeName.startsWith(ATTR_ON_PREFIX) && !safeName.equals(ATTR_STYLE))
									{
										String value = attr.getRawValue();
										// sanitise src and href attributes
                                        if (unsafeAttrs.contains(safeName))
										{
											// test the attribute value for XSS - the procedure is:
											// . first trim the string to remove whitespace at front (hides attack)
											// . test for encoded characters at start of attribute (hides javascript)
											// . test for direct javascript: attack
											if (attr.getValue() != null)
											{
												String test = attr.getValue().trim();
												if (test.length() > 2)
												{
													// handle that html encoder doesn't know about grave accent
													if (test.startsWith("`"))
													{
														test = test.substring(1);
													}
                                                    // all encoded attacks start with &# sequence - assume to be an attack
                                                    // there are no valid protocols starting with "J" - assume attack
                                                    // the "background" attribute is also vulnerable - no web colors start with "J"
                                                    // on IE6 "vbscript" can be used - no colors or protocols start with "VB"
													if (test.startsWith("&#") ||
                                                        test.substring(0, 1).toUpperCase().charAt(0) == 'J' ||
                                                        test.substring(0, 2).toUpperCase().startsWith("VB"))
													{
														value = "\"\"";
													}
												}
											}
										}
										buf.append(' ').append(name).append('=').append(value);
									}
								}
							}
						}

						// close the tag after attribute output and before child output
						buf.append('>');

						// process children if they exist, else end tag will be processed in next iteration
						if (tag.getChildren() != null)
						{
							processNodes(buf, tag.getChildren().elements(), encode);
							buf.append(tag.getEndTag().toHtml());
						}
					}
					else
					{
						// process children if they exist - or output end tag if not empty
						if (tag.getChildren() != null)
						{
							buf.append('<').append(tagname).append('>');
							processNodes(buf, tag.getChildren().elements(), encode);
							buf.append(tag.getEndTag().toHtml());
						}
						else
						{
							buf.append(tag.toHtml());
						}
					}
				}
			}
			else if (node instanceof Text)
			{
				String txt = ((Text)node).toPlainTextString();
				buf.append(encode ? encode(txt): txt);
			}
		}
	}

	/**
	 * Replace one string instance with another within the specified string
	 * 
	 * @param str
	 * @param repl
	 * @param with
	 * 
	 * @return replaced string
	 */
	public static String replace(String str, String repl, String with)
	{
		if (str == null)
		{
			return null;
		}

		int lastindex = 0;
		int pos = str.indexOf(repl);

		// If no replacement needed, return the original string
		// and save StringBuffer allocation/char copying
		if (pos < 0)
		{
			return str;
		}

		int len = repl.length();
		int lendiff = with.length() - repl.length();
		StringBuilder out = new StringBuilder((lendiff <= 0) ? str.length() : (str.length() + (lendiff << 3)));
		for (; pos >= 0; pos = str.indexOf(repl, lastindex = pos + len))
		{
			out.append(str.substring(lastindex, pos)).append(with);
		}

		return out.append(str.substring(lastindex, str.length())).toString();
	}

	/**
	 * Remove all occurances of a String from a String
	 * 
	 * @param str     String to remove occurances from
	 * @param match   The string to remove
	 * 
	 * @return new String with occurances of the match removed
	 */
	public static String remove(String str, String match)
	{
		int lastindex = 0;
		int pos = str.indexOf(match);

		// If no replacement needed, return the original string
		// and save StringBuffer allocation/char copying
		if (pos < 0)
		{
			return str;
		}

		int len = match.length();
		StringBuilder out = new StringBuilder(str.length());
		for (; pos >= 0; pos = str.indexOf(match, lastindex = pos + len))
		{
			out.append(str.substring(lastindex, pos));
		}

		return out.append(str.substring(lastindex, str.length())).toString();
	}

	/**
	 * Replaces carriage returns and line breaks with the &lt;br&gt; tag.
	 * 
	 * @param str The string to be parsed
	 * @return The string with line breaks removed
	 */
	public static String replaceLineBreaks(String str, boolean xhtml)
	{
		String replaced = null;

		if (str != null)
		{
			try
			{
				StringBuilder parsedContent = new StringBuilder(str.length() + 32);
				BufferedReader reader = new BufferedReader(new StringReader(str));
				String line = reader.readLine();
				while (line != null)
				{
					parsedContent.append(line);
					line = reader.readLine();
					if (line != null)
					{
						parsedContent.append(xhtml ? "<br/>" : "<br>");
					}
				}

				replaced = parsedContent.toString();
			}
			catch (IOException ioe)
			{
				if (logger.isWarnEnabled())
				{
					logger.warn("Failed to replace line breaks in string: " + str);
				}
			}
		}

		return replaced;
	}
    
    /**
     * Join an array of values into a String value
     * 
     * @param value non-null array of objects - toString() of each value is used
     * 
     * @return concatenated string value
     */
    public static String join(final Object[] value)
    {
        return join(value, null);
    }
    
    /**
     * Join an array of values into a String value using supplied delimiter between each.
     * 
     * @param value non-null array of objects - toString() of each value is used
     * @param delim delimiter value to apply between each value - null indicates no delimiter
     * 
     * @return concatenated string value
     */
    public static String join(final Object[] value, final String delim)
    {
        final StringBuilder buf = new StringBuilder(value.length << 4);
        for (int i=0; i<value.length; i++)
        {
            if (i != 0 && delim != null)
            {
                buf.append(delim);
            }
            buf.append(value[i] != null ? value[i].toString() : "");
        }
        return buf.toString();
    }
	
	private static class StyleReplacementDefinition
	{
		private final String styleName;
		private final String attributeName;
		private final Pattern styleExtractor;
		
		public StyleReplacementDefinition(final String style, final String attribute, final Pattern styleExtractor)
		{
			this.styleName=style+":";
			this.attributeName=attribute;
			this.styleExtractor = styleExtractor;
		}
		
		public String getStyle()
		{
			return styleName;
		}
		
		public String getAttribute()
		{
			return attributeName;
		}
		
		public Pattern getStyleExtractor()
		{
			return styleExtractor;
		}
	}
}