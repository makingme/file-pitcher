package kr.uracle.ums.fpc.woker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import kr.uracle.ums.fpc.core.Roguing;

public class OldFileRoguing extends Roguing{

	private long STANDARD_TIME = 1*60*1000;
	
	public OldFileRoguing(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		super(PRCS_NAME, PARAM_MAP);
		STANDARD_TIME = (Long)PARAM_MAP.get("STANDARD_TIME")!=null?(Long)PARAM_MAP.get("STANDARD_TIME"):STANDARD_TIME;
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
				if((now - lastModiTime) > STANDARD_TIME) {
					errorCnt++;
					iter.remove();
					if(Files.deleteIfExists(p)) log.info("{} 초 잔류 파일({}) 삭제", STANDARD_TIME/1000, p.toString());
				}
			}
			log.info("총 파일{}, 현재 파일:{}, 솎음파일:{}",totalCnt, pathList.size(), errorCnt);
		}catch(Exception e) {
			pathList = null;
			log.error("{} 파일 분류 중 에러 발생:{}", p.toString(), e);
			e.printStackTrace();
		}
		return pathList;
	}

}
