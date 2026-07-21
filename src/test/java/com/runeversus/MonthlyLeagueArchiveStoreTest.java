package com.runeversus;

import java.io.File;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MonthlyLeagueArchiveStoreTest
{
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void persistsFrozenRosterAndFinalResult() throws Exception
	{
		File directory = temporaryFolder.newFolder("leagues");
		MonthlyLeagueArchiveStore store = new MonthlyLeagueArchiveStore(directory);
		Instant frozenAt = Instant.parse("2026-07-01T02:00:00Z");
		Instant finalizedAt = Instant.parse("2026-08-01T00:05:00Z");
		MonthlyLeagueParticipant participant = new MonthlyLeagueParticipant(
			4156L,
			"Alice",
			"ironman",
			12.5,
			8.25,
			4L,
			Instant.parse("2026-07-01T00:10:00Z"),
			Instant.parse("2026-07-31T23:30:00Z"),
			Instant.parse("2026-06-01T00:00:00Z"),
			true);

		store.save(139, YearMonth.of(2026, 7), frozenAt, finalizedAt,
			Collections.singletonList(participant));
		MonthlyLeagueArchiveStore.Archive archive = store.load(139, YearMonth.of(2026, 7));

		Assert.assertNotNull(archive);
		Assert.assertTrue(archive.isFinalized());
		Assert.assertEquals(frozenAt, archive.getFrozenAt());
		Assert.assertEquals(finalizedAt, archive.getFinalizedAt());
		Assert.assertEquals(1, archive.getParticipants().size());
		MonthlyLeagueParticipant restored = archive.getParticipants().get(0);
		Assert.assertEquals(4156L, restored.getPlayerId());
		Assert.assertEquals("Alice", restored.getName());
		Assert.assertTrue(restored.isRosterEligible());
		Assert.assertEquals(12.5, restored.getEhpGained(), 0.001);
	}
}
