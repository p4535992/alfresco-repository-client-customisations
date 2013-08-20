/*
 * Copyright (C) 2008-2010 Surevine Limited.
 *   
 * Although intended for deployment and use alongside Alfresco this module should
 * be considered 'Not a Contribution' as defined in Alfresco'sstandard contribution agreement, see
 * http://www.alfresco.org/resource/AlfrescoContributionAgreementv2.pdf
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
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
		// These characters aren't being mapped properly in UTF-8 under maven 3.  Can we use characters that are?
		preserved("test���~��", "test");
		removed("test���~��", "�");
		removed("test���~��", "�");
		removed("test���~��", "�");
		preserved("test���~��", "~");
		removed("test���~��", "�");
		removed("test���~��", "E");
		removed("    jfdkdghfj test���~��", "E");
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
