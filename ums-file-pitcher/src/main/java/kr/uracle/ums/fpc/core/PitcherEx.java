package kr.uracle.ums.fpc.core;


import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.bean.config.AlarmConfigBean;
import kr.uracle.ums.fpc.bean.config.ModuleConfigBean;
import kr.uracle.ums.fpc.bean.config.PitcherConfigBean;
import kr.uracle.ums.fpc.bean.config.RootConfigBean;
import kr.uracle.ums.sdk.util.UmsAlarmSender;

public class PitcherEx extends Thread{

	public enum PitcherStatus{
		READY, DETECTION, FILTERING, PRE_HANDLE, MAIN_HANDLE, POST_HANDLE, DONE
	}
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	protected PitcherStatus status = PitcherStatus.READY;
	
	protected boolean isRun = true;
	protected boolean isMaster = false;
	
	private final PitcherConfigBean PITCHER_CONFIG;
	private final AlarmConfigBean ALARM_CONFIG;
	
	private Path TARGET_PATH;
	private int MAX_THREAD = 5;
	private long CYCLE_TIME = 30*1000;
	private final int MAX_ERROR_COUNT =10;
	
	private long startTime = 0;
	private long leadTime =0;
	
	protected List<Handler> handlerList;
			
	private Detect detect = null;
	private Filter filter = null;
	private PreHandle preHandler = null;
	private MainHandle mainHandler = null;
	private PostHandle postHandler = null;
	
	private ModuleConfigBean preConfig = null;
	private ModuleConfigBean mainConfig = null;
	private ModuleConfigBean postConfig = null;
	
	public PitcherEx(String name, RootConfigBean rootConfigBean, PitcherConfigBean config) {
		setName(name);
		this.isMaster = rootConfigBean.getDUPLEX().isIS_MASTER();
		this.PITCHER_CONFIG = config;
		this.ALARM_CONFIG = rootConfigBean.getALARM();
	}

	public boolean initailize(){
		// 파일 처리 핸들러 관리 리스트
		handlerList = new ArrayList<Handler>(MAX_THREAD);
		
		// 목적 경로 설정
		String path = PITCHER_CONFIG.getPATH();
		if(StringUtils.isBlank(path)) {
			logger.error("PATH 설정이 누락됨");
			return false;
		}
		if(Paths.get(path).isAbsolute() == false) {
			logger.error("PATH 설정은 절대 경로만 가능");
			return false;
		}
		TARGET_PATH = Paths.get(path);
		
		// 수행 주기 설정
		if(PITCHER_CONFIG.getCYCLE() != null && PITCHER_CONFIG.getCYCLE() > 0) {
			CYCLE_TIME = PITCHER_CONFIG.getCYCLE();
		}
		
		// DETECT 생성
		ModuleConfigBean detectConfig = PITCHER_CONFIG.getDETECTION();
		if(ObjectUtils.isEmpty(detectConfig) || StringUtils.isBlank(detectConfig.getCLASS_NAME())) {
			logger.error("DETECTION_CLASS 설정이 누락됨");
			return false;
		}
		
		detect = generateDetect(detectConfig, ALARM_CONFIG, Detect.class);
		if(detect == null) return false;
		if(detect.initailize() == false)return false;
		
		// 필터 생성
		ModuleConfigBean filterConfig = PITCHER_CONFIG.getFILTER();
		if(ObjectUtils.isNotEmpty(filterConfig) && StringUtils.isNotBlank(filterConfig.getCLASS_NAME())) {
			filter = generateDetect(filterConfig, ALARM_CONFIG,  Filter.class);
			if(filter == null) return false;
			if(filter.initailize() == false)return false;
		}
		
		// 전처리기 생성
		preConfig = PITCHER_CONFIG.getPREHANDLE();
		if(ObjectUtils.isNotEmpty(preConfig) && StringUtils.isNotBlank(preConfig.getCLASS_NAME())) {
			preHandler = generateDetect(preConfig, ALARM_CONFIG, PreHandle.class);
			if(preHandler == null) return false;
			if(preHandler.initailize() == false)return false;
		}
		
		// 본처리기 생성
		mainConfig = PITCHER_CONFIG.getMAINHANDLE();
		if(ObjectUtils.isNotEmpty(mainConfig) && StringUtils.isNotBlank(mainConfig.getCLASS_NAME())) {
			mainHandler = generateDetect(mainConfig, ALARM_CONFIG, MainHandle.class);
			if(mainHandler == null) return false;
			if(mainHandler.initailize() == false)return false;
		}
		
		// 후처리기 생성
		postConfig = PITCHER_CONFIG.getPOSTHANDLE();
		if(ObjectUtils.isNotEmpty(postConfig) && StringUtils.isNotBlank(postConfig.getCLASS_NAME())) {
			postHandler = generateDetect(postConfig, ALARM_CONFIG, PostHandle.class);
			if(postHandler == null) return false;
			if(postHandler.initailize() == false)return false;
		}
		
		return true;
	}
	
	public void close() {
		isRun = false;
	}
	
	public void run() {
		int errorCnt = 1;
		while(isRun) {
			status = PitcherStatus.READY;
			try {
				if(errorCnt > MAX_ERROR_COUNT) errorCnt = MAX_ERROR_COUNT;
				sleep(CYCLE_TIME * errorCnt);
			} catch (InterruptedException e) {
				logger.error("대기 중 에러 발생:{}", e);
				e.printStackTrace();
			}
			// 마스터가 아니면 휴식, 매니저가 마스터 파일을 감시하여 이상 시 슬레이브에서 마스터로 전환 함
			if(isMaster == false) {
				logger.info("현재 슬레이브로 {} 동안 휴식", CYCLE_TIME);
				continue;
			}
		
			// 수행 시작 시간 - 매니저에서 Hang 여부 판단을 위해 기록해둠 
			startTime = System.currentTimeMillis();
			status = PitcherStatus.DETECTION;
			
			List<Path> pathList = detect.detect(TARGET_PATH);
			// 탐색 중 에러 발생
			if(pathList == null ) {
				errorCnt +=1;
				sendAlarm(TARGET_PATH +" 탐색 중 에러 발생");
				logger.info("탐색 수행 중 에러 발생으로 {} 동안 휴식", CYCLE_TIME * errorCnt);
				continue;
			}
			
			// 탐색 파일이 없으면 휴식
			if(pathList.size() <= 0) {
				logger.debug("{} 탐색된 신규 파일 없음, {} 동안 휴식", TARGET_PATH, CYCLE_TIME);
				//에러 횟수 초기화
				errorCnt = 1;
				leadTime = System.currentTimeMillis() - startTime;
				status = PitcherStatus.DONE;
				continue;
			}

			// 지정 필터 수행
			if(filter != null) {
				status = PitcherStatus.FILTERING;
				pathList = filter.process(pathList);
				if(pathList == null) {
					errorCnt +=1;
					sendAlarm(TARGET_PATH +" 필터 중 에러 발생");
					logger.info("필터 수행 중 에러 발생으로 {} 동안 휴식", CYCLE_TIME * errorCnt);
					continue;
				}
			}
			int totalCnt = pathList.size(); 
			try {
				while(pathList.size()>0){
					// 현재 Activate 상태의 핸드러만 추출
					Iterator<Handler> iter = handlerList.iterator();
					while(iter.hasNext()) {
						Handler h = iter.next();
						if(h.isAlive() == false) iter.remove();
					}
					
					// 현재 가용 쓰레드 갯수 확인
					int freeHandlerCnt = MAX_THREAD - handlerList.size();
					if(freeHandlerCnt <= 0) {
						logger.info("현재 모든 가용 쓰레드({})가 활성화 중입니다.", MAX_THREAD);
						continue;
					}
					// 가용 가능한 쓰레드 갯수 만큼 파일 처리
					int fileCnt = pathList.size();
					if(fileCnt > freeHandlerCnt) fileCnt =  freeHandlerCnt;
					
					if(fileCnt == 0 ) continue;
					
					// 핸들러 파일 처리
					for(int i =0; i<fileCnt ; i++) {
						Path p = pathList.remove(0);
						Handler h = new Handler(p);
						if(preHandler != null) {
							PreHandle newPre = generateDetect(preConfig, ALARM_CONFIG, PreHandle.class);
							h.setPreHandler(newPre);
						}
						
						if(mainHandler != null) {
							MainHandle newMain = generateDetect(mainConfig, ALARM_CONFIG, MainHandle.class);
							h.setMainHandler(newMain);
						}
						
						if(postHandler != null) {
							PostHandle newPost = generateDetect(postConfig, ALARM_CONFIG, PostHandle.class);
							h.setPostHandler(newPost);
						}
						h.start();
					}
				}
			}catch (Exception e) {
				e.printStackTrace();
				sendAlarm(TARGET_PATH+" 파일 처리 중 에러 발생:"+e);
				errorCnt++;
				continue;
			}

			//에러 횟수 초기화
			errorCnt = 1;
			leadTime = System.currentTimeMillis() - startTime;
			logger.info("파일 처리 완료, 처리 파일:{}개, 처리 시간:{}ms", totalCnt, leadTime);
			status = PitcherStatus.DONE;
		}
	}

	private <T> T generateDetect(ModuleConfigBean modulConfig, AlarmConfigBean alarmConfig, Class<T> clazz) {
		try {
			Class<?> targetClass = Class.forName(modulConfig.getCLASS_NAME());
			Constructor<?> ctor = targetClass.getDeclaredConstructor(ModuleConfigBean.class, AlarmConfigBean.class);
			return clazz.cast(ctor.newInstance(modulConfig, alarmConfig));
		} catch (Exception e) {
			logger.error("{} 생성 중 에러 발생:{}", modulConfig.getCLASS_NAME(), e);
			e.printStackTrace();
			return null;
		}
	}
	
	private void sendAlarm(String msg) {
		msg = ALARM_CONFIG.getPREFIX_MESSAGE()+msg;
		UmsAlarmSender.getInstance().sendAlarm(ALARM_CONFIG.getSEND_CHANNEL() , ALARM_CONFIG.getURL(), msg);
	}
	
	public void changeMaster(boolean isMaster) { this.isMaster = isMaster;	}
	
	public long getStartTime() { return startTime; }
	
	public long getLeadTime() { return leadTime;}
	
	public PitcherStatus getStatus() { return status; }
	
}
