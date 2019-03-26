package com.jadedpacks.jadedlibs;

class DummyDownloader implements IDownloader {

	@Override
	public void resetProgress(final int sizeGuess) {}

	@Override
	public void setThread(final Thread thread) {}

	@Override
	public void updateProgress(final int sizeGuess) {}

	@Override
	public boolean shouldStop() {
		return false;
	}

	@Override
	public void updateProgressString(final String progress) {}

	@Override
	public Object makeDialog() {
		return null;
	}

	@Override
	public void showErrorDialog(final String name, final String url) {}
}