package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import com.runeversus.model.PlayerProfile;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;

public class RuneVersusCardRendererTest
{
	@Test
	public void rendersNonBlankDuelCard()
	{
		DuelResult duel = new DuelResult(
			new PlayerProfile("Hakim", null),
			new PlayerProfile("Rival", null),
			Arrays.asList(
				new MetricResult(MetricType.SKILL, "Attack", 20_000_000, 13_000_000),
				new MetricResult(MetricType.SKILL, "Magic", 15_000_000, 32_000_000)),
			Arrays.asList(
				new MetricResult(MetricType.BOSS, "Vorkath", 1200, 930),
				new MetricResult(MetricType.BOSS, "Zulrah", 54, 2200)),
			Collections.singletonList(new MetricResult(MetricType.COLLECTION_LOG, "Collections Logged", 890, 820)),
			Collections.singletonList(new MetricResult(MetricType.FORM_DAY, "Overall", 240_000, 110_000)),
			Collections.emptyList(),
			Collections.emptyList());

		BufferedImage image = new RuneVersusCardRenderer().render(duel);
		Assert.assertEquals(1200, image.getWidth());
		Assert.assertEquals(675, image.getHeight());

		int differentPixels = 0;
		int first = image.getRGB(0, 0);
		for (int y = 0; y < image.getHeight(); y += 25)
		{
			for (int x = 0; x < image.getWidth(); x += 25)
			{
				if (image.getRGB(x, y) != first)
				{
					differentPixels++;
				}
			}
		}
		Assert.assertTrue(differentPixels > 50);
	}
}
