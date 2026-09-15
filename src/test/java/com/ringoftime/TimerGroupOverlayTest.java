package com.ringoftime;

import com.ringoftime.EffectTimerTracker.Effect;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class TimerGroupOverlayTest
{
	@Test
	public void circleComponentUsesRenderedSizeAndTracksItsBounds()
	{
		final TimerCircleComponent component = new TimerCircleComponent(
			new FakeTimer(true, new Dimension(40, 30))
		);
		component.setPreferredLocation(new Point(10, 20));

		final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			assertEquals(new Dimension(40, 30), component.render(graphics));
			assertEquals(10, component.getBounds().x);
			assertEquals(20, component.getBounds().y);
			assertEquals(40, component.getBounds().width);
			assertEquals(30, component.getBounds().height);
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void orientationChangesDefaultGroupRows()
	{
		final RingOfTimeConfig config = new RingOfTimeConfig() { };
		final TimerGroupOverlay group = new TimerGroupOverlay(
			null,
			null,
			config,
			null,
			"Test group",
			ComponentOrientation.VERTICAL
		);
		for (int i = 0; i < 5; i++)
		{
			group.addMember(new FakeTimer(true, new Dimension(50, 50)));
		}

		final Dimension cell = TimerLabelLayout.estimateMaximumDimension(config);
		assertEquals(cell.height * 4 + 26, group.estimateAutomaticHeight(cell));

		group.flip();
		assertEquals(cell.height * 2 + 22, group.estimateAutomaticHeight(cell));
	}

	@Test
	public void resizingStopsAutomaticPlacement()
	{
		final RingOfTimeConfig config = new RingOfTimeConfig() { };
		final TimerGroupOverlay group = new TimerGroupOverlay(
			null,
			null,
			config,
			null,
			"Test group",
			ComponentOrientation.VERTICAL
		);
		group.initializeAutomaticLayout();

		assertTrue(group.applyAutomaticLocation(new Point(10, 10)));
		group.setPreferredSize(new Dimension(100, 100));
		assertFalse(group.applyAutomaticLocation(new Point(10, 80)));
	}

	@Test
	public void movingMembersUsesTheTargetSlotInEitherDirection()
	{
		final TimerGroupOverlay group = createGroup();
		final FakeTimer first = new FakeTimer("First");
		final FakeTimer second = new FakeTimer("Second");
		final FakeTimer third = new FakeTimer("Third");
		group.addMember(first);
		group.addMember(second);
		group.addMember(third);

		assertTrue(group.moveMember(first, third, true));
		assertEquals(Arrays.asList(second, third, first), group.getMembers());

		assertTrue(group.moveMember(first, second, false));
		assertEquals(Arrays.asList(first, second, third), group.getMembers());
		assertFalse(group.moveMember(first, first, false));
	}

	@Test
	public void restoringOrderKeepsNewMembersAtTheEnd()
	{
		final TimerGroupOverlay group = createGroup();
		final FakeTimer first = new FakeTimer("First");
		final FakeTimer second = new FakeTimer("Second");
		final FakeTimer third = new FakeTimer("Third");
		group.addMember(first);
		group.addMember(second);
		group.addMember(third);

		group.restoreMemberOrder(Arrays.asList("Third", "First", "Removed timer"));

		assertEquals(Arrays.asList(third, first, second), group.getMembers());
	}

	@Test
	public void renderedMemberBoundsCanBeHitTestedInCanvasCoordinates()
	{
		final TimerGroupOverlay group = new TimerGroupOverlay(
			fakeClient(),
			null,
			new RingOfTimeConfig() { },
			null,
			"Test group",
			ComponentOrientation.VERTICAL
		);
		final FakeTimer first = new FakeTimer("First");
		final FakeTimer second = new FakeTimer("Second");
		group.addMember(first);
		group.addMember(second);
		group.getBounds().setLocation(100, 100);

		final BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			group.render(graphics);
		}
		finally
		{
			graphics.dispose();
		}

		final TimerGroupOverlay.MemberHit hit = group.findMemberAt(new Point(111, 111));
		assertSame(group, hit.getGroup());
		assertSame(first, hit.getMember());
		assertNull(group.findMemberAt(new Point(99, 99)));

		final TimerGroupOverlay.DragLayout dragLayout = group.captureDragLayout();
		final TimerGroupOverlay.InsertionTarget startingSlot =
			dragLayout.findInsertionAt(new Point(111, 111), first);
		assertSame(second, startingSlot.getMember());
		assertFalse(startingSlot.isInsertAfter());

		final TimerGroupOverlay.InsertionTarget after =
			dragLayout.findInsertionAt(new Point(111, 200), first);
		assertSame(second, after.getMember());
		assertTrue(after.isInsertAfter());
	}

	@Test
	public void dragSnapshotKeepsTheHorizontalEndSlotReachableDuringLiveReorder()
	{
		final TimerGroupOverlay group = new TimerGroupOverlay(
			fakeClient(),
			null,
			new RingOfTimeConfig() { },
			null,
			"Test group",
			ComponentOrientation.HORIZONTAL
		);
		final FakeTimer first = new FakeTimer("First");
		final FakeTimer second = new FakeTimer("Second");
		final FakeTimer third = new FakeTimer("Third");
		group.addMember(first);
		group.addMember(second);
		group.addMember(third);

		final BufferedImage image = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			group.render(graphics);
		}
		finally
		{
			graphics.dispose();
		}

		final TimerGroupOverlay.DragLayout dragLayout = group.captureDragLayout();
		final TimerGroupOverlay.InsertionTarget startingSlot =
			dragLayout.findInsertionAt(new Point(-10_000, 25), first);
		assertSame(second, startingSlot.getMember());
		assertFalse(startingSlot.isInsertAfter());
		assertFalse(group.moveMember(
			first,
			startingSlot.getMember(),
			startingSlot.isInsertAfter()
		));

		final TimerGroupOverlay.InsertionTarget target =
			dragLayout.findInsertionAt(new Point(10_000, 25), first);
		assertSame(third, target.getMember());
		assertTrue(target.isInsertAfter());

		assertTrue(group.moveMember(first, target.getMember(), target.isInsertAfter()));
		assertEquals(Arrays.asList(second, third, first), group.getMembers());
		assertSame(
			third,
			dragLayout.findInsertionAt(new Point(10_000, 25), first).getMember()
		);

		final TimerGroupOverlay.InsertionTarget farLeft =
			dragLayout.findInsertionAt(new Point(-10_000, 25), first);
		assertTrue(group.moveMember(first, farLeft.getMember(), farLeft.isInsertAfter()));
		assertEquals(Arrays.asList(first, second, third), group.getMembers());
	}

	@Test
	public void overlayNamesAreSafeForRuneLiteConfigurationKeys()
	{
		for (Skill skill : Skill.values())
		{
			final SkillTimerOverlay overlay = new SkillTimerOverlay(
				null,
				null,
				null,
				null,
				skill
			);
			assertConfigurationSafe(overlay.getName());
		}

		for (Effect effect : Effect.values())
		{
			assertConfigurationSafe(EffectTimerOverlay.getOverlayName(effect));
		}

		assertConfigurationSafe(TimerCircleOverlay.persistentName("Group example"));
	}

	private static void assertConfigurationSafe(String name)
	{
		assertFalse(name.startsWith("$"));
		assertFalse(name.contains(":"));
	}

	private static TimerGroupOverlay createGroup()
	{
		return new TimerGroupOverlay(
			null,
			null,
			new RingOfTimeConfig() { },
			null,
			"Test group",
			ComponentOrientation.VERTICAL
		);
	}

	private static Client fakeClient()
	{
		return (Client) Proxy.newProxyInstance(
			Client.class.getClassLoader(),
			new Class<?>[] {Client.class},
			(proxy, method, arguments) ->
			{
				if (method.getName().equals("getMouseCanvasPosition"))
				{
					return new net.runelite.api.Point(-1, -1);
				}
				if (method.getReturnType() == boolean.class)
				{
					return false;
				}
				if (method.getReturnType() == int.class)
				{
					return 0;
				}
				return null;
			}
		);
	}

	private static final class FakeTimer extends TimerCircleOverlay
	{
		private final String name;
		private final boolean active;
		private final Dimension dimension;

		private FakeTimer(boolean active, Dimension dimension)
		{
			this("Fake timer", active, dimension);
		}

		private FakeTimer(String name)
		{
			this(name, true, new Dimension(50, 50));
		}

		private FakeTimer(String name, boolean active, Dimension dimension)
		{
			super(null);
			this.name = name;
			this.active = active;
			this.dimension = dimension;
		}

		@Override
		public String getName()
		{
			return name;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			return new Dimension(dimension);
		}

		@Override
		boolean isTimerActive()
		{
			return active;
		}
	}
}
