package com.surevine.alfresco.repo.delete.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptableHashMap;
import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.surevine.alfresco.repo.delete.ManagedDeletionService;
import com.surevine.alfresco.repo.delete.NodeArchivalDetails;
import com.surevine.alfresco.repo.delete.NodeArchivalDetails.ArchivalStatus;
import com.surevine.alfresco.repo.delete.PerishReason;
import com.surevine.alfresco.repo.delete.PerishabilityLogic;
import com.surevine.alfresco.repo.delete.ScriptManagedDeletionService;

public class ScriptManagedDeletionServiceTest {

	@Mock
	ManagedDeletionService mds;

	@Mock
	PerishabilityLogic perishabilityLogic;

	/**
	 * Class under test
	 */
	ScriptManagedDeletionService smds;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		smds = new ScriptManagedDeletionService();
		smds.setDelegate(mds);
		smds.setPerishabilityLogic(perishabilityLogic);
	}

	@Test
	public void testMarkForDelete() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef("store:///1");
		ScriptNode node = mock(ScriptNode.class);
		when(node.getNodeRef()).thenReturn(nodeRef);

		// +++ When
		// We mark it for delete in JS
		smds.markForDelete(node);

		// +++ Then
		// MarkForDelete is called on the delegate
		verify(mds).markForDelete(nodeRef);
	}

	@Test
	public void testRemoveDeletionMark() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef("store:///1");
		ScriptNode node = mock(ScriptNode.class);
		when(node.getNodeRef()).thenReturn(nodeRef);

		// +++ When
		// We remove the deletion mark in JS
		smds.removeDeletionMark(node);

		// +++ Then
		// RemoveDeletionMark is called on the delegate
		verify(mds).removeDeletionMark(nodeRef);
	}

	@Test
	public void testDelete() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef("store:///1");
		ScriptNode node = mock(ScriptNode.class);
		when(node.getNodeRef()).thenReturn(nodeRef);

		// +++ When
		// We delete the node in JS
		smds.delete(node);

		// +++ Then
		// Delete is called on the delegate
		verify(mds).delete(nodeRef);
	}

	@Test
	public void testUndelete() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef("store:///1");
		ScriptNode node = mock(ScriptNode.class);
		when(node.getNodeRef()).thenReturn(nodeRef);

		// +++ When
		// We undelete the node in JS
		smds.undelete(node);

		// +++ Then
		// Undelete is called on the delegate
		verify(mds).undelete(nodeRef);
	}

	@Test
	public void testSetPerishable() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef("store:///1");
		ScriptNode node = mock(ScriptNode.class);
		when(node.getNodeRef()).thenReturn(nodeRef);

		String reason = "reason";

		// +++ When
		// We set the node perishable in JS
		smds.setPerishable(node, reason);

		// +++ Then
		// SetPerishable is called on the delegate
		verify(mds).setPerishable(nodeRef, reason);
	}

	@Test
	public void testSetPerishableNullReason() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef("store:///1");
		ScriptNode node = mock(ScriptNode.class);
		when(node.getNodeRef()).thenReturn(nodeRef);

		// +++ When
		// We set the node as not perishable in JS
		smds.setPerishable(node, null);

		// +++ Then
		// SetPerishable is called on the delegate
		verify(mds).setPerishable(nodeRef, null);
	}

	@Test
	public void testSetPerishableBlankReason() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef("store:///1");
		ScriptNode node = mock(ScriptNode.class);
		when(node.getNodeRef()).thenReturn(nodeRef);

		// +++ When
		// We set the node as not perishable in JS using a blank string
		smds.setPerishable(node, "");

		// +++ Then
		// SetPerishable is called on the delegate
		verify(mds).setPerishable(nodeRef, null);
	}

	@Test
	public void testGetArchivalDetailsUnmarkedNode() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef("store:///1");
		ScriptNode node = mock(ScriptNode.class);
		when(node.getNodeRef()).thenReturn(nodeRef);

		// And the node is not marked for delete or perishable
		when(mds.getArchivalDetails(nodeRef)).thenReturn(
				new NodeArchivalDetails(ArchivalStatus.UNMARKED, null, null,
						null));

		// +++ When
		// We get the archival details
		Map<String, Object> result = smds.getArchivalDetails(node);

		// +++ Then
		assertEquals("status should be as expected", "UNMARKED",
				result.get("status"));
		assertNull("archivalDue should be null", result.get("archivalDue"));
		assertNull("archivalUser should be null", result.get("archivalUser"));
		assertNull("perishableReason should be null",
				result.get("perishableReason"));
	}

	@SuppressWarnings("serial")
	@Test
	public void testGetArchivalDetailsMarkedForDelete() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef("store:///1");
		ScriptNode node = mock(ScriptNode.class);
		when(node.getNodeRef()).thenReturn(nodeRef);

		// And the node is marked for delete
		final Date archiveDue = new Date();
		when(mds.getArchivalDetails(nodeRef)).thenReturn(
				new NodeArchivalDetails(ArchivalStatus.MARKED_FOR_DELETE,
						archiveDue, "username", null));
		when(node.getProperties()).thenReturn(new HashMap<String, Object>() {
			{
				put("md:archivalDue", archiveDue);
			}
		});

		// +++ When
		// We get the archival details
		Map<String, Object> result = smds.getArchivalDetails(node);

		// +++ Then
		assertEquals("status should be as expected", "MARKED_FOR_DELETE",
				result.get("status"));
		assertEquals("archivalDue should be as expected", archiveDue,
				result.get("archivalDue"));
		assertEquals("archivalUser should be as expected", "username",
				result.get("archivalUser"));
		assertNull("perishableReason should be null",
				result.get("perishableReason"));
	}

	@SuppressWarnings("serial")
	@Test
	public void testGetArchivalDetailsPerishable() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef("store:///1");
		ScriptNode node = mock(ScriptNode.class);
		when(node.getNodeRef()).thenReturn(nodeRef);

		// And the node is not marked for delete or perishable
		final Date archiveDue = new Date();
		when(mds.getArchivalDetails(nodeRef)).thenReturn(
				new NodeArchivalDetails(ArchivalStatus.PERISHABLE, archiveDue,
						"username", "somekindofreason"));
		when(node.getProperties()).thenReturn(new HashMap<String, Object>() {
			{
				put("md:perishDue", archiveDue);
			}
		});

		// +++ When
		// We get the archival details
		Map<String, Object> result = smds.getArchivalDetails(node);

		// +++ Then
		assertEquals("status should be as expected", "PERISHABLE",
				result.get("status"));
		assertEquals("archivalDue should be as expected", archiveDue,
				result.get("archivalDue"));
		assertEquals("archivalUser should be as expected", "username",
				result.get("archivalUser"));
		assertEquals("perishableReason should be as expected",
				"somekindofreason", result.get("perishableReason"));
	}

	@Test
	public void testGetArchivalDetailsDeleted() {
		// +++ Given
		// We have a node
		NodeRef nodeRef = new NodeRef("store:///1");
		ScriptNode node = mock(ScriptNode.class);
		when(node.getNodeRef()).thenReturn(nodeRef);

		// And the node is not marked for delete or perishable
		when(mds.getArchivalDetails(nodeRef)).thenReturn(
				new NodeArchivalDetails(ArchivalStatus.DELETED, null, null,
						null));

		// +++ When
		// We get the archival details
		Map<String, Object> result = smds.getArchivalDetails(node);

		// +++ Then
		assertEquals("status should be as expected", "DELETED",
				result.get("status"));
		assertNull("archivalDue should be null", result.get("archivalDue"));
		assertNull("archivalUser should be null", result.get("archivalUser"));
		assertNull("perishableReason should be null",
				result.get("perishableReason"));
	}

	@Test
	public void testGetPerishReason() {
		String reasonCode = "reason";
		int perishDays = 33;
		String title = "title";
		String description = "description";
		HashSet<String> sites = new HashSet<String>();

		PerishReason expected = new PerishReason(reasonCode, perishDays, title,
				description, sites);
		when(perishabilityLogic.getPerishReason(reasonCode)).thenReturn(
				expected);

		// +++ When
		// We get a perish reason
		ScriptableHashMap<String, Object> reason = smds
				.getPerishReason(reasonCode);

		// +++ Then
		assertEquals("The code should be correct", reasonCode,
				reason.get("code"));
		assertEquals("The perish days should be correct", perishDays,
				reason.get("perishDays"));
		assertEquals("The title should be correct", title, reason.get("title"));
		assertEquals("The description should be correct", description,
				reason.get("description"));
	}
}
