package kr.uracle.ums.fpc.module.preHandle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import kr.uracle.ums.fpc.bean.config.AlarmConfigBean;
import kr.uracle.ums.fpc.core.PreHandle;

/**
 * @author : URACLE KKB
 * @see : 파일 본 처리 전 처리 중 폴터로 파일을 이동 시키는 전처리기
 * @see : 사용자 지정 설정(PARAM_MAP)에 PROCCESS_PATH : 절대경로 설정 정보 지정 시 처리 파일을 지정 경로로 이동 시킴(기본값: PRCS_NAME_PROCESS)
 */
public class FileMovePreHandle extends PreHandle{
	
	private DateTimeFormatter DATE_TIME_FORMAT = null;
	
	private String PROCCESS_PATH; 
	
	public FileMovePreHandle(String PRCS_NAME, Map<String, Object> PARAM_MAP, AlarmConfigBean ALARM_CONFIG) {
		super(PRCS_NAME, PARAM_MAP, ALARM_CONFIG);
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
		
		//NAMING PATTERN
		Object nObj = PARAM_MAP.get("DATE_PATTEN");
		if(ObjectUtils.isNotEmpty(nObj)) {
			try {
				DATE_TIME_FORMAT = DateTimeFormatter.ofPattern(nObj.toString());				
			}catch(IllegalArgumentException e) {
				logger.error("{} - 잘못된 데이터 포맷 형식", nObj.toString());
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	@Override
	public Path process(Path path) {
		String prcsPath = StringUtils.isNotBlank(PROCCESS_PATH)?PROCCESS_PATH:path.getParent().getParent()+File.separator+PRCS_NAME+"_PROCESS";
		if(prcsPath.endsWith(File.separator) == false) prcsPath += File.separator;
		String fileName = path.getFileName().toString();
		if(DATE_TIME_FORMAT != null) fileName = getNewFileName(fileName);
		Path targetPath = Paths.get(prcsPath+fileName);
		
		try {
			Path derectory = Paths.get(prcsPath);
			if(Files.exists(derectory) == false) {
				Files.createDirectories(derectory);
				logger.info("{} 디렉토리 생성", prcsPath);
			}
			Files.move(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			logger.error("{} 파일 전처리-프로세스 디렉토리로- 이동 중에 에러 발생:{}", path.toString(), e);
			e.printStackTrace();
			return null;
		}
		return targetPath;
	}

	
	private String getNewFileName(String FILE_NAME) {
		
		return FILE_NAME.substring(0, FILE_NAME.lastIndexOf("."))+"_"+DATE_TIME_FORMAT.format(LocalDateTime.now())+FILE_NAME.substring(FILE_NAME.lastIndexOf("."));
	}
}
