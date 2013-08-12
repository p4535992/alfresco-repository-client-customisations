package com.surevine.alfresco.repo.test;

import com.surevine.alfresco.repo.HTMLIdentifier;
import com.surevine.alfresco.repo.impl.FilenameAndContentHTMLIdentifier;
import com.surevine.alfresco.repo.test.stub.ContentServiceStub;
import com.surevine.alfresco.repo.test.stub.NodeServiceStub;
import org.junit.Test;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class FilenameAndContentHTMLIdentifierTest extends AlfrescoHTMLIdentifierTest {
	
	private static String[] HTML_EXT={"html"};
	private static String[] HTML_EL={"html"};
	
	protected HTMLIdentifier getIdentifierNoTarget(String mimeType, String content, String fileName)
	{
		FilenameAndContentHTMLIdentifier identifier = new FilenameAndContentHTMLIdentifier();
		identifier.setContentService(new ContentServiceStub(mimeType, content));
		identifier.setNodeService(new NodeServiceStub(fileName));
		return identifier;
	}
	
	protected HTMLIdentifier getIdentifierNoTarget(String mimeType)
	{
		return getIdentifierNoTarget(mimeType, "Some Content", "filename.dat");
	}
	
	protected HTMLIdentifier getFACIdentifier(String[] fileNameHTML, String[] contentHTML, String fileName, String content, int maxChars)
	{
		FilenameAndContentHTMLIdentifier identifier=(FilenameAndContentHTMLIdentifier)getIdentifierNoTarget("text/plain", content, fileName);
		identifier.setTargetNodeRef(getNodeRef());
		identifier.setHTMLContent(contentHTML);
		identifier.setHTMLExtensions(fileNameHTML);
		identifier.setMaxCharsToSearch(maxChars);
		return identifier;
	}
	
	@Test
	public void testHTMLNodeNameFromOne()
	{
		assertTrue(getFACIdentifier(HTML_EXT, HTML_EL, "myFile.html", "Some Content", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLNodeNameFromSeveral()
	{
		String[] htmlExts = {"html", "htm", "shtm", "fooble"};
		assertTrue(getFACIdentifier(htmlExts, HTML_EL, "myFile.fooble", "Some Content", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLNodeNameCaseInsensitive()
	{
		String[] htmlExts = {"html", "htm", "shtm", "fooble"};
		assertTrue(getFACIdentifier(htmlExts, HTML_EL, "myFile.FOObLE", "Some Content", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLNodeNameMatchInTheMiddle()
	{
		String[] htmlExts = {"html", "htm", "shtm", "fooble"};
		assertTrue(getFACIdentifier(htmlExts, HTML_EL, "myFile.FOObLE.wobble.bibble", "Some Content", 10240).isTargetHTML());	
	}

	@Test
	public void testHTMLNodeNotHTML()
	{
		String[] htmlExts = {"html", "htm", "shtm", "fooble"};
		assertFalse(getFACIdentifier(htmlExts, HTML_EL, "myFile.nothtml", "Some Content", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLNodeNotHTMLNoConfig()
	{
		assertFalse(getFACIdentifier(null, null, "myFile.nothtml", "Some Content", 0).isTargetHTML());	
	}
	
	@Test
	public void testHTMLNodeContentFromOne()
	{
		assertTrue(getFACIdentifier(HTML_EXT, HTML_EL, "myFile.txt", "Hiding some HTML: <html>Some Things", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLNodeContentFromOneWithSpacesInElement()
	{
		assertTrue(getFACIdentifier(HTML_EXT, HTML_EL, "myFile.txt", "Hiding some HTML: <  html language='javascript'>Some Things", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLNodeContentFromSeveral()
	{
		String[] htmlEls = {"foo", "bar", "baz", "html"};
		assertTrue(getFACIdentifier(HTML_EXT, htmlEls, "myFile.txt", "Hiding some HTML: <html>Some Things", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLNodeLineBreaksInContent()
	{
		String[] htmlEls = {"foo", "bar", "baz", "html"};
		assertTrue(getFACIdentifier(HTML_EXT, htmlEls, "myFile.txt", "Hiding some HTML:\r\n\r\n\n <html>Some Things", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLNodeContentCaseInsensitive()
	{
		String[] htmlEls = {"foo", "bar", "baz", "html"};
		assertTrue(getFACIdentifier(HTML_EXT, htmlEls, "myFile.txt", "Hiding some HTML: <HtMl>Some Things", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLNodeButWontFindItCauseMaxCharsTooSmall()
	{
		String[] htmlEls = {"foo", "bar", "baz", "html"};
		assertFalse(getFACIdentifier(HTML_EXT, htmlEls, "myFile.txt", "Hiding some HTML: <HtMl>Some Things", 10).isTargetHTML());	
	}
	
	@Test
	public void testHTMLMatchesOnMultipleCounts()
	{
		assertTrue(getFACIdentifier(HTML_EXT, HTML_EL, "myFile.html", "Some <html>Content", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLMatchesOnFilenameButNullContent()
	{
		assertTrue(getFACIdentifier(HTML_EXT, HTML_EL, "myFile.html", null, 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLMatchesOnFilenameButEmptyContent()
	{
		assertTrue(getFACIdentifier(HTML_EXT, HTML_EL, "myFile.html", "", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLMatchesOnContentButNullFilename()
	{
		assertTrue(getFACIdentifier(HTML_EXT, HTML_EL, null, "This is some <html> Content", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLMatchesOnContentButEmptyFilename()
	{
		assertTrue(getFACIdentifier(HTML_EXT, HTML_EL, "", "This is some <html> Content", 10240).isTargetHTML());	
	}
	
	@Test
	public void testHTMLBigContentSmallMaxBytes()
	{
		String content="This is some content what I am making.  It will get longer\n\r\r\n";
		StringBuffer sb = new StringBuffer(content.length()*100000);
		for (int i=0; i < 100000; i++)
		{
			sb.append(content);
		}
		sb.append("html");
		assertFalse(getFACIdentifier(HTML_EXT, HTML_EL, "myFile.dat", sb.toString(), 10).isTargetHTML());	
	}
	
	@Test
	public void testHTMLBigContentBigMaxBytes()
	{
		String content="This is some content what I am making.  It will get longer\n\r\r\n";
		StringBuffer sb = new StringBuffer(content.length()*100000);
		for (int i=0; i < 100000; i++)
		{
			sb.append(content);
		}
		sb.append("html");
		assertFalse(getFACIdentifier(HTML_EXT, HTML_EL, "myFile.dat", sb.toString(), Integer.MAX_VALUE).isTargetHTML());	
	}
	
}
