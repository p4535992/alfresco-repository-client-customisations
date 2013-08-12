package com.surevine.alfresco.repo.upload.test;

import org.junit.Before;
import org.junit.Test;

import com.surevine.alfresco.repo.upload.FilenameNormaliser;
import com.surevine.alfresco.repo.upload.FilenameNormaliserImpl;

import static junit.framework.Assert.*;

public class FilenameNormaliserTest {

	private FilenameNormaliser _impl=null;
	
	@Before 
	public void setup()
	{
		_impl = new FilenameNormaliserImpl();
	}
	
	private void unchanged(String filename)
	{
		assertEquals(_impl.normalise(filename), filename);
	}
	
	private void preserved(String filename, String s)
	{
		assertFalse(_impl.normalise(filename).indexOf(s)==-1);
	}
	
	private void removed(String filename, String s)
	{
		assertTrue(_impl.normalise(filename).indexOf(s)==-1);
	}
	
	@Test
	public void regularFilename()
	{
		unchanged("hello");
		unchanged("hElLo");
	}
	
	@Test
	public void spaces()
	{
		unchanged("hello world");
		unchanged(" hello world");
		unchanged("hello world");
		unchanged("  hello  world  this is a thing    ");
	}
	
	@Test
	public void extendedASCIIForbidden()
	{
		// TODO These characters aren't being mapped properly in UTF-8 under maven 3.  Can we use characters that are?
		preserved("test¬‚«~«æ", "test");
		removed("test¬‚«~«æ", "¬");
		removed("test¬‚«~«æ", "‚");
		removed("test¬‚«~«æ", "«");
		preserved("test¬‚«~«æ", "~");
		removed("test¬‚«~«æ", "«");
		removed("test¬‚«~«æ", "E");
		removed("    jfdkdghfj test¬‚«~«æ", "E");
	}
	
	@Test
	public void encodedSpaces()
	{
		unchanged("A%20file%20name.doc");
	}
	
	@Test
	public void percents()
	{
		removed("foo%bar", "%");
		removed("foo%%bar", "%");
		removed("%foo%bar%", "%");
	}
	
	@Test
	public void percentsAfterEncoding()
	{
		removed("A%2525252520dou%ble%2525252520encoded%2525252520name.doc", "%"); //Note the extra % in the word "double"
	}
	
	@Test
	public void doubleEncodedSpaces()
	{
		removed("A%2520double%2520encoded%2520name.doc", "%2520");
		preserved("A%2520double%2520encoded%2520name.doc", "%20");
	}
	
	@Test
	public void fiveTimesEncodedSpaces()
	{
		removed("A%2525252520double%2525252520encoded%2525252520name.doc", "%25");
		preserved("A%2525252520double%2525252520encoded%2525252520name.doc", "%20");
	}
	
	@Test
	public void miscEncoded()
	{
		removed("myFile!%40%C2%A3%24%25%5E%26*().doc", "%");
	}
	
	@Test
	public void doubleMiscEncoded()
	{
		removed("myFile!%2540%25C2%25A3%2524%2525%255E%2526*().doc", "!%25");
		preserved("myFile!%2540%25C2%25A3%2524%2525%255E%2526*().doc", "%40");
		preserved("myFile!%2540%25C2%25A3%2524%2525%255E%2526*().doc", "%24");
		preserved("myFile!%2540%25C2%25A3%2524%2525%255E%2526*().doc", "%25");
		preserved("myFile!%2540%25C2%25A3%2524%2525%255E%2526*().doc", "%26");
	}
	
	@Test
	public void fiveTimesMiscEncoded()
	{
		removed("myFile!%2525252540%25252525C2%25252525A3%2525252524%2525252525%252525255E%2525252526*().doc", "!%25");
		preserved("myFile!%2525252540%25252525C2%25252525A3%2525252524%2525252525%252525255E%2525252526*().doc", "%40");
		preserved("myFile!%2525252540%25252525C2%25252525A3%2525252524%2525252525%252525255E%2525252526*().doc", "%24");
		preserved("myFile!%2525252540%25252525C2%25252525A3%2525252524%2525252525%252525255E%2525252526*().doc", "%25");
		preserved("myFile!%2525252540%25252525C2%25252525A3%2525252524%2525252525%252525255E%2525252526*().doc", "%26");
	}
		
}