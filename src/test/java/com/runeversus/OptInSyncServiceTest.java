package com.runeversus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Properties;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class OptInSyncServiceTest
{
	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private final RuneVersusConfig enabledConfig = new RuneVersusConfig()
	{
		@Override
		public boolean localOptInSync()
		{
			return true;
		}
	};

	@Test
	public void loadsSupportedPrivateFields() throws Exception
	{
		File directory = temporaryFolder.newFolder("sync");
		Properties properties = new Properties();
		properties.setProperty("pb.vorkath", "42");
		properties.setProperty("collection.items", "987");
		properties.setProperty("ca.tier", "elite");
		try (FileOutputStream output = new FileOutputStream(new File(directory, "Alice.properties")))
		{
			properties.store(output, "test");
		}

		SyncedPlayerData data = new OptInSyncService(enabledConfig, directory).load("Alice");

		Assert.assertEquals(42L, data.getPersonalBests().get("vorkath").longValue());
		Assert.assertEquals(987L, data.getCollectionItems());
		Assert.assertEquals(CombatAchievementTier.ELITE, data.getCombatAchievementTier());
	}

	@Test
	public void ignoresOversizedPrivateDataFiles() throws Exception
	{
		File directory = temporaryFolder.newFolder("large-sync");
		File file = new File(directory, "Alice.properties");
		try (RandomAccessFile output = new RandomAccessFile(file, "rw"))
		{
			output.setLength(1024L * 1024L + 1L);
		}

		Assert.assertTrue(new OptInSyncService(enabledConfig, directory).load("Alice").isEmpty());
	}
}
