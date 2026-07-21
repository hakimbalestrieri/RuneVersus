package com.runeversus;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.Assert;
import org.junit.Test;

public class MonthlyLeaguePanelTest
{
	@Test
	public void presentsLivePodiumAndReordersByDiscipline() throws Exception
	{
		MonthlyLeagueSeason season = sampleSeason();
		AtomicReference<MonthlyLeaguePanel> panelRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() ->
		{
			MonthlyLeaguePanel panel = new MonthlyLeaguePanel(
				() -> { }, ignored -> { }, () -> { }, ignored -> { }, () -> { });
			panel.setSeason(season);
			panelRef.set(panel);
		});

		MonthlyLeaguePanel panel = panelRef.get();
		Assert.assertEquals("LIVE", panel.getStateText());
		Assert.assertEquals(4, panel.getDisplayedPlayerCount());
		Assert.assertEquals("Balanced", panel.getDisplayedPlayerName(0));

		SwingUtilities.invokeAndWait(() -> panel.selectView(MonthlyLeagueMetric.PVM));
		Assert.assertEquals("Pvmer", panel.getDisplayedPlayerName(0));
		Assert.assertEquals("Late joiner", panel.getDisplayedPlayerName(3));
	}

	static MonthlyLeagueSeason sampleSeason()
	{
		Instant start = Instant.parse("2026-07-01T00:00:00Z");
		Instant now = Instant.parse("2026-07-21T12:00:00Z");
		return new MonthlyLeagueSeason(139, YearMonth.of(2026, 7), now, Arrays.asList(
			participant("Balanced", 28.4, 24.2, 3, start.plusSeconds(3_600), now),
			participant("Skiller", 52.8, 3.1, 18, start.plusSeconds(7_200), now),
			participant("Pvmer", 4.6, 49.9, 2, start.plusSeconds(8_000), now),
			participant("Late joiner", 80.0, 90.0, 40, start.plusSeconds(10 * 86_400L), now)));
	}

	private static MonthlyLeagueParticipant participant(
		String name,
		double ehp,
		double ehb,
		long clogs,
		Instant start,
		Instant end)
	{
		return new MonthlyLeagueParticipant(name, "regular", ehp, ehb, clogs, start, end);
	}
}
