package kr.uracle.ums.fpc.woker;

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

import kr.uracle.ums.fpc.core.Roguing;

public class ZeroFIleRoguing extends Roguing{

	public ZeroFIleRoguing(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		super(PRCS_NAME, PARAM_MAP);
	}

	@Override
	public List<Path> process(List<Path> pathList) {
		Iterator<Path> iter = pathList.iterator();
		List<Path> errorPathList = new ArrayList<Path>();
		Path p = null;
		try {
			
			// 불량 파일 옮길 경로 취득
			Object object = PARAM_MAP.get("ERROR_PATH");
			
			int totalCnt = pathList.size();
			while(iter.hasNext()) {
				p = iter.next();
				
				if(Files.size(p) <= 0) {
					errorPathList.add(p);
					String errorPath = ObjectUtils.isEmpty(object)?p.getParent().toString():object.toString();
					if(errorPath.endsWith(File.separator) == false ) errorPath+=File.separator;
					
					iter.remove();
					Files.move(p, Paths.get(errorPath+p.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			
			if(errorPathList.size() >0) {
				sendAlarm("솎음 파일 발생");
			}
			log.info("총 파일{}, 현재 파일:{}, 솎음파일:{}",totalCnt, pathList.size(), errorPathList.size());
		}catch(Exception e) {
			pathList = null;
			log.error("{} 파일 분류 중 에러 발생:{}", p.toString(), e);
			e.printStackTrace();
		}
		return pathList;
	}
	

}
