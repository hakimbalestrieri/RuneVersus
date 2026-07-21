package com.runeversus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;

@Singleton
public class MonthlyLeagueArchiveStore
{
	private static final String SCHEMA_VERSION = "1";
	private static final int MAX_PARTICIPANTS = 2_000;
	private final File directory;

	@Inject
	public MonthlyLeagueArchiveStore()
	{
		this(new File(RuneLite.RUNELITE_DIR, "rune-versus/leagues"));
	}

	MonthlyLeagueArchiveStore(File directory)
	{
		this.directory = directory;
	}

	synchronized Archive load(int groupId, YearMonth month)
	{
		File file = archiveFile(groupId, month);
		if (!file.isFile())
		{
			return null;
		}

		Properties properties = new Properties();
		try (FileInputStream input = new FileInputStream(file))
		{
			properties.load(input);
			if (!SCHEMA_VERSION.equals(properties.getProperty("schema"))
				|| groupId != intValue(properties, "groupId")
				|| !month.toString().equals(properties.getProperty("month")))
			{
				return null;
			}

			int count = intValue(properties, "participant.count");
			if (count < 0 || count > MAX_PARTICIPANTS)
			{
				return null;
			}
			List<MonthlyLeagueParticipant> participants = new ArrayList<>();
			for (int index = 0; index < count; index++)
			{
				String prefix = "participant." + index + ".";
				String name = properties.getProperty(prefix + "name", "").trim();
				if (name.isEmpty())
				{
					continue;
				}
				participants.add(new MonthlyLeagueParticipant(
					longValue(properties, prefix + "playerId"),
					name,
					properties.getProperty(prefix + "accountType", "unknown"),
					doubleValue(properties, prefix + "ehp"),
					doubleValue(properties, prefix + "ehb"),
					longValue(properties, prefix + "collections"),
					instantValue(properties, prefix + "trackedFrom"),
					instantValue(properties, prefix + "trackedUntil"),
					instantValue(properties, prefix + "joinedAt"),
					Boolean.parseBoolean(properties.getProperty(prefix + "rosterEligible", "false"))));
			}
			return new Archive(
				instantValue(properties, "frozenAt"),
				instantValue(properties, "finalizedAt"),
				participants);
		}
		catch (IOException | RuntimeException ex)
		{
			return null;
		}
	}

	synchronized void save(
		int groupId,
		YearMonth month,
		Instant frozenAt,
		Instant finalizedAt,
		List<MonthlyLeagueParticipant> participants) throws IOException
	{
		Files.createDirectories(directory.toPath());
		Properties properties = new Properties();
		properties.setProperty("schema", SCHEMA_VERSION);
		properties.setProperty("groupId", String.valueOf(groupId));
		properties.setProperty("month", month.toString());
		setInstant(properties, "frozenAt", frozenAt);
		setInstant(properties, "finalizedAt", finalizedAt);
		List<MonthlyLeagueParticipant> values = participants == null
			? Collections.emptyList() : participants;
		if (values.size() > MAX_PARTICIPANTS)
		{
			throw new IOException("Monthly league roster is too large to archive safely");
		}
		properties.setProperty("participant.count", String.valueOf(values.size()));
		for (int index = 0; index < values.size(); index++)
		{
			MonthlyLeagueParticipant participant = values.get(index);
			String prefix = "participant." + index + ".";
			properties.setProperty(prefix + "playerId", String.valueOf(participant.getPlayerId()));
			properties.setProperty(prefix + "name", participant.getName());
			properties.setProperty(prefix + "accountType", participant.getAccountType());
			properties.setProperty(prefix + "ehp", String.valueOf(participant.getEhpGained()));
			properties.setProperty(prefix + "ehb", String.valueOf(participant.getEhbGained()));
			properties.setProperty(prefix + "collections", String.valueOf(participant.getCollectionsGained()));
			setInstant(properties, prefix + "trackedFrom", participant.getTrackedFrom());
			setInstant(properties, prefix + "trackedUntil", participant.getTrackedUntil());
			setInstant(properties, prefix + "joinedAt", participant.getJoinedAt());
			properties.setProperty(prefix + "rosterEligible", String.valueOf(participant.isRosterEligible()));
		}

		File temporary = File.createTempFile("runeversus-league-", ".tmp", directory);
		try
		{
			try (FileOutputStream output = new FileOutputStream(temporary))
			{
				properties.store(output, "RuneVersus monthly league snapshot");
			}
			moveAtomically(temporary, archiveFile(groupId, month));
		}
		finally
		{
			Files.deleteIfExists(temporary.toPath());
		}
	}

	private File archiveFile(int groupId, YearMonth month)
	{
		return new File(directory, "group-" + Math.max(0, groupId) + "-" + month + ".properties");
	}

	private static void moveAtomically(File source, File target) throws IOException
	{
		try
		{
			Files.move(source.toPath(), target.toPath(),
				StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (AtomicMoveNotSupportedException ex)
		{
			Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void setInstant(Properties properties, String key, Instant value)
	{
		if (value != null)
		{
			properties.setProperty(key, value.toString());
		}
	}

	private static int intValue(Properties properties, String key)
	{
		long value = longValue(properties, key);
		return value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ? 0 : (int) value;
	}

	private static long longValue(Properties properties, String key)
	{
		try
		{
			return Long.parseLong(properties.getProperty(key, "0").trim());
		}
		catch (NumberFormatException ex)
		{
			return 0L;
		}
	}

	private static double doubleValue(Properties properties, String key)
	{
		try
		{
			double value = Double.parseDouble(properties.getProperty(key, "0").trim());
			return Double.isFinite(value) ? Math.max(0.0, value) : 0.0;
		}
		catch (NumberFormatException ex)
		{
			return 0.0;
		}
	}

	private static Instant instantValue(Properties properties, String key)
	{
		String value = properties.getProperty(key, "").trim();
		if (value.isEmpty())
		{
			return null;
		}
		try
		{
			return Instant.parse(value);
		}
		catch (DateTimeParseException ex)
		{
			return null;
		}
	}

	static final class Archive
	{
		private final Instant frozenAt;
		private final Instant finalizedAt;
		private final List<MonthlyLeagueParticipant> participants;

		Archive(
			Instant frozenAt,
			Instant finalizedAt,
			List<MonthlyLeagueParticipant> participants)
		{
			this.frozenAt = frozenAt;
			this.finalizedAt = finalizedAt;
			this.participants = Collections.unmodifiableList(new ArrayList<>(participants));
		}

		Instant getFrozenAt()
		{
			return frozenAt;
		}

		Instant getFinalizedAt()
		{
			return finalizedAt;
		}

		boolean isFinalized()
		{
			return finalizedAt != null;
		}

		List<MonthlyLeagueParticipant> getParticipants()
		{
			return participants;
		}
	}
}
