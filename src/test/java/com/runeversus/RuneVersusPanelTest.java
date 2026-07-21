package com.runeversus;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.Assert;
import org.junit.Test;

public class RuneVersusPanelTest
{
	@Test
	public void exposesSimpleCompareFlowAndAlwaysShowsClanTools() throws Exception
	{
		AtomicReference<String> comparison = new AtomicReference<>();
		AtomicReference<RuneVersusPanel> panelRef = new AtomicReference<>();

		SwingUtilities.invokeAndWait(() ->
		{
			RuneVersusPanel panel = new RuneVersusPanel();
			panel.setCompareCallback((left, right) -> comparison.set(left + " vs " + right));
			panelRef.set(panel);
		});

		RuneVersusPanel panel = panelRef.get();
		List<JTextField> fields = components(panel, JTextField.class);
		Assert.assertEquals(3, fields.size());
		JButton comparePlayers = button(panel, "Compare players");
		Assert.assertNotNull(comparePlayers);
		JButton clanComparison = button(panel, "Clan member comparison");
		JButton monthlyLeague = button(panel, "Monthly league");
		JButton exportProgress = button(panel, "Export progress card");
		Assert.assertNotNull(clanComparison);
		Assert.assertNotNull(monthlyLeague);
		Assert.assertNotNull(exportProgress);
		Assert.assertTrue(clanComparison.isVisible());
		Assert.assertTrue(monthlyLeague.isVisible());
		Assert.assertTrue(exportProgress.isVisible());
		Assert.assertNull(button(panel, "Fight Night"));
		Assert.assertEquals(new java.awt.Color(255, 193, 7), comparePlayers.getBackground());
		Assert.assertEquals(new java.awt.Color(255, 193, 7), clanComparison.getBackground());
		Assert.assertEquals(new java.awt.Color(255, 193, 7), monthlyLeague.getBackground());
		Assert.assertEquals(new java.awt.Color(255, 193, 7), exportProgress.getBackground());
		SwingUtilities.invokeAndWait(() ->
		{
			fields.get(0).setText("Hakim");
			fields.get(1).setText("Rival");
			fields.get(1).postActionEvent();
		});

		Assert.assertEquals("Hakim vs Rival", comparison.get());
	}

	@Test
	public void keepsContentReadableAndShowsResultsBeforeToolsAtPanelWidth() throws Exception
	{
		AtomicReference<RuneVersusPanel> panelRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() ->
		{
			RuneVersusPanel panel = new RuneVersusPanel();
			panel.setSize(300, 1200);
			layout(panel);
			panelRef.set(panel);
		});

		JButton useMyName = button(panelRef.get(), "Use my name");
		Assert.assertNotNull(useMyName);
		Assert.assertNull(button(panelRef.get(), "Swap"));
		Assert.assertTrue(useMyName.getWidth() >= 220);

		JTextArea playerHelp = textArea(panelRef.get(),
			"Type two names, or load players from your clan.");
		JTextArea clanHelp = textArea(panelRef.get(),
			"Opens a large clan window with five periods, champions, totals, search and ranking filters.");
		Assert.assertNotNull(playerHelp);
		Assert.assertNotNull(clanHelp);
		Assert.assertTrue(playerHelp.getWidth() >= 220);
		Assert.assertTrue(clanHelp.getWidth() >= 220);

		SwingUtilities.invokeAndWait(() -> panelRef.get().showText("24h | XP: Alice +1m"));
		SwingUtilities.invokeAndWait(() -> layout(panelRef.get()));
		JScrollPane output = components(panelRef.get(), JScrollPane.class).get(0);
		Assert.assertTrue(output.isVisible());
		Assert.assertTrue(output.getY() < button(panelRef.get(), "Clan member comparison").getY());
	}

	private static JButton button(Container root, String text)
	{
		for (JButton button : components(root, JButton.class))
		{
			if (text.equals(button.getText()))
			{
				return button;
			}
		}
		return null;
	}

	private static JTextArea textArea(Container root, String text)
	{
		for (JTextArea area : components(root, JTextArea.class))
		{
			if (text.equals(area.getText()))
			{
				return area;
			}
		}
		return null;
	}

	private static <T extends Component> List<T> components(Container root, Class<T> type)
	{
		List<T> out = new ArrayList<>();
		for (Component component : root.getComponents())
		{
			if (type.isInstance(component))
			{
				out.add(type.cast(component));
			}
			if (component instanceof Container)
			{
				out.addAll(components((Container) component, type));
			}
		}
		return out;
	}

	private static void layout(Container root)
	{
		root.doLayout();
		for (Component component : root.getComponents())
		{
			if (component instanceof Container)
			{
				layout((Container) component);
			}
		}
	}
}
