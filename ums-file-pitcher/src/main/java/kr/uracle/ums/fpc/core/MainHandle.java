package kr.uracle.ums.fpc.core;

import java.nio.file.Path;
import java.util.Map;

public abstract class MainHandle {
	
	private final Map<String, Object> PARAM_MAP;
	
	public MainHandle(Map<String, Object> PARAM_MAP) {
		this.PARAM_MAP = PARAM_MAP;
	}
	abstract public boolean handle(Path path, boolean isPreSuccess);
}
