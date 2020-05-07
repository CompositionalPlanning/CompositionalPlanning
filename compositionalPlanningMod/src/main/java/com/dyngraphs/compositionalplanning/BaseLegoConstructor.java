package com.dyngraphs.compositionalplanning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

abstract class BaseLegoConstructor {
	protected ConstructionHelper constructionHelper = new ConstructionHelper();
	protected HashMap<Integer, Lego> constructionElements = new HashMap<Integer, Lego>();
	
	boolean hasBuiltLego(Lego lego) {
		return constructionElements.containsKey(lego.getId());
	}
	
	List<Lego> getAllLegos() {
		return new ArrayList(constructionElements.values());
	}
	
	HashMap<Integer, Lego> getAllLegosWithId() {
		return new HashMap(this.constructionElements);
	}
	
}
