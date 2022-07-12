package kr.uracle.ums.fpc.module.postHandle;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import kr.uracle.ums.fpc.bean.config.AlarmConfigBean;
import kr.uracle.ums.fpc.core.PostHandle;

/**
 * @author : URACLE KKB
 * @see : 파일 본 처리 결과에 따른 파일 이동(성공/실패 경로)
 * @see : 사용자 지정 설정(PARAM_MAP)에 SUCCESS_PATH : 절대경로 설정, 본 처리 성공 시 이동 경로(기본값: PRCS_NAME_SUCCESS)
 * @see : 사용자 지정 설정(PARAM_MAP)에 FAIL_PATH : 절대경로 설정, 본 처리 실패 시 이동 경로(기본값: PRCS_NAME_FAIL)
 */
public class ResultMovePostHandle extends PostHandle{

	private String SUCCESS_PATH;
	private String FAIL_PATH;

	public ResultMovePostHandle(String PRCS_NAME, Map<String, Object> PARAM_MAP, AlarmConfigBean ALARM_CONFIG) {
		super(PRCS_NAME, PARAM_MAP, ALARM_CONFIG);
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
	public int process(Path path, int mainResultCode) {

		String destination = StringUtils.isBlank(SUCCESS_PATH)?path.getParent().getParent()+File.separator+PRCS_NAME+"_SUCCESS":SUCCESS_PATH;
		if(mainResultCode < 0) {
			destination = StringUtils.isBlank(FAIL_PATH)?path.getParent().getParent()+File.separator+PRCS_NAME+"_FAIL":FAIL_PATH;
		}
		if(destination.endsWith(File.separator) == false) destination += File.separator;
		
		try {
			Path derectory = Paths.get(destination);
			if(Files.exists(derectory) == false) {
				Files.createDirectories(derectory);
				logger.info("{} 디렉토리 생성", destination);
			}
			Path movePath = Paths.get(destination+path.getFileName());
			Files.move(path, movePath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logger.error("{} 파일 결과:{} 에 따른, 후처리(파일 이동) 중에 에러 발생:{}", path.toString(), mainResultCode,  e);
			return -1;
		}
		
		return 1;
	}
	
}
