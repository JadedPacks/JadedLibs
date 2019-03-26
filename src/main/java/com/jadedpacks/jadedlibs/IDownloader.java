package com.jadedpacks.jadedlibs;

interface IDownloader {
	void resetProgress(final int sizeGuess);
	void setThread(final Thread thread);
	void updateProgress(final int progress);
	boolean shouldStop();
	void updateProgressString(final String progress);
	Object makeDialog();
	void showErrorDialog(final String name, final String url);
}