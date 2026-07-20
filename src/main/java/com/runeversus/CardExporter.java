package com.runeversus;

import com.runeversus.model.DuelResult;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;

@Singleton
public class CardExporter
{
	private static final DateTimeFormatter FILE_TIME =
		DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

	private final RuneVersusCardRenderer renderer;

	@Inject
	public CardExporter(RuneVersusCardRenderer renderer)
	{
		this.renderer = renderer;
	}

	public File export(DuelResult duel, boolean copyPathToClipboard) throws IOException
	{
		return export(duel, copyPathToClipboard, RuneVersusCardTheme.AUTO, duel.getVerdict());
	}

	public File export(DuelResult duel, boolean copyPathToClipboard, RuneVersusCardTheme theme, String verdict) throws IOException
	{
		File dir = new File(RuneLite.RUNELITE_DIR, "rune-versus/cards");
		if (!dir.exists() && !dir.mkdirs())
		{
			throw new IOException("Unable to create export directory: " + dir);
		}

		String name = sanitize(duel.getLeft().getName()) + "-vs-" + sanitize(duel.getRight().getName())
			+ "-" + FILE_TIME.format(duel.getCreatedAt()) + ".png";
		File out = new File(dir, name);
		BufferedImage image = renderer.render(duel, theme, verdict);
		ImageIO.write(image, "png", out);

		if (copyPathToClipboard)
		{
			Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new StringSelection(out.getAbsolutePath()), null);
		}
		return out;
	}

	public File exportRecap(RosterLeaderboard leaderboard, boolean copyPathToClipboard, RuneVersusCardTheme theme) throws IOException
	{
		File dir = new File(RuneLite.RUNELITE_DIR, "rune-versus/cards");
		if (!dir.exists() && !dir.mkdirs())
		{
			throw new IOException("Unable to create export directory: " + dir);
		}

		String name = sanitize(leaderboard.getLabel()) + "-recap-" + FILE_TIME.format(leaderboard.getCreatedAt()) + ".png";
		File out = new File(dir, name);
		BufferedImage image = renderer.renderRosterRecap(leaderboard, theme);
		ImageIO.write(image, "png", out);

		if (copyPathToClipboard)
		{
			Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new StringSelection(out.getAbsolutePath()), null);
		}
		return out;
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
