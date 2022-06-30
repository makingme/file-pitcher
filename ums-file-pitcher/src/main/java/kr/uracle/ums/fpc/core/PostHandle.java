package kr.uracle.ums.fpc.core;

import java.nio.file.Path;
import java.util.Map;

public abstract class PostHandle {
	
	private final Map<String, Object> PARAM_MAP;
	
	public PostHandle(Map<String, Object> PARAM_MAP) {
		this.PARAM_MAP = PARAM_MAP;
	}
	
	abstract public boolean postHandle(Path path, boolean isMainSuccess);
}
