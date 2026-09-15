package com.ringoftime;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CustomUiAnchorsCompatibilityTest
{
	@Test
	public void transfersMemberAssignmentToGroupName()
	{
		final Map<String, Integer> assignments = new LinkedHashMap<>();
		assignments.put("Ring of Time - Antifire", 1);

		assertTrue(CustomUiAnchorsCompatibility.reconcileNames(
			assignments,
			"Ring of Time - Hunter",
			Arrays.asList("Ring of Time - Hunter", "Ring of Time - Antifire")
		));
		assertEquals(Integer.valueOf(1), assignments.get("Ring of Time - Hunter"));
		assertFalse(assignments.containsKey("Ring of Time - Antifire"));
	}

	@Test
	public void existingGroupAssignmentWinsOverMemberAliases()
	{
		final Map<String, Integer> assignments = new LinkedHashMap<>();
		assignments.put("Ring of Time - Hunter", 2);
		assignments.put("Ring of Time - Antifire", 1);

		assertTrue(CustomUiAnchorsCompatibility.reconcileNames(
			assignments,
			"Ring of Time - Hunter",
			Arrays.asList("Ring of Time - Hunter", "Ring of Time - Antifire")
		));
		assertEquals(Integer.valueOf(2), assignments.get("Ring of Time - Hunter"));
		assertFalse(assignments.containsKey("Ring of Time - Antifire"));
	}

	@Test
	public void leavesUnrelatedAssignmentsUntouched()
	{
		final Map<String, Integer> assignments = new LinkedHashMap<>();
		assignments.put("InfoBoxOverlay", 3);

		assertFalse(CustomUiAnchorsCompatibility.reconcileNames(
			assignments,
			"Ring of Time - Hunter",
			Arrays.asList("Ring of Time - Hunter", "Ring of Time - Antifire")
		));
		assertEquals(Integer.valueOf(3), assignments.get("InfoBoxOverlay"));
	}
}
