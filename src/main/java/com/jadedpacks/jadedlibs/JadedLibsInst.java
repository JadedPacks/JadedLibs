package com.jadedpacks.jadedlibs;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLLaunchHandler;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

class JadedLibsInst {
	private final File mcDir, modsDir;
	private ArrayList<Dependency> depMap;
	private IDownloader downloadMonitor;

	JadedLibsInst() {
		mcDir = (File) FMLInjectionData.data()[6];
		modsDir = new File(mcDir, "mods");
	}

	void load() {
		final File file = new File(mcDir, "config/jadedlibs.json");
		if(!file.exists()) {
			return;
		}
		depMap = new ArrayList<Dependency>();
		try {
			loadJSON(file);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if(depMap.isEmpty()) {
			return;
		}
		loadDeps();
	}

	private void loadJSON(final File file) throws IOException {
		final InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
		final JsonElement root = new JsonParser().parse(reader);
		if(root.isJsonArray()) {
			for(final JsonElement node : root.getAsJsonArray()) {
				depMap.add(new Dependency(node.getAsJsonObject()));
			}
		} else {
			depMap.add(new Dependency(root.getAsJsonObject()));
		}
		reader.close();
	}

	private void loadDeps() {
		downloadMonitor = FMLLaunchHandler.side().isClient() ? new Downloader() : new DummyDownloader();
		JDialog popupWindow = (JDialog) downloadMonitor.makeDialog();
		try {
			for(final Dependency dependency : depMap) {
				download(dependency);
			}
		} finally {
			if(popupWindow != null) {
				popupWindow.setVisible(false);
				popupWindow.dispose();
			}
		}
	}

	private void download(final Dependency dependency) {
		final File target = new File(modsDir, dependency.file);
		if(target.exists()) {
			System.out.println("File already exists; Skipping: " + dependency.file);
			return;
		}
		try {
			final URL url = new URL(dependency.repo + dependency.file.replace(" ", "%20"));
			downloadMonitor.updateProgressString("Downloading file '" + dependency.file + "'");
			System.out.println("Downloading file '" + dependency.file + "' from url '" + url.toString());
			final URLConnection connection = url.openConnection();
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty("User-Agent", "JadedPacks Downloader");
			download(connection.getInputStream(), connection.getContentLength(), target);
			downloadMonitor.updateProgressString("Download complete");
			System.out.println("Download complete");
		} catch(final Exception e) {
			if(downloadMonitor.shouldStop()) {
				System.err.println("You have stopped the download before it could be completed");
				System.exit(1);
			}
			downloadMonitor.showErrorDialog(dependency.file, dependency.repo + dependency.file);
			throw new RuntimeException("Download error", e);
		}
	}

	private void download(final InputStream input, final int sizeGuess, final File target) throws Exception {
		downloadMonitor.resetProgress(sizeGuess);
		int read, length = 0;
		try {
			downloadMonitor.setThread(Thread.currentThread());
			FileOutputStream output = new FileOutputStream(target);
			byte[] buffer = new byte[1024];
			while((read = input.read(buffer, 0, 1024)) != -1) {
				if(downloadMonitor.shouldStop()) {
					break;
				}
				output.write(buffer, 0, read);
				length += read;
				downloadMonitor.updateProgress(length);
			}
			input.close();
			downloadMonitor.setThread(null);
		} catch(final InterruptedIOException e) {
			Thread.interrupted();
			target.delete();
			throw new Exception("Stop");
		}
	}
}