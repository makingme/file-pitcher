package kr.uracle.ums.fpc.module.filter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import kr.uracle.ums.fpc.bean.config.AlarmConfigBean;
import kr.uracle.ums.fpc.core.Filter;

/**
 * @author : URACLE KKB
 * @see : 파일 경로 목록 중 사이즈가 0 인 파일을 필터하여 ERROR_PATH 로 이동 시킴
 * @see : 사용자 지정 설정(PARAM_MAP)에 ERROR_PATH : 절대경로 설정 정보 지정 시 필터링 파일 지정 경로로 이동 시킴(기본값:PRCS_NAME_ERROR)
 */
public class ZeroFileFilter extends Filter{
	
	private String ERROR_PATH; 
	
	public ZeroFileFilter(String PRCS_NAME, Map<String, Object> PARAM_MAP, AlarmConfigBean ALARM_CONFIG) {
		super(PRCS_NAME, PARAM_MAP, ALARM_CONFIG);
	}
	
	@Override
	public boolean initailize() {
		// 불량 파일 옮길 경로 취득
		Object object = PARAM_MAP.get("ERROR_PATH");
		String errorPath = ObjectUtils.isEmpty(object)?"":object.toString();
		if(Paths.get(errorPath).isAbsolute() == false) {			
			errorPath = "";
		}
		ERROR_PATH = errorPath;
		return true;
	}

	@Override
	public List<Path> process(List<Path> pathList) {
		Iterator<Path> iter = pathList.iterator();
		List<Path> errorPathList = new ArrayList<Path>();
		Path p = null;
		try {

			int totalCnt = pathList.size();
			while(iter.hasNext()) {
				p = iter.next();
				
				if(Files.size(p) <= 0) {
					errorPathList.add(p);
					String errorPath = StringUtils.isNotBlank(ERROR_PATH)?ERROR_PATH:p.getParent().getParent()+File.separator+PRCS_NAME+"_ERROR";
					if(errorPath.endsWith(File.separator) == false ) errorPath+=File.separator;
					Path derectory = Paths.get(errorPath);
					if(Files.exists(derectory) == false) {
						Files.createDirectories(derectory);
						logger.info("{} 디렉토리 생성", errorPath);
					}
					
					iter.remove();
					Files.move(p, Paths.get(errorPath+p.getFileName()), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			
			if(errorPathList.size()>0)logger.info("총 파일{}, 제로 사이즈 필터(삭제)파일:{}, 처리 가능 파일:{}, ",totalCnt, errorPathList.size(), pathList.size());
		}catch(Exception e) {
			pathList = null;
			logger.error("{} 파일 분류 중 에러 발생:{}", p.toString(), e);
			e.printStackTrace();
		}
		return pathList;
	}	

}
