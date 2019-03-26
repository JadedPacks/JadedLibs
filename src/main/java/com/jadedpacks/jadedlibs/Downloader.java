package com.jadedpacks.jadedlibs;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class Downloader extends JOptionPane implements IDownloader {
	private boolean stop;
	private Thread thread;
	private JProgressBar progress;
	private JLabel currentActivity;
	private JDialog container;

	@Override
	public void setThread(final Thread thread) {
		this.thread = thread;
	}

	@Override
	public void resetProgress(final int sizeGuess) {
		if(progress != null) {
			progress.getModel().setRangeProperties(0, 0, 0, sizeGuess, false);
		}
	}

	@Override
	public void updateProgress(final int sizeGuess) {
		if(progress != null) {
			progress.getModel().setValue(sizeGuess);
		}
	}

	@Override
	public void updateProgressString(final String progress) {
		if(currentActivity != null) {
			currentActivity.setText(progress);
		}
	}

	@Override
	public boolean shouldStop() {
		return stop;
	}

	@Override
	public Object makeDialog() {
		if(container != null) {
			return container;
		}
		setMessageType(JOptionPane.INFORMATION_MESSAGE);
		setMessage(makeProgressPanel());
		setOptions(new Object[]{"Stop"});
		addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent evt) {
				if(evt.getSource() == this && evt.getPropertyName().equals("value")) {
					requestClose("This will stop minecraft from launching\nAre you sure you want to do this?");
				}
			}
		});
		container = new JDialog(null, "JadedPacks Downloader", Dialog.ModalityType.MODELESS);
		container.setResizable(false);
		container.setLocationRelativeTo(null);
		container.add(this);
		updateUI();
		container.pack();
		container.setMinimumSize(container.getPreferredSize());
		container.setVisible(true);
		container.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		container.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(final WindowEvent e) {
				requestClose("Closing this window will stop minecraft from launching\nAre you sure you wish to do this?");
			}
		});
		return container;
	}

	@Override
	public void showErrorDialog(final String name, final String url) {
		final JEditorPane ep = new JEditorPane("text/html", "<html>JadedPacks was unable to download required library " + name + "<br>Check your internet connection and try restarting or download it manually from<br><a href=\"" + url + "\">" + url + "</a> and put it in your mods folder</html>");
		ep.setEditable(false);
		ep.setOpaque(false);
		ep.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(final HyperlinkEvent event) {
				try {
					if(event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
						Desktop.getDesktop().browse(event.getURL().toURI());
					}
				} catch(final Exception e) {
					e.printStackTrace();
				}
			}
		});
		JOptionPane.showMessageDialog(null, ep, "A download error has occured", JOptionPane.ERROR_MESSAGE);
	}

	private Box makeProgressPanel() {
		final Box box = Box.createVerticalBox();
		box.add(Box.createRigidArea(new Dimension(0, 10)));
		JLabel welcomeLabel = new JLabel("<html><b><font size='+1'>JadedPacks is setting up your minecraft environment</font></b></html>");
		box.add(welcomeLabel);
		welcomeLabel.setAlignmentY(0);
		welcomeLabel = new JLabel("<html>Please wait, JadedPacks has some tasks to do before you can play</html>");
		welcomeLabel.setAlignmentY(0);
		box.add(welcomeLabel);
		box.add(Box.createRigidArea(new Dimension(0, 10)));
		currentActivity = new JLabel("Currently doing ...");
		box.add(currentActivity);
		box.add(Box.createRigidArea(new Dimension(0, 10)));
		progress = new JProgressBar(0, 100);
		progress.setStringPainted(true);
		box.add(progress);
		box.add(Box.createRigidArea(new Dimension(0, 30)));
		return box;
	}

	private void requestClose(final String message) {
		final int shouldClose = JOptionPane.showConfirmDialog(container, message, "Are you sure you want to stop?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(shouldClose == JOptionPane.YES_OPTION) {
			container.dispose();
		}
		stop = true;
		if(thread != null) {
			thread.interrupt();
		}
	}
}