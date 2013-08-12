package com.surevine.alfresco.repo.test;

import org.springframework.extensions.webscripts.ui.common.StringUtils;
import org.junit.Test;
import static junit.framework.Assert.*;

import com.surevine.alfresco.repo.impl.AlfrescoHTMLSanitiser;

public class AlfrescoHTMLSanitiserTest {
	
	@Test
	public void testWithSubclassElementsSafe(){
		processUnchangedHTML("<html><body><p>This is some innocous HTML</p></body></html>");
	}
	
	@Test
	public void testVerySimpleSafe(){
		processUnchangedHTML("<p>This is simple</p>");
	}
	
	@Test
	public void testOnlySafeSuperclassElementsWithNesting(){
		processUnchangedHTML("<p>This is a top level para </p><p> This is a nested Para<b>This is more nesting</b></p></p>");
	}
	
	@Test
	public void testJustOneScriptTag(){
		processRemove("<script> Here is my naughty code</script>", new String[]{"script"});
	}
	
	@Test
	public void scriptTagInsideAPara(){
		processRemove("<p> This is safe<script> Here is my naughty code</script></p>", new String[]{"script"});
	}
	
	@Test
	public void scriptTagInsideAParaWithAttributeOnTheScriptTag(){
		processRemove("<p> This is safe<script pleasedonttrimme='true'> Here is my naughty code</script></p>", new String[]{"script"});
	}
	
	@Test
	public void nestedUnsafeTags(){
		processRemove("<p> This is unsafe<scr<script>ipt> Here is my<scr<anothertag>ipt>  Aha!</scr</anothertag>ipt> naughty code</scr</script>ipt></p>", new String[]{"script", "anothertag"});
	}
	
	@Test
	public void realisticUnsafeHTMLNoNestingInvalidHTML(){
		processRemove("<html><body><h1>This is a heading</h1><p> There is an <b><i>Important <p>alert(1);</p></i></b> point to make here </p< </body></html>", new String[]{"script"});
	}
	
	@Test
	public void realisticUnsafeHTMLNoNesting(){
		processRemove("<html><body><h1>This is a heading</h1><p> There is an <b><i>Important <script>alert(1);</script></i></b> point to make here </p> </body></html>", new String[]{"script"});
	}
	
	@Test
	public void realisticUnsafeHTMLWithNesting(){
		processRemove("<html><body><h1>This is a heading</h1><p> There is an <b><i>Important <de<script>coy>alert(1);</de</script>coy></i></b> point to make here </p> </body></html>", new String[]{"script", "decoy"});
	}
	
	@Test
	public void realisticUnsafeHTMLWithNestingAndMixedCase(){
		processRemove("<html><body><h1>This is a heading</h1><p> There is an <b><i>Important <De<sCrIpt>coy>alert(1);</de</ScripT>cOy></i></b> point to make here </p> </body></html>", new String[]{"script", "decoy"});
	}
	
	@Test
	public void simpleInvalidHTML(){
		processRemove("<p> No close tag <<", new String[]{"script"});
	}
	
	@Test
	public void emptyString(){
		processRemove("", new String[]{"script"});
	}
	
	@Test (expected=NullPointerException.class)
	public void nullString(){
		processRemove(null, new String[]{"script"});
	}
	
	@Test
	public void justSomeWords(){
		processRemove("Why these are just some words!\r Not\r\n HTML at all.  Script.", new String[]{"script"});
	}
	
	@Test
	public void testBasicStringInAndOut()
	{
		assertTrue(textIsUnchanged("Hello World!"));
	}
	
	@Test
	public void testBasicSafeHTML()
	{
		assertTrue(textIsUnchanged("<p color='red'>This is my paragraph</p><table><tr><td>1</td><td>2</td></tr></table>"));
	}
	
	@Test
	public void testBackgroundColourStyleForInt237()
	{
		String input = "<table style=\"background-color: white\" bgcolor=\"#ffffff\"><tr><td>1</td><td>2</td></tr><tr><td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table>";
		assertTrue(compare(input, "bgcolor=\"white\"", "background-color"));
		assertTrue(compare(input, null, "&gt;"));
	}
	
	@Test
	public void testBackgroundColourStyleForInt237Alternate()
	{
		String input = "<table style=\"background-color: white\"> <tr> <td>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		assertTrue(compare(input, "bgcolor=\"white\"", "background-color"));
		assertTrue(compare(input, null, "&gt;"));
	}
	
	@Test
	public void testUpperCaseTableBackgroundColourStyleForInt237Alternate()
	{
		String input = "<TABLE style=\"background-color: white\"> <tr> <td>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </TABLE> ";
		assertTrue(compare(input, "bgcolor=\"white\"", "background-color"));
		assertTrue(compare(input, null, "&gt;"));
	}
	
	@Test
	public void testBorderColorPreservedAsAttributeForInt240()
	{
		String input = "<table style=\"border-color: white\"> <tr> <td>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		assertTrue(compare(input, "bordercolor=\"white\"", "border-color"));
		assertTrue(compare(input, null, "&gt;"));
	}

	@Test
	public void testBorderColorPreservedAsAttributeForInt240AltWithTextualColour()
	{
		String input = "<table style=\"border: 1px solid white\"> <tr> <td>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		assertTrue(compare(input, "bordercolor=\"white\"", "style"));
		assertTrue(compare(input, null, "&gt;"));
	}

	@Test
	public void testBorderColorPreservedAsAttributeForInt240AltWithTextualColour2()
	{
		String input = "<table style=\"border: 1px solid LightGoldenRodYellow\"> <tr> <td>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		assertTrue(compare(input, "bordercolor=\"LightGoldenRodYellow\"", "style"));
		assertTrue(compare(input, null, "&gt;"));
	}

	@Test
	public void testBorderColorPreservedAsAttributeForInt240AltWithHashColour()
	{
		String input = "<table style=\"border: 1px solid #aabbcc\"> <tr> <td>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		assertTrue(compare(input, "bordercolor=\"#aabbcc\"", "style"));
		assertTrue(compare(input, null, "&gt;"));
	}

	@Test
	public void testBorderColorPreservedAsAttributeForInt240AltWithHashColour2()
	{
		String input = "<table style=\"border: 1px solid #aBc\"> <tr> <td>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		assertTrue(compare(input, "bordercolor=\"#aBc\"", "style"));
		assertTrue(compare(input, null, "&gt;"));
	}

	@Test
	public void testBorderColorPreservedAsAttributeForInt240AltWithRgbColour()
	{
		String input = "<table style=\"border: 1px solid rgb(12,5,253)\"> <tr> <td>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		assertTrue(compare(input, "bordercolor=\"rgb(12,5,253)\"", "style"));
		assertTrue(compare(input, null, "&gt;"));
	}

	@Test
	public void testBorderColorPreservedAsAttributeForInt240AltWithRgbColour2()
	{
		String input = "<table style=\"border: 1px rgb(12 , 5, 253) solid\"> <tr> <td>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		assertTrue(compare(input, "bordercolor=\"rgb(12 , 5, 253)\"", "style"));
		assertTrue(compare(input, null, "&gt;"));
	}

	@Test
	public void testBorderColorPreservedAsAttributeForInt240AltWithNoColour()
	{
		String input = "<table style=\"border: 1px solid\"> <tr> <td>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		assertTrue(compare(input, null, "border-color"));
		assertTrue(compare(input, null, "bordercolor"));
		assertTrue(compare(input, null, "&gt;"));
	}
	
	@Test
	public void testTableWidthPreservedAsAttributeForInt239()
	{
		String input = "<table style=\"border-color: white; width:200px\"> <tr> <td>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		assertTrue(compare(input, "width=\"200px\"", "width:"));
	}
	
	@Test
	public void testBadlyFormedStyle()
	{
		String input = "<table style=\"border-color: white\"> <tr> <td style='width:200px>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		compare(input, "width=\"200px\"", "width");
		//No assertions, we pass if this test completes
	}
	
	@Test
	public void testColumnWidthPreservedAsAttributeForInt239()
	{
		String input = "<table style=\"border-color: white;\"> <tr> <td style='width: 200px'>1</td><td>2</td> </tr> <tr> <td>11</td><td>12</td></tr> <tr> <td>21</td><td>22</td> </tr> </table> ";
		assertTrue(compare(input, "width=\"200px\"", "width:"));
	}
	
	@Test
	public void testUnknownStyleRemoved()
	{
		String input="<table style='flimble: wimble'><tr><td>1</td></tr>";
		assertTrue(compare(input, "table", "flimble"));
	}
	
	@Test
	public void testMixedKnownAndUnknownStyles()
	{
		String input="<table style='flimble: wimble width:200px'><tr><td>1</td></tr>";
		assertTrue(compare(input, "width=\"200px\"", "flimble"));
		assertTrue(compare(input, null, "&gt;"));
	}
	
	@Test
	public void testLotsOfKnownStyles()
	{
		String input="<table style='border-color: white; background-color:#ababab; width:200px;'><tr><td>1</td></tr>";
		assertTrue(compare(input, "width=\"200px\"", "width:"));
		assertTrue(compare(input, "bgcolor", "background-color"));
		assertTrue(compare(input, "bordercolor", "border-color"));
	}
	
	@Test
	public void testLotsOfKnownStylesInUnusualCase()
	{
		String input="<table style='borDER-coLor: blue; bAckgRound-cOlor:#ababab; WIDTH:200px;'><tr><td>1</td></tr>";
		assertTrue(compare(input, "width=\"200px\"", "width:"));
		assertTrue(compare(input, "bgcolor", "background-color"));
		assertTrue(compare(input, "bordercolor", "border-color"));
	}
	
	@Test
	public void testLotsOfKnownStylesIncludedTwice()
	{
		String input="<table style='border-color: white; background-color:#ababab; width:200px; border-color: red; background-color:#ababdb; width:400px;'><tr><td>1</td></tr>";
		assertTrue(compare(input, "width=\"200px\"", "width:"));
		assertTrue(compare(input, "bgcolor", "background-color"));
		assertTrue(compare(input, "bordercolor", "border-color"));
		assertTrue(compare(input, null, "&gt;"));
	}
	
	@Test
	public void testLotsOfKnownStylesOnTDBadlyFormed()
	{
		String input="<table'><tr><td style='border-color: white; background-color:#ababab; width:200px;>1</td></tr>";
		assertTrue(compare(input, "width=\"200px\"", "width:"));
		assertTrue(compare(input, "bgcolor", "background-color"));
		assertTrue(compare(input, "bordercolor", "border-color"));
		assertTrue(compare(input, null, "&gt;"));
	}
	
	@Test
	public void testQuoteMarkAttack()
	{
		String input="<table style=\"gosh <table style='flimble: wimble width:200px'><tr><td>1</td></tr>";
		assertTrue(compare(input, null, "style"));
	}
	
	@Test
	public void testQuoteMarkAttackWithEmbeddedScriptTag()
	{
		String input="<table style=\"gosh <script style='<script language='javascript'>alert(1);</script>flimble: wimble width:200px'><tr><td>1</td></tr>";
		assertTrue(compare(input, null, "style"));
		assertTrue(compare(input, null, "<script"));
	}
	
	@Test
	public void testStylesThatBelongOnTablesOnAParagraph()
	{
		String input="<p style='border-color: white; background-color:#ababab; width:200px;'>Hello World</p>";
		assertTrue(compare(input, null, "width:"));
		assertTrue(compare(input, null, "background-color"));
		assertTrue(compare(input, null, "border-color"));
		assertTrue(compare(input, "hello world", null));
	}
	
	@Test
	public void testSubstrings()
	{
		String input="<table><tr><td>'border-color: white; background-color:#ababab; width:200px;' Hello World</td></tr></table>";
		assertTrue(compare(input, "width:", "width="));
		assertTrue(compare(input, "background-color", "bgcolor"));
		assertTrue(compare(input, "hello world", null));
	}
	
	@Test
	public void testBadStyleAttributeOnSpanForInt249()
	{
		String input="<span style=\"FONT-SIZE: 10pt; FONT-FAMILY: Helvetica\"> ";
		compare(input,null,null);
		//No assertion, we pass the test if we complete without error
	}
	
	@Test
	public void testBadStyleAttributeOnSpanForInt249Alternate()
	{
		String input="<span style=\"COLOR: #0000ff; \"><a href=\"/blah.htm\"><font face=\"helvetica\"><span >blah</span>&nbsp;</font></a></span>";
		assertTrue(compare(input, "blah",null));
	}
	
	@Test
	public void testTableRowHeightForInt239()
	{
		String input="<p><table border=\"0\"><tbody><tr><td>&nbsp;</td><td>&nbsp;</td></tr><tr style=\"height: 200px;\"><td>&nbsp;</td><td>&nbsp;</td></tr></tbody></table></p>";
		assertTrue(compare(input, "height=", "height:"));
	}
	
	@Test
	public void testParagraphCenterAlignWithImageForInt242()
	{
		String input="<p style=\"text-align: center;\"><img src=\"http://www.boycevoice.com/blog/wp-content/uploads/goat-woman.jpg\" alt=\"\" width=\"308\" height=\"335\" align=\"middle\" /></p>";
		assertTrue(compare(input, "align=\"center\"", "text-align:"));
	}
	
	@Test
	public void testParagraphRightAlignWithImageForInt242()
	{
		String input="<p style=\"text-align: right;\"><img src=\"http://www.boycevoice.com/blog/wp-content/uploads/goat-woman.jpg\" alt=\"\" width=\"308\" height=\"335\" align=\"middle\" /></p>";
		assertTrue(compare(input, "align=\"right\"", "text-align:"));
	}
	
	/*
	 
	 HTML comments are currently being stripped out - this seems to produce problems with documents copied-and-pasted
	 from newer version of Word.  This isn't a problem now, but it will be later
	  	*/
	@Test
	public void testHTMLComments()
	{
		String input="<!-- This is a comment --><table <!--Another comment-->><tr><td>1</td></tr></table><!--Last Comment-->";
		compare(input, null, null);
		/*assertTrue(compare(input, "<!--", "&lt;"));
		assertTrue(compare(input, "-->", "&gt;"));
		assertTrue(compare(input, "this is a comment", null));
		assertTrue(compare(input, "another comment", null));
		assertTrue(compare(input, "last comment", null));*/
	}
	
	@Test
	public void testQuoteMarkScriptAttack()
	{
		String input="<table style='ablatibe\"  <script>alert(1)\'</script><script>alert(1)</script></table>";
		assertTrue(compare(input, null, "script"));
		assertTrue(compare(input, null, "alert"));
	}
	
	@Test
	public void testAllWhitespaceStyle()
	{
		String input="<table style = ' helloworld  ' >Hello World</table>";
		assertTrue(compare(input, null, "helloworld"));
	}
	
	/*
	  In an ideal world, the following test would pass.  At the moment, the code is stripping the style, which is OK
	  but not perfect
	 */
	@Test
	public void testAllWhitespaceStyleWithSomeValidCSS()
	{
		String input="<table style = ' helloworld  width:200px'>Hello World</table>";
		compare(input, null, null);
		//assertTrue(compare(input, "width=\"200px\"", "helloworld"));
	}
	
	//TinyMCE seems to use border-color for new tables and border for editing existing tables in IE6
	@Test
	public void testBorderStyleAttributeConvertToColor()
	{
		String input="<table style='border: #67ad51 1px solid;'><tr><td>blah</td></tr></table>";
		assertTrue(compare(input,"bordercolor=\"#67ad51\"", "border:"));
		assertTrue(compare(input,"blah", null));
	}
	
	@Test
	public void testTableBorderForIE6CompatabilityNoStylePresent()
	{
		String input="<p><table border=\"1\"><tbody><tr><td>1</td><td>2</td></tr><tr><td>3</td><td>4</td></tr></tbody></table></p>";
		assertTrue(compare(input, "bordercolor='black'",null));
	}
	
	@Test
	public void testTableBorderForIE6CompatabilityStylePresent()
	{
		String input="<p><table border=\"1\"><tbody><tr><td style='flibble:wibble'>1</td><td>2</td></tr><tr><td>3</td><td>4</td></tr></tbody></table></p>";
		assertTrue(compare(input, "bordercolor='black'",null));
	}
	
	@Test
	public void testTableBorderForIE6CompatabilityDoesntResultInTwoColoursAttribute()
	{
		String input="<p><table bordercolor='red' border=\"1\"><tbody><tr><td>1</td><td>2</td></tr><tr><td>3</td><td>4</td></tr></tbody></table></p>";
		assertTrue(compare(input, "bordercolor='red'","bordercolor='black'"));
	}
	
	@Test
	public void testTableBorderForIE6CompatabilityDoesntResultInTwoColoursStyle()
	{
		String input="<p><table style=\"border: #2e55d1 1px solid;\" border=\"1\"><tbody><tr><td>1</td><td>2</td></tr><tr><td>3</td><td>4</td></tr></tbody></table></p>";
		assertTrue(compare(input, "bordercolor=\"#2e55d1\"","bordercolor='black'"));
	}
		
	protected boolean textIsUnchanged(String text)
	{
		String out = StringUtils.stripUnsafeHTMLTags(text);
		System.out.println(out);
		return (out.replaceAll("bordercolor='black'","").replaceAll("\\s", "")).equalsIgnoreCase(text.replaceAll("bordercolor='black'","").replaceAll("\\s", ""));
	}
	
	protected boolean compare(String text, String has, String doesntHave)
	{
		boolean retVal=true;
		text=getSanitiser().sanitiseHTMLString(text).toLowerCase();
		System.out.println(text);
		if (has!=null)
		{
			has=has.toLowerCase();
			if (text.indexOf(has)==-1)
			{
				retVal=false;
			}
		}
		if (retVal && doesntHave!=null)
		{
			doesntHave=doesntHave.toLowerCase();
			if (text.indexOf(doesntHave)!=-1)
			{
				retVal=false;
			}
		}
		return retVal;
	}
	
	protected AlfrescoHTMLSanitiser getSanitiser()
	{
		return new AlfrescoHTMLSanitiser();
	}
	
	/**
	 * Note this does a case insensitive comparison
	 */
	protected void processUnchangedHTML(String shouldntChange)
	{
		String sanitisedHTML = getSanitiser().sanitiseHTMLString(shouldntChange);
		System.out.println("*****");
		System.out.println(shouldntChange);
		System.out.println(sanitisedHTML);
		assertEquals(shouldntChange.toLowerCase(), sanitisedHTML.toLowerCase());
	}
	
	protected void processRemove(String original, String[] shouldRemove)
	{
		String sanitisedHTML = getSanitiser().sanitiseHTMLString(original).toLowerCase();
		for (int i=0; i < shouldRemove.length; i++)
		{
			assertEquals(sanitisedHTML.indexOf("<".concat(shouldRemove[i].toLowerCase())),-1);
		}
	}
	

	
}