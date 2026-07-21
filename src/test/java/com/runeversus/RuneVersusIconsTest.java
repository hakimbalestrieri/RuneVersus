package com.runeversus;

import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import javax.swing.Icon;
import javax.swing.JLabel;
import net.runelite.client.game.SkillIconManager;
import org.junit.Assert;
import org.junit.Test;

public class RuneVersusIconsTest
{
	@Test
	public void usesNativeRuneLiteSkillSpritesAndAllowsMissingItemService()
	{
		RuneVersusIcons icons = new RuneVersusIcons(new SkillIconManager(), null);
		Icon attack = icons.metricIcon(
			new MetricResult(MetricType.SKILL, "Attack", 1L, 0L),
			"Attack",
			20,
			new JLabel());

		Assert.assertNotNull(attack);
		Assert.assertEquals(20, attack.getIconWidth());
		Assert.assertEquals(20, attack.getIconHeight());
		Assert.assertNotNull(icons.icon(RuneVersusIcons.Kind.XP, 18, new JLabel()));
		Assert.assertNull(icons.icon(RuneVersusIcons.Kind.PVM, 18, new JLabel()));
	}
}
