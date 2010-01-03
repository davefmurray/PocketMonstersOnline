package org.pokenet.thin;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

import org.pokenet.thin.libs.CheckSums;
import org.pokenet.thin.libs.JGet;

/**
 * ThinClient
 * @author Sienide
 *
 */
public class ThinClient extends JFrame implements Runnable {
	private ImageIcon m_logo;
	private JProgressBar m_progress;
	/* The root directory of update location 
	 * NOTE: Must start with http:// and end with a /
	 */
	public static String UPDATEURL = "http://pokedev.org/";
	public static String LOGOURL = "http://trainerdex.org/bg.png";

	/**
	 * Constructor
	 */
	public ThinClient() {
		super("PokeNet Updater");
		this.setSize(396, 160);
		this.setResizable(false);
		/* Center the updater */
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (int) ((d.getWidth() / 2) - this.getWidth() / 2);
		int y = (int) ((d.getHeight() / 2) - this.getHeight() / 2);
		this.setLocation(x, y);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		/* Add PokeNet Image */
		try {
			m_logo = new ImageIcon(new URL(LOGOURL));
			this.add(new JLabel(m_logo), BorderLayout.CENTER);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		/* Create progress bar */
		m_progress = new JProgressBar();
		m_progress.setValue(0);
		/* Create bottom panel */
		JPanel l = new JPanel();
		l.add(new JLabel("Updating: "));
		l.add(m_progress);
		this.add(l, BorderLayout.SOUTH);
		this.setVisible(true);
		/* Start downloading updates */
		new Thread(this).start();
	}

	public static void main(String [] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		ThinClient c = new ThinClient();
	}

	@Override
	public void run() {
		/* Hashmap of <files, checksums> */
		HashMap<String, String> files = new HashMap<String, String>();
		/* Download updates if possible */
		try {
			URL u = new URL(UPDATEURL + "updates.txt");
			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							u.openStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				String checksum = inputLine.substring(0, inputLine.indexOf(' '));
				String file = inputLine.substring(inputLine.indexOf(' ') + 1);
				files.put(file, checksum);
			}
			in.close();
		} catch (Exception e) {
			/* Update server not available, run game */
			try {
				this.setVisible(false);
				runPokenet();
				System.exit(0);
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(null, "An error occured while running the game.");
				System.exit(0);
			}
			return;
		}
		int total = files.keySet().size();
		int value = 0;
		/* We got the list of checksums, let's see if we need to update */
		Iterator<String> it = files.keySet().iterator();
		CheckSums s;
		while(it.hasNext()) {
			String file = it.next();
			/* First check if we have the file */
			File f = new File(file);
			if(f.exists()) {
				/* It exists, does it need updating? */
				s = new CheckSums();
				String current = "";
				String online = files.get(file);
				try {
					current = s.getSHA1Checksum(f.getPath());
					if(current.compareTo(online) != 0) {
						/* We need to update */
						f.delete();
						JGet.getFile(UPDATEURL + file, f.getPath());
					}
				} catch (Exception e) {
					/* Error! Redownload file */
					try {
						JGet.getFile(UPDATEURL + file, f.getPath());
					} catch (Exception ex) {
						ex.printStackTrace();
						JOptionPane.showMessageDialog(null, "Could not download update.");
						break;
					}
				}
			} else {
				/* We don't have it, download it */
				try {
					JGet.getFile(UPDATEURL + file, f.getPath());
				} catch (Exception e) {
					JOptionPane.showMessageDialog(null, "Could not download update.");
					break;
				}
			}
			value++;
			m_progress.setValue((value / total) * 100);
		}
		/* Launch the game */
		try {
			this.setVisible(false);
			runPokenet();
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "An error occured while running the game.");
			System.exit(0);
		}
	}

	public void runPokenet() throws Exception {
		Process p = Runtime.getRuntime().exec("java -Dres.path=./ -Djava.library.path=lib/native " +
		"-Xmx512m -Xms512m -jar client.jar");
		BufferedReader stdInput = new BufferedReader(new 
				InputStreamReader(p.getInputStream()));
		BufferedReader stdError = new BufferedReader(new 
				InputStreamReader(p.getErrorStream()));
		String line;
		PrintWriter pw = new PrintWriter(new File("./errors.txt"));
		while(true) {
			while ((line = stdInput.readLine()) != null) {
				System.out.println(line);
			}
			while ((line = stdError.readLine()) != null) {
				pw.println(line);
				pw.flush();
			}
			try {
				Thread.sleep(1000);
			} catch (Exception e) {}
			try {
				if(p.exitValue() >= 0)
					break;
			} catch (Exception e) {}
		}
		pw.close();
	}
}