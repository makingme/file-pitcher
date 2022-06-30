package kr.uracle.ums.fpc.woker;

import java.nio.file.Path;
import java.util.Map;

import kr.uracle.ums.fpc.core.PreHandle;

public class MoveFile extends PreHandle{

	public MoveFile(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		super(PRCS_NAME, PARAM_MAP);
	}

	@Override
	public boolean process(Path path) {
		boolean isOk = false;
		
		return isOk;
	}

}
