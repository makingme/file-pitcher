package kr.uracle.ums.fpc.woker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import kr.uracle.ums.fpc.core.Filter;

/**
 * @author : URACLE KKB
 * @see : 파일 경로 목록 중 EXPIRY_TIME 기준 시간 초과로 지정 경로에 머문 파일을 필터하여 삭제함
 * @see : 사용자 지정 설정(PARAM_MAP)에 EXPIRY_TIME : 1000(mils) 설정 정보 지정 시 파일 탐색 시간 시간 변동 가능(기본: 10*60*1000)
 */
public class OldFileFilter extends Filter{

	private long EXPIRY_TIME = 10*60*1000;
	
	public OldFileFilter(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		super(PRCS_NAME, PARAM_MAP);
		
		EXPIRY_TIME = (Long)PARAM_MAP.get("EXPIRY_TIME")!=null?(Long)PARAM_MAP.get("EXPIRY_TIME"):EXPIRY_TIME;
	}

	@Override
	public List<Path> process(List<Path> pathList) {
		Iterator<Path> iter = pathList.iterator();
		long now = System.currentTimeMillis();
		Path p = null;
		try {
			int totalCnt = pathList.size();
			int errorCnt = 0;
			while(iter.hasNext()) {
				p = iter.next();
				long lastModiTime = p.toFile().lastModified();
				if((now - lastModiTime) > EXPIRY_TIME) {
					errorCnt++;
					iter.remove();
					if(Files.deleteIfExists(p)) logger.info("{} 분 잔류 파일({}) 삭제", EXPIRY_TIME/60000, p.toString());
				}
			}
			logger.info("총 파일{}, 현재 파일 갯수:{}, 필터 파일 갯수:{}",totalCnt, pathList.size(), errorCnt);
		}catch(Exception e) {
			pathList = null;
			logger.error("{} 파일 분류 중 에러 발생:{}", p.toString(), e);
			e.printStackTrace();
		}
		return pathList;
	}

}
