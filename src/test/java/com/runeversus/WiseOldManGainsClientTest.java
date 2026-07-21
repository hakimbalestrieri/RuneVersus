package com.runeversus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class WiseOldManGainsClientTest
{
	@Test
	public void parsesOverallCollectionsAndSummedBossKc()
	{
		String json = "[{"
			+ "\"player\":{\"username\":\"alice\",\"displayName\":\"Alice\"},"
			+ "\"data\":["
			+ "{\"metric\":\"overall\",\"gained\":123456},"
			+ "{\"metric\":\"collections_logged\",\"gained\":2},"
			+ "{\"metric\":\"vorkath\",\"gained\":10},"
			+ "{\"metric\":\"chambers_of_xeric\",\"gained\":5},"
			+ "{\"metric\":\"attack\",\"gained\":99999}]}]";

		JsonArray root = new Gson().fromJson(json, JsonArray.class);
		Map<String, ClanProgressGains> parsed = WiseOldManGainsClient.parseGroupGains(root);

		Assert.assertEquals(1, parsed.size());
		ClanProgressGains gains = parsed.get("Alice");
		Assert.assertNotNull(gains);
		Assert.assertEquals(123456L, gains.getXp());
		Assert.assertEquals(2L, gains.getCollections());
		Assert.assertEquals(15L, gains.getBossKc());
		Assert.assertEquals(10L, gains.getBossKc("Vorkath"));
		Assert.assertEquals(5L, gains.getBossKc("Chambers of Xeric"));
	}

	@Test
	public void parsesAllTimeTotalsFromBulkHiscores()
	{
		String json = "[{"
			+ "\"player\":{\"username\":\"alice\",\"displayName\":\"Alice\",\"exp\":999},"
			+ "\"data\":{\"data\":{"
			+ "\"skills\":{\"overall\":{\"experience\":123456789}},"
			+ "\"activities\":{\"collections_logged\":{\"score\":987}},"
			+ "\"bosses\":{\"vorkath\":{\"kills\":1200},\"zulrah\":{\"kills\":-1},"
			+ "\"chambers_of_xeric\":{\"kills\":300}}}}}]";

		JsonArray root = new Gson().fromJson(json, JsonArray.class);
		ClanProgressGains totals = WiseOldManGainsClient.parseGroupAllTime(root).get("Alice");

		Assert.assertNotNull(totals);
		Assert.assertEquals(123456789L, totals.getXp());
		Assert.assertEquals(987L, totals.getCollections());
		Assert.assertEquals(1500L, totals.getBossKc());
		Assert.assertEquals(1200L, totals.getBossKc("Vorkath"));
		Assert.assertEquals(300L, totals.getBossKc("Chambers of Xeric"));
	}

}
