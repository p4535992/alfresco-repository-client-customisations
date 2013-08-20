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
package com.surevine.alfresco.repo.test;

import org.alfresco.service.cmr.repository.NodeRef;

import com.surevine.alfresco.repo.impl.AlfrescoHTMLIdentifier;
import com.surevine.alfresco.repo.HTMLIdentifier;
import com.surevine.alfresco.repo.test.stub.ContentServiceStub;
import org.junit.Test;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class AlfrescoHTMLIdentifierTest {
	
	@Test (expected=IllegalStateException.class)
	public void testIncorrectAccessOrder() 
	{
		getIdentifierNoTarget("text/html").isTargetHTML(); //Oh no!  We haven't set the target
	}
	
	@Test
	public void testIsHTML()
	{
		assertTrue(getIdentifier("text/html").isTargetHTML());
	}
	
	@Test
	public void testIsHTMLMixedCase()
	{
		assertTrue(getIdentifier("tExT/hTmL").isTargetHTML());
	}
	
	@Test
	public void testIsHTMLWeirdMimeTypes()
	{
		assertTrue(getIdentifier("html").isTargetHTML());
		assertTrue(getIdentifier("html, baby!").isTargetHTML());
		assertTrue(getIdentifier(".html").isTargetHTML());
		assertTrue(getIdentifier(".htm").isTargetHTML());
		assertTrue(getIdentifier("htm").isTargetHTML());
		assertTrue(getIdentifier(".hTml").isTargetHTML());
	}
	
	@Test
	public void testIsHTMLLineBreakInMimeType()
	{
		assertTrue(getIdentifier("text/html\r").isTargetHTML());
	}
	
	@Test
	public void testIsNotHTML()
	{
		assertFalse(getIdentifier("text/plain").isTargetHTML());
	}
	
	@Test
	public void testNullMimetype()
	{
		assertFalse(getIdentifier(null).isTargetHTML());
	}
	
	@Test
	public void testEmptyMimetype()
	{
		assertFalse(getIdentifier("").isTargetHTML());
	}
	
	@Test
	public void testEmptyMimetypeButStillHasTheSlashInTheMiddle()
	{
		assertFalse(getIdentifier("/").isTargetHTML());
	}
	
	
	
	protected HTMLIdentifier getIdentifierNoTarget(String mimeType)
	{
		AlfrescoHTMLIdentifier identifier = new AlfrescoHTMLIdentifier();
		identifier.setContentService(new ContentServiceStub(mimeType, "Some Content"));
		return identifier;
	}
	
	protected HTMLIdentifier getIdentifier(String mimeType){
		HTMLIdentifier identifier = getIdentifierNoTarget(mimeType);
		identifier.setTargetNodeRef(getNodeRef());
		return identifier;
	}
	
	/**
	 * Get us a noderef with a valid name - anything that looks like a valid NodeRef will do
	 * @return An anonymous NodeRef
	 */
	protected NodeRef getNodeRef()
	{
		return new NodeRef("workspace://SpacesStore/8039e4ee-ba29-4a07-9e6a-5b2ae758811d");
	}

}
