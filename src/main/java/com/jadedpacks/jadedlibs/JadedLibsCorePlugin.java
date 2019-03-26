package com.jadedpacks.jadedlibs;

import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

public class JadedLibsCorePlugin implements IFMLLoadingPlugin, IFMLCallHook {
	private JadedLibsInst inst;

	public JadedLibsCorePlugin() {
		load();
	}

	@Override
	public String[] getASMTransformerClass() {
		return null;
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return this.getClass().getName();
	}

	@Override
	public void injectData(final Map<String, Object> map) {}

	@Override
	public Void call() {
		load();
		return null;
	}

	private void load() {
		if(inst == null) {
			inst = new JadedLibsInst();
			inst.load();
		}
	}
}