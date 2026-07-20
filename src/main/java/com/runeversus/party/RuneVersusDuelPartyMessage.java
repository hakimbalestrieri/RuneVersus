package com.runeversus.party;

import net.runelite.client.party.messages.PartyMemberMessage;

public class RuneVersusDuelPartyMessage extends PartyMemberMessage
{
	public String leftName;
	public String rightName;
	public String winnerName;
	public String verdict;
	public int leftScore;
	public int rightScore;
}
