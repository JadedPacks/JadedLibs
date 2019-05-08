package com.jadedpacks.jadedlibs;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import cpw.mods.fml.relauncher.CoreModManager;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.FMLLaunchHandler;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import net.minecraft.launchwrapper.LaunchClassLoader;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

class JadedLibsInst {
	private final boolean isClient;
	private final File mcDir, modsDir;
	private LaunchClassLoader loader = (LaunchClassLoader) JadedLibsInst.class.getClassLoader();
	private ArrayList<Dependency> depMap;
	private IDownloader downloadMonitor;

	JadedLibsInst() {
		isClient = FMLLaunchHandler.side().isClient();
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
		activateDeps();
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
		downloadMonitor = isClient ? new Downloader() : new DummyDownloader();
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
		if(dependency.clientOnly && isClient) {
			System.out.println("File is a clientOnly mod; Skipping: " + dependency.file);
			return;
		}
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

	private void activateDeps() {
		for(final Dependency dep : depMap) {
			File coreMod = new File(modsDir, dep.file);
			JarFile jar = null;
			Attributes mfAttributes;
			try {
				jar = new JarFile(coreMod);
				if(jar.getManifest() == null) {
					return;
				}
				mfAttributes = jar.getManifest().getMainAttributes();
			} catch(IOException e) {
				System.err.println("Unable to read the jar file " + dep.file + " - ignoring");
				e.printStackTrace();
				return;
			} finally {
				try {
					if(jar != null) {
						jar.close();
					}
				} catch(IOException ignored) {}
			}
			String fmlCorePlugin = mfAttributes.getValue("FMLCorePlugin");
			if(fmlCorePlugin == null) {
				return;
			}
			try {
				loader.addURL(coreMod.toURI().toURL());
			} catch(MalformedURLException e) {
				throw new RuntimeException(e);
			}
			try {
				Class<CoreModManager> c = CoreModManager.class;
				if(!mfAttributes.containsKey(new Attributes.Name("FMLCorePluginContainsFMLMod"))) {
					FMLRelaunchLog.finest("Adding %s to the list of known coremods, it will not be examined again", coreMod.getName());
					Field f_loadedCoremods = c.getDeclaredField("loadedCoremods");
					f_loadedCoremods.setAccessible(true);
					((List) f_loadedCoremods.get(null)).add(coreMod.getName());
				} else {
					FMLRelaunchLog.finest("Found FMLCorePluginContainsFMLMod marker in %s, it will be examined later for regular @Mod instances", coreMod.getName());
					Field f_reparsedCoremods = c.getDeclaredField("reparsedCoremods");
					f_reparsedCoremods.setAccessible(true);
					((List) f_reparsedCoremods.get(null)).add(coreMod.getName());
				}
				Method m_loadCoreMod = c.getDeclaredMethod("loadCoreMod", LaunchClassLoader.class, String.class, File.class);
				m_loadCoreMod.setAccessible(true);
				m_loadCoreMod.invoke(null, loader, fmlCorePlugin, coreMod);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}