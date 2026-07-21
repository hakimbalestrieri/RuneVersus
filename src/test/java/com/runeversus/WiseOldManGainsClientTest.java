package com.runeversus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Assert;
import org.junit.Test;

public class WiseOldManGainsClientTest
{
	private static final MediaType JSON = MediaType.parse("application/json");

	@Test
	public void identifiesRequestsAndCachesRepeatedPlayerGains() throws Exception
	{
		AtomicInteger requests = new AtomicInteger();
		AtomicReference<Request> captured = new AtomicReference<>();
		OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain ->
		{
			requests.incrementAndGet();
			captured.set(chain.request());
			return response(chain.request(), 200,
				"{\"data\":{\"skills\":{\"overall\":{\"experience\":{\"gained\":123}}}}}");
		}).build();
		WiseOldManRequestGate gate = new WiseOldManRequestGate(
			20, 60_000L, 0L, System::currentTimeMillis, Thread::sleep);
		WiseOldManGainsClient client = new WiseOldManGainsClient(http, new Gson(), gate);

		Assert.assertEquals(123L,
			client.getSkillExperienceGains("Alice", "day").get(net.runelite.client.hiscore.HiscoreSkill.OVERALL).longValue());
		client.getSkillExperienceGains("Alice", "day");

		Assert.assertEquals(1, requests.get());
		Assert.assertTrue(captured.get().header("User-Agent").startsWith("RuneVersus/"));
		Assert.assertEquals("application/json", captured.get().header("Accept"));
	}

	@Test
	public void turnsHttp429IntoSharedRetryAfterCooldown() throws Exception
	{
		OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain ->
			response(chain.request(), 429, "{}", "Retry-After", "12")).build();
		WiseOldManRequestGate gate = new WiseOldManRequestGate(
			20, 60_000L, 0L, System::currentTimeMillis, Thread::sleep);
		WiseOldManGainsClient client = new WiseOldManGainsClient(http, new Gson(), gate);

		try
		{
			client.getSkillExperienceGains("Alice", "day");
			Assert.fail("Expected WOM HTTP 429 to trigger backoff");
		}
		catch (WiseOldManRateLimitException ex)
		{
			Assert.assertEquals(12L, ex.getRetryAfterSeconds());
		}
	}

	@Test
	public void parsesMonthlyLeagueEfficiencyAndCollectionGains()
	{
		String json = "[{"
			+ "\"player\":{\"id\":4156,\"username\":\"alice\",\"displayName\":\"Alice\",\"type\":\"ironman\"},"
			+ "\"startDate\":\"2026-07-01T01:00:00Z\","
			+ "\"endDate\":\"2026-07-20T12:00:00Z\","
			+ "\"data\":["
			+ "{\"metric\":\"ehp\",\"gained\":12.75},"
			+ "{\"metric\":\"ehb\",\"gained\":8.5},"
			+ "{\"metric\":\"collections_logged\",\"gained\":6},"
			+ "{\"metric\":\"overall\",\"gained\":1234567}]}]";

		List<MonthlyLeagueParticipant> parsed = WiseOldManGainsClient.parseMonthlyLeagueGains(
			new Gson().fromJson(json, JsonArray.class));

		Assert.assertEquals(1, parsed.size());
		MonthlyLeagueParticipant participant = parsed.get(0);
		Assert.assertEquals(4156L, participant.getPlayerId());
		Assert.assertEquals("Alice", participant.getName());
		Assert.assertEquals("ironman", participant.getAccountType());
		Assert.assertEquals(12.75, participant.getEhpGained(), 0.001);
		Assert.assertEquals(8.5, participant.getEhbGained(), 0.001);
		Assert.assertEquals(6L, participant.getCollectionsGained());
		Assert.assertEquals(Instant.parse("2026-07-01T01:00:00Z"), participant.getTrackedFrom());
	}

	@Test
	public void parsesClanJoinDateFromGroupMemberships()
	{
		String json = "{\"memberships\":[{"
			+ "\"playerId\":4156,"
			+ "\"clientSyncJoinedAt\":\"2026-06-14T12:00:00Z\","
			+ "\"createdAt\":\"2026-06-20T12:00:00Z\","
			+ "\"player\":{\"id\":4156,\"displayName\":\"Alice\",\"type\":\"ironman\"}}]}";

		List<MonthlyLeagueMembership> memberships = WiseOldManGainsClient.parseGroupMemberships(
			new Gson().fromJson(json, JsonObject.class));

		Assert.assertEquals(1, memberships.size());
		Assert.assertEquals(4156L, memberships.get(0).getPlayerId());
		Assert.assertEquals("Alice", memberships.get(0).getName());
		Assert.assertEquals(Instant.parse("2026-06-14T12:00:00Z"), memberships.get(0).getJoinedAt());
	}

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

	private static Response response(Request request, int code, String body, String... header)
	{
		Response.Builder response = new Response.Builder()
			.request(request)
			.protocol(Protocol.HTTP_1_1)
			.code(code)
			.message(code == 200 ? "OK" : "Too Many Requests")
			.body(ResponseBody.create(JSON, body));
		if (header.length == 2)
		{
			response.header(header[0], header[1]);
		}
		return response.build();
	}

}
