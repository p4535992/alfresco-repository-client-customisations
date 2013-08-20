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
package com.surevine.alfresco.repo.test.stub;

import java.util.List;
import java.util.Map;

import org.alfresco.repo.content.transform.ContentTransformer;
import org.alfresco.service.cmr.dictionary.InvalidTypeException;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NoTransformerException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.namespace.QName;

public class ContentServiceStub implements ContentService {

	private String _mimeType;
	private String _content;
	
	public ContentServiceStub(String mimeType, String content)
	{
		_mimeType=mimeType;
		_content=content;
	}
	
	public ContentTransformer getImageTransformer() {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public ContentReader getRawReader(String arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public ContentReader getReader(NodeRef arg0, QName arg1)
			throws InvalidNodeRefException, InvalidTypeException {
		return new ContentReaderStub(_mimeType, _content);
	}

	public ContentWriter getTempWriter() {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public ContentTransformer getTransformer(String arg0, String arg1) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public ContentTransformer getTransformer(String arg0, String arg1,
			TransformationOptions arg2) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public ContentWriter getWriter(NodeRef arg0, QName arg1, boolean arg2)
			throws InvalidNodeRefException, InvalidTypeException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public boolean isTransformable(ContentReader arg0, ContentWriter arg1) {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	public boolean isTransformable(ContentReader arg0, ContentWriter arg1,
			TransformationOptions arg2) {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	public void transform(ContentReader arg0, ContentWriter arg1)
			throws NoTransformerException, ContentIOException {
		// Auto-generated method stub intentionally unimplemented

	}

	public void transform(ContentReader arg0, ContentWriter arg1,
			Map<String, Object> arg2) throws NoTransformerException,
			ContentIOException {
		// Auto-generated method stub intentionally unimplemented

	}

	public void transform(ContentReader arg0, ContentWriter arg1,
			TransformationOptions arg2) throws NoTransformerException,
			ContentIOException {
		// Auto-generated method stub intentionally unimplemented

	}

	@Override
	public long getStoreFreeSpace() {
		// Auto-generated method stub intentionally unimplemented
		return 0;
	}

	@Override
	public long getStoreTotalSpace() {
		// Auto-generated method stub intentionally unimplemented
		return 0;
	}

	@Override
	public List<ContentTransformer> getActiveTransformers(
			String sourceMimetype, String targetMimetype,
			TransformationOptions options) {
		// TODO Auto-generated method stub
		return null;
	}

}
