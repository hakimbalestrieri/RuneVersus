package com.runeversus;

import com.runeversus.model.DuelResult;
import com.runeversus.model.PlayerProfile;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CardExporterTest
{
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void preservesExistingCardsWhenNamesAndTimestampsMatch() throws Exception
	{
		File directory = temporaryFolder.newFolder("cards");
		CardExporter exporter = new CardExporter(new StubRenderer(), directory, ignored -> { });
		DuelResult duel = duel();

		File first = exporter.export(duel, false);
		File second = exporter.export(duel, false);

		Assert.assertTrue(first.isFile());
		Assert.assertTrue(second.isFile());
		Assert.assertNotEquals(first.getName(), second.getName());
	}

	@Test
	public void keepsSuccessfulExportWhenClipboardIsUnavailable() throws Exception
	{
		File directory = temporaryFolder.newFolder("clipboard");
		AtomicReference<String> attemptedPath = new AtomicReference<>();
		CardExporter exporter = new CardExporter(new StubRenderer(), directory, path ->
		{
			attemptedPath.set(path);
			throw new IllegalStateException("clipboard busy");
		});

		File exported = exporter.export(duel(), true);

		Assert.assertTrue(exported.isFile());
		Assert.assertEquals(exported.getAbsolutePath(), attemptedPath.get());
	}

	private static DuelResult duel()
	{
		return new DuelResult(
			new PlayerProfile("Alice", null),
			new PlayerProfile("Bob", null),
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyList());
	}

	private static final class StubRenderer extends RuneVersusCardRenderer
	{
		@Override
		public BufferedImage render(DuelResult duel, RuneVersusCardTheme theme, String verdict)
		{
			return new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
		}
	}
}
