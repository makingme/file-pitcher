package kr.uracle.ums.fpc.woker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import kr.uracle.ums.fpc.core.Detect;

public class BasicDetector extends Detect{

	private Map<File, Long> fileHistory = new HashMap<File, Long>();
	private long STANDARD_TIME = 15*1000;
	
	public BasicDetector(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		super(PRCS_NAME, PARAM_MAP);
		STANDARD_TIME = (Long)PARAM_MAP.get("STANDARD_TIME")!=null?(Long)PARAM_MAP.get("STANDARD_TIME"):STANDARD_TIME;
	}

	@Override
	public List<Path> process(Path path, String pattern) {
		List<Path> targetFileList = new ArrayList<Path>();
		String fnamePatter = StringUtils.isBlank(pattern)?".+":pattern;
		try {
			
			List<Path> list = Files.walk(path).filter(p -> p.toFile().isFile() && Pattern.matches(fnamePatter, p.getFileName().toString())).collect(Collectors.toList());
			
			// 지정 경로의 파일이 없으면 빈 인스턴스 리턴
			if(ObjectUtils.isEmpty(list)) {
				log.info("{} 경로에 파일 없음", path.toString());
				// 파일 히스토리에 정보가 있다면 별도의 방법으로 파일이 지워짐으로 간주 - 로그 출력
				if(fileHistory.size() > 0) {
					for(Entry<File, Long> element : fileHistory.entrySet()) {
						File f = element.getKey();
						log.warn("{} 파일이 사라짐", f.getName());
					}
				}
				return targetFileList;
			}
			

			
			long now = System.currentTimeMillis();
			int totalCnt = list.size();
			int writeCnt = 0;
			int newCnt = 0;
			int remainCnt =0;
			int prcsCnt = 0;
			for(Path p : list) {
				// WRITE LOCK 파일은 미완성 파일로 지정
				if(Files.isWritable(p) == false) {
					writeCnt++;
					continue;
				}
				
				Long regTime = fileHistory.get(p.toFile());
				// 신규 유입 파일은 히스토리에 등록 후 스킵
				if(regTime == null || regTime == 0){
					fileHistory.put(p.toFile(), now);
					newCnt++;
					continue;
				}
				
				// 기준 시간 미달 파일은 스킵
				if(STANDARD_TIME>0 && (now - regTime) < STANDARD_TIME) {
					remainCnt++;
					continue;
				}
				// 기준 시간 만족 파일 히스토리에서 삭제
				fileHistory.remove(p.toFile());
				
				// 모든 기준 조건 만족 파일 타겟(처리파일) 목록에 추가
				targetFileList.add(p);
				prcsCnt++;
			}
			log.info("총 파일:{}, 쓰는중:{}, 신규파일:{}, 기존파일:{}, 처리파일:{}", totalCnt, writeCnt, newCnt, remainCnt, prcsCnt);
		 }catch (Exception e) {
			targetFileList = null;
			log.error("{} 파일 탐색 중 에러 발생:{}", path.toString(), e);
			e.printStackTrace();
		}
		return targetFileList;
	}
	
	public static void main(String[] args) {
		Detect d = new BasicDetector("", new HashMap<String, Object>());
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				while(true){
					Path path = Paths.get("D:\\TEST\\PITCHER\\RECEIVE\\");
					List<Path> targetFileList = d.process(path, "");
					for(Path p: targetFileList) {
						System.out.println("처리 파일:"+p.toString());
					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		Thread t = new Thread(r);
		t.setDaemon(false);
		t.start();
	}

}
