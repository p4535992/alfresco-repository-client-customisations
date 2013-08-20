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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Locale;

import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.ContentAccessor;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentStreamListener;

public class ContentReaderStub implements ContentReader, ContentAccessor {
	
	private String _mimeType;
	private String _content;
	
	public ContentReaderStub(String mimeType, String content)
	{
		_mimeType=mimeType;
		_content=content;
	}

	public boolean exists() {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	public void getContent(OutputStream arg0) throws ContentIOException {
		// Auto-generated method stub intentionally unimplemented

	}

	public void getContent(File arg0) throws ContentIOException {
		// Auto-generated method stub intentionally unimplemented

	}

	public InputStream getContentInputStream() throws ContentIOException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public String getContentString() throws ContentIOException {
		return _content;
	}

	public String getContentString(int arg0) throws ContentIOException {
		if (_content==null)
		{
			return null;
		}
		if (_content.length()<arg0 || arg0==0)
		{
			return _content;
		}
		return _content.substring(0, arg0-1);
	}

	public FileChannel getFileChannel() throws ContentIOException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public long getLastModified() {
		// Auto-generated method stub intentionally unimplemented
		return 0;
	}

	public ReadableByteChannel getReadableChannel() throws ContentIOException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public ContentReader getReader() throws ContentIOException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public boolean isClosed() {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	public void addListener(ContentStreamListener arg0) {
		// Auto-generated method stub intentionally unimplemented

	}

	public ContentData getContentData() {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public String getContentUrl() {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public String getEncoding() {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public Locale getLocale() {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	public String getMimetype() {
		return _mimeType;
	}

	public long getSize() {
		// Auto-generated method stub intentionally unimplemented
		return 0;
	}

	public boolean isChannelOpen() {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	public void setEncoding(String arg0) {
		// Auto-generated method stub intentionally unimplemented

	}

	public void setLocale(Locale arg0) {
		// Auto-generated method stub intentionally unimplemented

	}

	public void setMimetype(String arg0) {
		// Auto-generated method stub intentionally unimplemented

	}

	public void setRetryingTransactionHelper(RetryingTransactionHelper arg0) {
		// Auto-generated method stub intentionally unimplemented

	}

}
