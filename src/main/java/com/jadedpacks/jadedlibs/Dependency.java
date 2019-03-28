package com.jadedpacks.jadedlibs;

import com.google.gson.JsonObject;

class Dependency {
	final String file, repo;

	Dependency(final JsonObject node) {
		this.file = node.get("file").getAsString();
		String repo = node.get("repo").getAsString();
		if(!repo.endsWith("/")) {
			repo += "/";
		}
		this.repo = repo;
	}
}