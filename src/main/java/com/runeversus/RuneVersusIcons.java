package com.runeversus;

import com.runeversus.model.MetricResult;
import com.runeversus.model.MetricType;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;

/**
 * Small bridge between RuneVersus UI components and RuneLite's native OSRS sprites.
 */
final class RuneVersusIcons
{
	enum Kind
	{
		XP,
		PVM,
		COLLECTION_LOG,
		COMBAT_ACHIEVEMENTS
	}

	private final SkillIconManager skillIconManager;
	private final ItemManager itemManager;
	private final Map<Kind, BufferedImage> images = new EnumMap<>(Kind.class);
	private final Map<Skill, BufferedImage> skillImages = new EnumMap<>(Skill.class);
	private final Map<JComponent, EnumSet<Kind>> repaintTargets = new WeakHashMap<>();

	RuneVersusIcons(SkillIconManager skillIconManager, ItemManager itemManager)
	{
		this.skillIconManager = skillIconManager;
		this.itemManager = itemManager;
	}

	static RuneVersusIcons empty()
	{
		return new RuneVersusIcons(null, null);
	}

	void apply(JLabel label, Kind kind, int size)
	{
		label.setIcon(icon(kind, size, label));
		label.setIconTextGap(5);
	}

	Icon icon(Kind kind, int size, JComponent repaintTarget)
	{
		BufferedImage image = image(kind, repaintTarget);
		return image == null ? null : new PixelIcon(image, size);
	}

	Icon metricIcon(MetricResult metric, String displayName, int size, JComponent repaintTarget)
	{
		if (metric == null)
		{
			return null;
		}

		MetricType type = metric.getType();
		if (type == MetricType.SKILL)
		{
			Skill skill = findSkill(displayName);
			return skill == null ? icon(Kind.XP, size, repaintTarget)
				: skillIcon(skill, size);
		}
		if (type == MetricType.BOSS)
		{
			return icon(Kind.PVM, size, repaintTarget);
		}
		if (type == MetricType.COLLECTION_LOG)
		{
			return icon(Kind.COLLECTION_LOG, size, repaintTarget);
		}
		if (type == MetricType.COMBAT_ACHIEVEMENTS)
		{
			return icon(Kind.COMBAT_ACHIEVEMENTS, size, repaintTarget);
		}
		if (type == MetricType.FORM_DAY || type == MetricType.FORM_WEEK || type == MetricType.FORM_MONTH)
		{
			return icon(Kind.XP, size, repaintTarget);
		}
		return null;
	}

	private Icon skillIcon(Skill skill, int size)
	{
		if (skillIconManager == null)
		{
			return null;
		}
		BufferedImage image = skillImages.computeIfAbsent(skill, skillIconManager::getSkillImage);
		return image == null ? null : new PixelIcon(image, size);
	}

	private BufferedImage image(Kind kind, JComponent repaintTarget)
	{
		BufferedImage image = images.get(kind);
		if (image == null)
		{
			switch (kind)
			{
				case XP:
					if (skillIconManager != null)
					{
						// RuneLite ships overall.png, but Skill.OVERALL is intentionally null.
						image = ImageUtil.loadImageResource(
							SkillIconManager.class, "/skill_icons/overall.png");
					}
					break;
				case PVM:
					image = itemImage(ItemID.SLAYER_HELM);
					break;
				case COLLECTION_LOG:
					image = itemImage(ItemID.COLLECTION_LOG);
					break;
				case COMBAT_ACHIEVEMENTS:
					image = itemImage(ItemID.CA_BOOK);
					break;
				default:
					break;
			}
			if (image != null)
			{
				images.put(kind, image);
			}
		}

		registerRepaint(kind, image, repaintTarget);
		return image;
	}

	private BufferedImage itemImage(int itemId)
	{
		return itemManager == null ? null : itemManager.getImage(itemId);
	}

	private void registerRepaint(Kind kind, BufferedImage image, JComponent target)
	{
		if (!(image instanceof AsyncBufferedImage) || target == null)
		{
			return;
		}

		synchronized (repaintTargets)
		{
			EnumSet<Kind> registered = repaintTargets.computeIfAbsent(
				target, ignored -> EnumSet.noneOf(Kind.class));
			if (!registered.add(kind))
			{
				return;
			}
		}

		((AsyncBufferedImage) image).onLoaded(() -> SwingUtilities.invokeLater(target::repaint));
	}

	private static Skill findSkill(String displayName)
	{
		String name = displayName == null ? "" : displayName.trim();
		for (Skill skill : Skill.values())
		{
			if (skill.getName().equalsIgnoreCase(name))
			{
				return skill;
			}
		}
		return null;
	}

	private static final class PixelIcon implements Icon
	{
		private final BufferedImage image;
		private final int size;

		private PixelIcon(BufferedImage image, int size)
		{
			this.image = image;
			this.size = size;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y)
		{
			int sourceWidth = Math.max(1, image.getWidth());
			int sourceHeight = Math.max(1, image.getHeight());
			double scale = Math.min((double) size / sourceWidth, (double) size / sourceHeight);
			int width = Math.max(1, (int) Math.round(sourceWidth * scale));
			int height = Math.max(1, (int) Math.round(sourceHeight * scale));
			int drawX = x + (size - width) / 2;
			int drawY = y + (size - height) / 2;
			Graphics2D graphics2D = (Graphics2D) graphics.create();
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			graphics2D.drawImage(image, drawX, drawY, width, height, null);
			graphics2D.dispose();
		}

		@Override
		public int getIconWidth()
		{
			return size;
		}

		@Override
		public int getIconHeight()
		{
			return size;
		}
	}
}
