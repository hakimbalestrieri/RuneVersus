package com.runeversus;

import com.runeversus.model.DuelResult;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;

@Singleton
public class CardExporter
{
	private static final DateTimeFormatter FILE_TIME =
		DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());
	private static final int MAX_FILENAME_ATTEMPTS = 10_000;

	private final RuneVersusCardRenderer renderer;
	private final File directory;
	private final Consumer<String> clipboardWriter;

	@Inject
	public CardExporter(RuneVersusCardRenderer renderer)
	{
		this(
			renderer,
			new File(RuneLite.RUNELITE_DIR, "rune-versus/cards"),
			CardExporter::copyToSystemClipboard);
	}

	CardExporter(RuneVersusCardRenderer renderer, File directory, Consumer<String> clipboardWriter)
	{
		this.renderer = Objects.requireNonNull(renderer);
		this.directory = Objects.requireNonNull(directory);
		this.clipboardWriter = Objects.requireNonNull(clipboardWriter);
	}

	public File export(DuelResult duel, boolean copyPathToClipboard) throws IOException
	{
		return export(duel, copyPathToClipboard, RuneVersusCardTheme.AUTO, duel.getVerdict());
	}

	public File export(DuelResult duel, boolean copyPathToClipboard, RuneVersusCardTheme theme, String verdict) throws IOException
	{
		String name = sanitize(duel.getLeft().getName()) + "-vs-" + sanitize(duel.getRight().getName())
			+ "-" + FILE_TIME.format(duel.getCreatedAt()) + ".png";
		return writeCard(name, renderer.render(duel, theme, verdict), copyPathToClipboard);
	}

	public File exportClanProgress(
		ClanProgressLeaderboard leaderboard,
		boolean copyPathToClipboard,
		RuneVersusCardTheme theme) throws IOException
	{
		String name = sanitize(leaderboard.getLabel()) + "-progress-"
			+ FILE_TIME.format(leaderboard.getCreatedAt()) + ".png";
		return writeCard(name, renderer.renderClanProgress(leaderboard, theme), copyPathToClipboard);
	}

	public File exportMonthlyLeague(
		MonthlyLeagueSeason season,
		boolean copyPathToClipboard,
		RuneVersusCardTheme theme) throws IOException
	{
		String name = "monthly-league-" + season.getMonth() + "-"
			+ FILE_TIME.format(season.getGeneratedAt()) + ".png";
		return writeCard(name, renderer.renderMonthlyLeague(season, theme), copyPathToClipboard);
	}

	private File writeCard(String fileName, BufferedImage image, boolean copyPathToClipboard) throws IOException
	{
		Path output = reserveOutput(fileName);
		boolean written = false;
		try
		{
			if (!ImageIO.write(image, "png", output.toFile()))
			{
				throw new IOException("No PNG image writer is available");
			}
			written = true;
		}
		finally
		{
			if (!written)
			{
				Files.deleteIfExists(output);
			}
		}

		File exported = output.toFile();
		if (copyPathToClipboard)
		{
			try
			{
				clipboardWriter.accept(exported.getAbsolutePath());
			}
			catch (RuntimeException ignored)
			{
				// The PNG was saved successfully. A busy or unavailable clipboard must not turn that into a failed export.
			}
		}
		return exported;
	}

	private Path reserveOutput(String fileName) throws IOException
	{
		Path exportDirectory = directory.toPath().toAbsolutePath().normalize();
		Files.createDirectories(exportDirectory);
		String stem = fileName.endsWith(".png")
			? fileName.substring(0, fileName.length() - 4) : fileName;
		for (int attempt = 1; attempt <= MAX_FILENAME_ATTEMPTS; attempt++)
		{
			String candidateName = attempt == 1 ? stem + ".png" : stem + "-" + attempt + ".png";
			Path candidate = exportDirectory.resolve(candidateName).normalize();
			if (!exportDirectory.equals(candidate.getParent()))
			{
				throw new IOException("Invalid card filename");
			}
			try
			{
				return Files.createFile(candidate);
			}
			catch (FileAlreadyExistsException ignored)
			{
				// Keep existing exports and reserve the next suffix atomically.
			}
		}
		throw new IOException("Too many cards share the same filename");
	}

	private static void copyToSystemClipboard(String path)
	{
		Toolkit.getDefaultToolkit().getSystemClipboard()
			.setContents(new StringSelection(path), null);
	}

	private static String sanitize(String name)
	{
		if (name == null || name.trim().isEmpty())
		{
			return "player";
		}
		return name.trim().replaceAll("[^a-zA-Z0-9_-]+", "_");
	}
}
