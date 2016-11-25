package com.comm.bean;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jfree.data.time.Second;

public class FixedLengthMap extends LinkedHashMap<Second, Integer> {
	/**
     * 
     */
	private static final long serialVersionUID = 1L;
	private static final int MAX_ENTRIES = 9;

	protected boolean removeEldestEntry(Map.Entry eldest) {
		return size() > MAX_ENTRIES;
	}
}