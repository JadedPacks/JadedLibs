package com.jadedpacks.jadedlibs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class Dependency {
	final String file, repo;
	boolean clientOnly;

	Dependency(final JsonObject node) {
		file = node.get("file").getAsString();
		JsonElement clientOnlyElem = node.get("clientOnly");
		clientOnly = clientOnlyElem != null && clientOnlyElem.getAsBoolean();
		String repoElem = node.get("repo").getAsString();
		if(!repoElem.endsWith("/")) {
			repoElem += "/";
		}
		repo = repoElem;
	}
}