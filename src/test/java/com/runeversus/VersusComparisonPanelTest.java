package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import com.runeversus.model.PlayerProfile;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import org.junit.Assert;
import org.junit.Test;

public class VersusComparisonPanelTest
{
	@Test
	public void filtersCompleteComparisonByCategoryAndMetricName() throws Exception
	{
		AtomicReference<VersusComparisonPanel> panelRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() ->
		{
			VersusComparisonPanel panel = new VersusComparisonPanel(
				() -> { }, () -> { }, () -> { });
			panel.setResult(sampleResult(), null, "Alice wins.");
			panelRef.set(panel);
		});

		VersusComparisonPanel panel = panelRef.get();
		Assert.assertFalse(hasButton(panel, "Compare again"));
		Assert.assertEquals(7, panel.getDisplayedMetricCount());
		Assert.assertEquals("24h XP", panel.getDisplayedMetricName(0));
		Assert.assertEquals("Week XP", panel.getDisplayedMetricName(1));
		Assert.assertEquals("Month XP", panel.getDisplayedMetricName(2));
		Assert.assertEquals("Total XP", panel.getDisplayedMetricName(3));
		Assert.assertTrue(panel.getDisplayedMetricHtml(0).contains("Alice"));
		Assert.assertTrue(panel.getDisplayedMetricHtml(0).contains("Bob"));
		Assert.assertTrue(panel.getDisplayedMetricHtml(0).contains("Alice leads by"));

		SwingUtilities.invokeAndWait(() -> panel.selectCategory(VersusComparisonPanel.Category.SKILLS));
		Assert.assertEquals(2, panel.getDisplayedMetricCount());
		Assert.assertEquals("Attack", panel.getDisplayedMetricName(0));

		SwingUtilities.invokeAndWait(() -> panel.setSearchText("att"));
		Assert.assertEquals(1, panel.getDisplayedMetricCount());
		Assert.assertEquals("Attack", panel.getDisplayedMetricName(0));

		SwingUtilities.invokeAndWait(() ->
		{
			panel.setSearchText("");
			panel.selectCategory(VersusComparisonPanel.Category.RECENT_XP);
		});
		Assert.assertEquals(4, panel.getDisplayedMetricCount());
		Assert.assertEquals("24h XP", panel.getDisplayedMetricName(0));
		Assert.assertEquals("Week XP", panel.getDisplayedMetricName(1));
		Assert.assertEquals("Month XP", panel.getDisplayedMetricName(2));
		Assert.assertEquals("Total XP", panel.getDisplayedMetricName(3));
	}

	private static boolean hasButton(Container root, String text)
	{
		for (Component component : root.getComponents())
		{
			if (component instanceof JButton && text.equals(((JButton) component).getText()))
			{
				return true;
			}
			if (component instanceof Container && hasButton((Container) component, text))
			{
				return true;
			}
		}
		return false;
	}

	private static DuelResult sampleResult()
	{
		return new DuelResult(
			new PlayerProfile("Alice", null),
			new PlayerProfile("Bob", null),
			Arrays.asList(
				new MetricResult(MetricType.SKILL, "Attack", 20_000_000L, 10_000_000L),
				new MetricResult(MetricType.SKILL, "Magic", 5_000_000L, 30_000_000L)),
			Collections.singletonList(new MetricResult(MetricType.BOSS, "Vorkath", 1_200, 900)),
			Arrays.asList(
				new MetricResult(MetricType.COLLECTION_LOG, "Collections Logged", 900, 850),
				new MetricResult(MetricType.COMBAT_ACHIEVEMENTS, "Combat Achievements", 5, 4)),
			Collections.singletonList(new MetricResult(MetricType.FORM_DAY, "Overall", 2_000_000, 1_000_000)),
			Collections.singletonList(new MetricResult(MetricType.FORM_WEEK, "Overall", 6_000_000, 8_000_000)),
			Collections.singletonList(new MetricResult(MetricType.FORM_MONTH, "Overall", 25_000_000, 22_000_000)));
	}
}
