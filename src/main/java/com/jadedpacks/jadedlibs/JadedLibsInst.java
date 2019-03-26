package com.jadedpacks.jadedlibs;

import argo.jdom.JdomParser;
import argo.jdom.JsonNode;
import argo.jdom.JsonRootNode;
import argo.saj.InvalidSyntaxException;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import net.minecraft.launchwrapper.LaunchClassLoader;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Logger;

class JadedLibsInst {
	private final Logger logger = Logger.getLogger("JadedLibs");
	private final File mcDir, modsDir;
	private ArrayList<String> depMap;
	private String repo;
	private IDownloader downloadMonitor;

	JadedLibsInst() {
		depMap = new ArrayList<String>();
		mcDir = (File) FMLInjectionData.data()[6];
		modsDir = new File(mcDir, "mods");
	}

	void load() {
		final File file = new File(mcDir, "config/jadedlibs.json");
		if(!file.exists()) {
			return;
		}
		try {
			loadJSON(file);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if(depMap.isEmpty()) {
			return;
		}
		loadDeps();
		activateDeps();
	}

	private void loadJSON(final File file) throws IOException, InvalidSyntaxException {
		final InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
		final JsonRootNode root = new JdomParser().parse(reader);
		repo = root.getStringValue("repo");
		if(root.hasElements()) {
			for(final JsonNode node : root.getElements()) {
				depMap.add(node.getStringValue("file"));
			}
		} else {
			depMap.add(root.getStringValue("file"));
		}
		reader.close();
	}

	private void loadDeps() {
		downloadMonitor = FMLLaunchHandler.side().isClient() ? new Downloader() : new DummyDownloader();
		JDialog popupWindow = (JDialog) downloadMonitor.makeDialog();
		try {
			for(String dependency : depMap) {
				download(dependency);
			}
		} finally {
			if(popupWindow != null) {
				popupWindow.setVisible(false);
				popupWindow.dispose();
			}
		}
	}

	private void download(final String dependency) {
		final File target = new File(modsDir, dependency);
		if(target.exists()) {
			return;
		}
		try {
			final URL url = new URL(repo + dependency);
			downloadMonitor.updateProgressString("Downloading file " + dependency);
			logger.info("Downloading file " + dependency);
			final URLConnection connection = url.openConnection();
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty("User-Agent", "JadedPacks Downloader");
			download(connection.getInputStream(), connection.getContentLength(), target);
			downloadMonitor.updateProgressString("Download complete");
			logger.info("Download complete");
		} catch(final Exception e) {
			if(downloadMonitor.shouldStop()) {
				logger.warning("You have stopped the download before it could be completed");
				System.exit(1);
			}
			downloadMonitor.showErrorDialog(dependency, repo + dependency);
			throw new RuntimeException("Download error", e);
		}
	}

	private void download(final InputStream input, final int sizeGuess, final File target) throws Exception {
		downloadMonitor.resetProgress(sizeGuess);
		int read, length = 0;
		try {
			downloadMonitor.setThread(Thread.currentThread());
			byte[] buffer = new byte[1024];
			while((read = input.read(buffer)) >= 0) {
				// Download bits?
				length += read;
				if(downloadMonitor.shouldStop()) {
					break;
				}
				downloadMonitor.updateProgress(length);
			}
			input.close();
			downloadMonitor.setThread(null);
		} catch(final InterruptedIOException e) {
			Thread.interrupted();
			throw new Exception("Stop");
		}
		// write file
	}

	private void activateDeps() {
		for(final String dep : depMap) {
			final File mod = new File(modsDir, dep);
			JarFile jar = null;
			try {
				jar = new JarFile(mod);
				if(jar.getManifest() != null && jar.getManifest().getMainAttributes().getValue("FMLCorePlugin") != null) {
					((LaunchClassLoader) JadedLibsInst.class.getClassLoader()).addURL(mod.toURI().toURL());
				}
			} catch(final IOException e) {
				logger.warning("Unable to read the jar file " + dep + " - ignoring");
				e.printStackTrace();
			} finally {
				try {
					if(jar != null) {
						jar.close();
					}
				} catch(final IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}