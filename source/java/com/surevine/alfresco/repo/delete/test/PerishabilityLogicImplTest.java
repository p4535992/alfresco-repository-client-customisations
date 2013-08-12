package com.surevine.alfresco.repo.delete.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;

import javax.transaction.UserTransaction;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.transaction.TransactionService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.surevine.alfresco.repo.delete.PerishReason;
import com.surevine.alfresco.repo.delete.PerishabilityLogicImpl;
import com.surevine.alfresco.repo.test.stub.ContentReaderStub;

public class PerishabilityLogicImplTest {

	static final String PERISHABLE_REASONS_JSON = "{perishableReasons:[" + 
		"{" +
			"code:\"reason1\"," +
			"perishDays:14," +
			"title:\"Reason 1\"," +
			"description:\"This is Reason 1\"," +
			"sites:[\"site1\"]" +
		"},{" +
			"code:\"reason2\"," +
			"perishDays:28," +
			"title:\"Reason 2\"," +
			"description:\"This is Reason 2\"," +
			"sites:[\"site1\",\"site2\"]" +
		"}]}";

	@SuppressWarnings("serial")
	static final List<PerishReason> PERISHABLE_REASONS = new ArrayList<PerishReason>() {
		{
			add(new PerishReason("reason1", 14, "Reason 1", "This is Reason 1",
					new HashSet<String>() {
						{
							add("site1");
						}
					}));
			add(new PerishReason("reason2", 28, "Reason 2", "This is Reason 2",
					new HashSet<String>() {
						{
							add("site1");
							add("site2");
						}
					}));
		}
	};
	
	NodeRef perishableReasonsNodeRef;

	@Mock
	SearchService searchService;

	@Mock
	ContentService contentService;

	@Mock
	TransactionService transactionService;

	/**
	 * Class under test
	 */
	PerishabilityLogicImpl perishabilityLogic;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		perishableReasonsNodeRef = new NodeRef(
				"store:///perishableReasons.json");

		when(
				contentService.getReader(perishableReasonsNodeRef,
						ContentModel.PROP_CONTENT)).thenReturn(
				new ContentReaderStub("application/json",
						PERISHABLE_REASONS_JSON));

		ResultSet results = mock(ResultSet.class);
		when(results.length()).thenReturn(1);
		when(results.getNodeRef(0)).thenReturn(perishableReasonsNodeRef);

		when(
				searchService
						.query(new StoreRef(StoreRef.PROTOCOL_WORKSPACE,
								"SpacesStore"), SearchService.LANGUAGE_LUCENE,
								"PATH:\"/app:company_home/app:dictionary/cm:perishableReasons.json\""))
				.thenReturn(results);
		
		when(transactionService.getUserTransaction(true)).thenReturn(mock(UserTransaction.class));

		perishabilityLogic = new PerishabilityLogicImpl();
		perishabilityLogic.setSearchService(searchService);
		perishabilityLogic.setContentService(contentService);
		perishabilityLogic.setTransactionService(transactionService);
		perishabilityLogic.init();
	}

	@Test
	public void testCalculatePerishDue() {
		Calendar currentCal = Calendar.getInstance();
		currentCal.set(2013, 3, 4, 12, 34, 12);

		Calendar expectedCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		expectedCal.set(2013, 3, 19, 0, 0, 0);
		expectedCal.set(Calendar.MILLISECOND, 0);

		// When
		// We get the perish due date for reason1
		Date perishDue = perishabilityLogic.calculatePerishDue("reason1", currentCal.getTime());
		
		// Then
		assertEquals("The due date should be correct", expectedCal.getTime(), perishDue);
	}

	@Test
	public void testCalculatePerishDueUsesMidnightZulu() {
		Calendar currentCal = Calendar.getInstance(TimeZone.getTimeZone("PST"));
		currentCal.set(2013, 3, 4, 21, 34, 12);

		Calendar expectedCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		expectedCal.set(2013, 3, 20, 0, 0, 0);
		expectedCal.set(Calendar.MILLISECOND, 0);

		// When
		// We get the perish due date for reason1
		Date perishDue = perishabilityLogic.calculatePerishDue("reason1", currentCal.getTime());

		// Then
		assertEquals("The due date should be correct", expectedCal.getTime(), perishDue);
	}

	@Test
	public void testCalculatePerishDueUsesMidnightZulu2() {
		Calendar currentCal = Calendar.getInstance(TimeZone.getTimeZone("PST"));
		currentCal.set(2013, 3, 4, 5, 34, 12);

		Calendar expectedCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		expectedCal.set(2013, 3, 19, 0, 0, 0);
		expectedCal.set(Calendar.MILLISECOND, 0);

		// When
		// We get the perish due date for reason1
		Date perishDue = perishabilityLogic.calculatePerishDue("reason1", currentCal.getTime());

		// Then
		assertEquals("The due date should be correct", expectedCal.getTime(), perishDue);
	}
	
	@Test
	public void testInitWithMissingConfigDoesntThrowException() throws Exception {
		ResultSet results = mock(ResultSet.class);
		when(results.length()).thenReturn(0);
		
		when(
				searchService
						.query(new StoreRef(StoreRef.PROTOCOL_WORKSPACE,
								"SpacesStore"), SearchService.LANGUAGE_LUCENE,
								"PATH:\"/app:company_home/app:dictionary/cm:perishableReasons.json\""))
				.thenReturn(results);
		
		perishabilityLogic = new PerishabilityLogicImpl();
		perishabilityLogic.setSearchService(searchService);
		perishabilityLogic.setContentService(contentService);
		perishabilityLogic.setTransactionService(transactionService);
		perishabilityLogic.init();
	}
	
	@Test
	public void testInitWithInvalidJSONDoesntThrowException() throws Exception {
		when(
				contentService.getReader(perishableReasonsNodeRef,
						ContentModel.PROP_CONTENT)).thenReturn(
				new ContentReaderStub("application/json",
						"some invalid json"));
		
		perishabilityLogic = new PerishabilityLogicImpl();
		perishabilityLogic.setSearchService(searchService);
		perishabilityLogic.setContentService(contentService);
		perishabilityLogic.setTransactionService(transactionService);
		perishabilityLogic.init();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCalculatePerishDueThrowsExceptionForInvalidReasonCode() {
		perishabilityLogic.calculatePerishDue("randomReasonThatDoesntExist", new Date());
	}

	@Test
	public void testGetPerishReasons() {
		// When
		// We get the perishable reasons
		List<PerishReason> perishReasons = perishabilityLogic
				.getPerishReasons("site1");

		// Then
		assertEquals("The perishable reasons should be correct",
				PERISHABLE_REASONS, perishReasons);
	}

	@Test
	public void testGetPerishReasonsForSite1() {
		// When
		// We get the perishable reasons
		List<PerishReason> perishReasons = perishabilityLogic
				.getPerishReasons("site2");

		// Then
		assertEquals("The perishable reasons should be correct",
				PERISHABLE_REASONS.subList(1, 2), perishReasons);
	}

	@Test
	public void testGetPerishReason() {
		// When
		// We get the reason for "reason1"
		PerishReason reason = perishabilityLogic.getPerishReason("reason1");
		
		// Then
		assertEquals("We should have the 'reason1' perish reason", PERISHABLE_REASONS.get(0), reason);
	}
	
	@Test
	public void testGetPerishReasonIncorrectCode() {
		// When
		// We get the reason for a non-existant reason code
		PerishReason reason = perishabilityLogic.getPerishReason("non_existant");
		
		// Then
		assertNull("The reason returned should be null", reason);
	}
}
