package kr.uracle.ums.fpc.woker;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import kr.uracle.ums.fpc.core.PostHandle;

/**
 * @author : URACLE KKB
 * @see : 파일 본 처리 결과에 따른 파일 이동(성공/실패 경로)
 * @see : 사용자 지정 설정(PARAM_MAP)에 SUCCESS_PATH : 절대경로 설정, 본 처리 성공 시 이동 경로(기본값: PRCS_NAME_SUCCESS)
 *  * @see : 사용자 지정 설정(PARAM_MAP)에 FAIL_PATH : 절대경로 설정, 본 처리 실패 시 이동 경로(기본값: PRCS_NAME_FAIL)
 */
public class ResultMovePostHandle extends PostHandle{

	private String SUCCESS_PATH;
	private String FAIL_PATH;

	public ResultMovePostHandle(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		super(PRCS_NAME, PARAM_MAP);
	}
	

	@Override
	public boolean initailize() {
		// 이동 경로 취득
		Object success_object = PARAM_MAP.get("SUCCESS_PATH");
		String successPath = ObjectUtils.isEmpty(success_object)?"":success_object.toString();
		if(Paths.get(successPath).isAbsolute() == false) {			
			successPath = "";
		}
		SUCCESS_PATH = successPath;
		
		Object fail_object = PARAM_MAP.get("FAIL_PATH");
		String failPath = ObjectUtils.isEmpty(fail_object)?"":fail_object.toString();
		if(Paths.get(failPath).isAbsolute() == false) {			
			failPath = "";
		}
		FAIL_PATH = failPath;
		
		return true;
	}

	@Override
	public boolean process(Path path, boolean isMainSuccess) {

		String destination = StringUtils.isBlank(SUCCESS_PATH)?path.getParent().getParent()+File.separator+PRCS_NAME+"_SUCCESS":SUCCESS_PATH;
		if(isMainSuccess == false) {
			destination = StringUtils.isBlank(FAIL_PATH)?path.getParent().getParent()+File.separator+PRCS_NAME+"_FAIL":FAIL_PATH;
		}
		
		Path movePath = Paths.get(destination+path.getFileName().toString());
		try {
			Files.move(path, movePath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			log.error("{} 파일 결과:{} 에 따른, 후처리(파일 이동) 중에 에러 발생:{}", path.toString(), isMainSuccess,  e);
			return false;
		}
		
		return true;
	}
	
}
