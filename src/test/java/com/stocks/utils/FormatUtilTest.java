package com.stocks.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FormatUtilTest {

	@Test
	public void testGetMonthExpiry() {
		// Before last Thursday of June 2025
		assertEquals("26-06-2025", FormatUtil.getMonthExpiry("25-06-2025"));
		// On last Thursday of June 2025
		assertEquals("26-06-2025", FormatUtil.getMonthExpiry("26-06-2025"));
		// After last Thursday of June 2025
		assertEquals("31-07-2025", FormatUtil.getMonthExpiry("27-06-2025"));
		// Before last Thursday of July 2025
		assertEquals("31-07-2025", FormatUtil.getMonthExpiry("01-07-2025"));
		// On last Thursday of July 2025
		assertEquals("31-07-2025", FormatUtil.getMonthExpiry("31-07-2025"));
		// After last Thursday of July 2025
		assertEquals("28-08-2025", FormatUtil.getMonthExpiry("01-08-2025"));
	}
}

