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
package com.surevine.alfresco.repo.delete.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileFolderServiceType;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.model.FileNotFoundException;
import org.alfresco.service.cmr.model.SubFolderFilter;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class FileFolderServiceMock implements FileFolderService {

	public Collection<String> names = new ArrayList<String>(10);
	
	@Override
	public FileInfo create(NodeRef arg0, String arg1, QName arg2) throws FileExistsException {
		names.add(arg1);
		return new FileInfo() {
			
			@Override
			public boolean isLink() {
				// Auto-generated method stub intentionally unimplemented
				return false;
			}
			
			@Override
			public boolean isFolder() {
				// Auto-generated method stub intentionally unimplemented
				return false;
			}
			
			@Override
			public Map<QName, Serializable> getProperties() {
				// Auto-generated method stub intentionally unimplemented
				return null;
			}
			
			@Override
			public NodeRef getNodeRef() {
				// Auto-generated method stub intentionally unimplemented
				return null;
			}
			
			@Override
			public String getName() {
				// Auto-generated method stub intentionally unimplemented
				return null;
			}
			
			@Override
			public Date getModifiedDate() {
				// Auto-generated method stub intentionally unimplemented
				return null;
			}
			
			@Override
			public NodeRef getLinkNodeRef() {
				// Auto-generated method stub intentionally unimplemented
				return null;
			}
			
			@Override
			public Date getCreatedDate() {
				// Auto-generated method stub intentionally unimplemented
				return null;
			}
			
			@Override
			public ContentData getContentData() {
				// Auto-generated method stub intentionally unimplemented
				return null;
			}

			@Override
			public boolean isHidden() {
				// TODO Auto-generated method stub
				return false;
			}
		};
	}
	
	//Following methods are just stubs
	
	@Override
	public FileInfo copy(NodeRef arg0, NodeRef arg1, String arg2)
			throws FileExistsException, FileNotFoundException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public FileInfo create(NodeRef arg0, String arg1, QName arg2, QName arg3)
			throws FileExistsException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public void delete(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented

	}

	@Override
	public boolean exists(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return false;
	}

	@Override
	public FileInfo getFileInfo(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<FileInfo> getNamePath(NodeRef arg0, NodeRef arg1)
			throws FileNotFoundException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public ContentReader getReader(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public FileFolderServiceType getType(QName arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public ContentWriter getWriter(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<FileInfo> list(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<FileInfo> listDeepFolders(NodeRef arg0, SubFolderFilter arg1) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<FileInfo> listFiles(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<FileInfo> listFolders(NodeRef arg0) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public FileInfo move(NodeRef arg0, NodeRef arg1, String arg2)
			throws FileExistsException, FileNotFoundException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public FileInfo move(NodeRef arg0, NodeRef arg1, NodeRef arg2, String arg3)
			throws FileExistsException, FileNotFoundException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public FileInfo rename(NodeRef arg0, String arg1)
			throws FileExistsException, FileNotFoundException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public FileInfo resolveNamePath(NodeRef arg0, List<String> arg1)
			throws FileNotFoundException {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<FileInfo> search(NodeRef arg0, String arg1, boolean arg2) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public List<FileInfo> search(NodeRef arg0, String arg1, boolean arg2,
			boolean arg3, boolean arg4) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public NodeRef searchSimple(NodeRef arg0, String arg1) {
		// Auto-generated method stub intentionally unimplemented
		return null;
	}

	@Override
	public FileInfo moveFrom(NodeRef sourceNodeRef, NodeRef sourceParentRef,
			NodeRef targetParentRef, String newName)
			throws FileExistsException, FileNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FileInfo resolveNamePath(NodeRef rootNodeRef,
			List<String> pathElements, boolean mustExist)
			throws FileNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FileInfo> removeHiddenFiles(List<FileInfo> files) {
		// TODO Auto-generated method stub
		return null;
	}

}
