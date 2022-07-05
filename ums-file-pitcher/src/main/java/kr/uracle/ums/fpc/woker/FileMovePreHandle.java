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

import kr.uracle.ums.fpc.core.PreHandle;

/**
 * @author : URACLE KKB
 * @see : 파일 본 처리 전 처리 중 폴터로 파일을 이동 시키는 전처리기
 * @see : 사용자 지정 설정(PARAM_MAP)에 PROCCESS_PATH : 절대경로 설정 정보 지정 시 처리 파일을 지정 경로로 이동 시킴(기본값: PRCS_NAME_PROCESS)
 */
public class FileMovePreHandle extends PreHandle{

	private String PROCCESS_PATH; 
	
	public FileMovePreHandle(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		super(PRCS_NAME, PARAM_MAP);
	}
	
	@Override
	public boolean initailize() {
		// 이동 경로 취득
		Object object = PARAM_MAP.get("PROCCESS_PATH");
		String prcsPath = ObjectUtils.isEmpty(object)?"":object.toString();
		if(Paths.get(prcsPath).isAbsolute() == false) {			
			prcsPath = "";
		}
		PROCCESS_PATH = prcsPath;
		return true;
	}

	@Override
	public boolean process(Path path) {
		String prcsPath = StringUtils.isNotBlank(PROCCESS_PATH)?PROCCESS_PATH:path.getParent().getParent()+File.separator+PRCS_NAME+"_PROCESS";
		if(prcsPath.endsWith(File.separator) == false ) prcsPath+=File.separator;
		
		try {
			Path movePath = Paths.get(prcsPath+path.getFileName().toString());
			Files.move(path, movePath, StandardCopyOption.REPLACE_EXISTING);
			path = movePath;
		} catch (IOException e) {
			logger.error("{} 파일 선처리(이동) 중에 에러 발생:{}", path.toString(), e);
			return false;
		}
		return true;
	}

}
