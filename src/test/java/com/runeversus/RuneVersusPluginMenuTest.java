package com.runeversus;

import net.runelite.api.MenuAction;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.WidgetUtil;
import org.junit.Assert;
import org.junit.Test;

public class RuneVersusPluginMenuTest
{
	@Test
	public void recognizesClanMemberListWidgets()
	{
		Assert.assertTrue(RuneVersusPlugin.isClanMemberComponent(
			InterfaceID.ClansSidepanel.PLAYERLIST, MenuAction.CC_OP));
		Assert.assertTrue(RuneVersusPlugin.isClanMemberComponent(
			InterfaceID.ClansGuestSidepanel.PLAYERLIST, MenuAction.CC_OP_LOW_PRIORITY));
		Assert.assertTrue(RuneVersusPlugin.isClanMemberComponent(
			WidgetUtil.packComponentId(InterfaceID.CLANS_MEMBERS, 1), MenuAction.CC_OP));
		Assert.assertFalse(RuneVersusPlugin.isClanMemberComponent(
			WidgetUtil.packComponentId(InterfaceID.FRIENDS, 1), MenuAction.CC_OP));
		Assert.assertFalse(RuneVersusPlugin.isClanMemberComponent(
			InterfaceID.ClansSidepanel.PLAYERLIST, MenuAction.CANCEL));
	}

	@Test
	public void stripsRuneLiteTagsFromClanMemberNames()
	{
		Assert.assertEquals("SKOPOKS", RuneVersusPlugin.clean("<col=ff9040>SKOPOKS</col>"));
		Assert.assertEquals("Nithod", RuneVersusPlugin.clean("<img=5><col=ff9040>Nithod</col>"));
	}
}
