package com.runeversus;

final class OsrsPlayerName
{
	private static final int MAX_LENGTH = 12;

	private OsrsPlayerName()
	{
	}

	static String requireValid(String value)
	{
		String name = value == null ? "" : value.replace('\u00a0', ' ').trim();
		if (name.isEmpty() || name.length() > MAX_LENGTH)
		{
			throw invalid();
		}
		for (int index = 0; index < name.length(); index++)
		{
			char character = name.charAt(index);
			if (!isAsciiLetter(character)
				&& !(character >= '0' && character <= '9')
				&& character != ' '
				&& character != '-'
				&& character != '_')
			{
				throw invalid();
			}
		}
		return name;
	}

	private static boolean isAsciiLetter(char character)
	{
		return character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z';
	}

	private static IllegalArgumentException invalid()
	{
		return new IllegalArgumentException(
			"An RSN must contain 1-12 letters, numbers, spaces, hyphens, or underscores");
	}
}
