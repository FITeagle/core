package org.fiteagle.core.reservation;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class TimeTest {

	@Test
	public void test_getTimeFromString() throws TimeParsingException {

		String s = "2016-01-06T16:03:24";
		TimeHelperMethods.getTimeFromString(s);
	}

	@Test(expected = TimeParsingException.class)
	public void test_getTimeFromString_exception() throws TimeParsingException {

		String s = "2016-01-06sfdT16:03:24";
		TimeHelperMethods.getTimeFromString(s);
	}

	@Test
	public void test_date1SameOrAfterDate2() throws TimeParsingException {

		String s1 = "2016-01-06T16:05:24";
		Date date1 = TimeHelperMethods.getTimeFromString(s1);
		String s2 = "2016-01-06T16:03:24";
		Date date2 = TimeHelperMethods.getTimeFromString(s2);

		Assert.assertTrue(TimeHelperMethods.date1SameOrAfterDate2(date1, date2));

		String s3 = "2016-02-06T16:03:24";
		Date date3 = TimeHelperMethods.getTimeFromString(s3);
		String s4 = "2016-01-06T16:03:24";
		Date date4 = TimeHelperMethods.getTimeFromString(s4);

		Assert.assertTrue(TimeHelperMethods.date1SameOrAfterDate2(date3, date4));

		String s5 = "2016-01-06T16:03:24";
		Date date5 = TimeHelperMethods.getTimeFromString(s5);
		String s6 = "2016-01-06T16:05:24";
		Date date6 = TimeHelperMethods.getTimeFromString(s6);

		Assert.assertFalse(TimeHelperMethods
				.date1SameOrAfterDate2(date5, date6));
	}

	@Test
	public void test_timesOverlap() throws TimeParsingException {
		String s1 = "2016-01-06T16:03:24";
		String e1 = "2016-01-06T16:05:24";
		String s2 = "2016-01-06T16:02:24";
		String e2 = "2016-01-06T16:04:24";

		Assert.assertTrue(TimeHelperMethods.timesOverlap(s1, e1, s2, e2));
		Assert.assertTrue(TimeHelperMethods.timesOverlap(s2, e2, s1, e1));
		Assert.assertTrue(TimeHelperMethods.timesOverlap(s2, e1, s1, e2));
		Assert.assertTrue(TimeHelperMethods.timesOverlap(s1, e2, s2, e1));
		Assert.assertFalse(TimeHelperMethods.timesOverlap(s2, s1, e2, e1));
		Assert.assertFalse(TimeHelperMethods.timesOverlap(e2, e1, s2, s1));

		String s3 = "2016-01-06T16:03:24";
		String e3 = "2016-01-06T16:03:25";
		String s4 = "2016-01-06T16:03:24";
		String e4 = "2016-01-06T16:03:26";

		Assert.assertTrue(TimeHelperMethods.timesOverlap(s3, e3, s4, e4));
		Assert.assertTrue(TimeHelperMethods.timesOverlap(s4, e4, s3, e3));
		Assert.assertTrue(TimeHelperMethods.timesOverlap(s4, s4, s3, s3));
		Assert.assertFalse(TimeHelperMethods.timesOverlap(s3, s3, e3, e3));
		Assert.assertFalse(TimeHelperMethods.timesOverlap(e3, e3, s3, s3));
	}
	
	@Test
	public void test_dateNotInPast() throws TimeParsingException {
		String s1 = "2056-01-06T16:03:24";
		Date d1 = TimeHelperMethods.getTimeFromString(s1);
		
		Assert.assertTrue(TimeHelperMethods.dateNotInPast(d1));
		
		String s2 = "2016-01-06T16:03:24";
		Date d2 = TimeHelperMethods.getTimeFromString(s2);
		
		Assert.assertFalse(TimeHelperMethods.dateNotInPast(d2));
	}
}