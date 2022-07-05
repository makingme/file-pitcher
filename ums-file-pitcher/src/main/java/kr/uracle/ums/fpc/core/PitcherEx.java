package kr.uracle.ums.fpc.core;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.config.bean.AlarmConfigBean;
import kr.uracle.ums.fpc.config.bean.PitcherExConfigBean;
import kr.uracle.ums.fpc.config.bean.RootConfigExBean;


public class PitcherEx extends Thread{

	public enum PitcherStatus{
		READY, DETECTION, ROGUING, PRE_HANDLE, MAIN_HANDLE, POST_HANDLE, DONE
	}
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	protected PitcherStatus status = PitcherStatus.READY;
	
	protected boolean isRun = false;
	protected boolean isMaster = false;
	
	private final AlarmConfigBean alarm;
	private final PitcherExConfigBean config;
	
	private long startTime = 0;
	private long leadTime =0;
			
	private Detect detect = null;
	private Filter roguing = null;
	private PreHandle preHandler = null;
	private MainHandle mainHandler = null;
	private PostHandle postHandler = null;
	
	public PitcherEx(String name, RootConfigExBean rootConfigBean, PitcherExConfigBean config) {
		setName(name);
		this.isMaster = rootConfigBean.getDUPLEX().isIS_MASTER();
		this.alarm = rootConfigBean.getALARM();
		this.config = config;
	}
	
	public boolean init(){
		isRun = true;
		return true;
	}
	
	public void close() {
		isRun = false;
	}
	
	public void run() {
		isRun = init();
		while(isRun) {
			status = PitcherStatus.READY;
			try {
				sleep(1000);
			} catch (InterruptedException e) {
				log.error("{} 대기 중 에러 발생:{}", this.getName(), e);
			}
			
			// 마스터가 아니면 휴식, 매니저가 마스터 파일을 감시하여 이상 시 슬레이브에서 마스터로 전환 함
			if(isMaster == false) {
				log.info("[{}]  현재 슬레이브로 {} 동안 휴식", getName(), 10000);
				continue;
			}
			
			// 수행 시작 시간 - 매니저에서 Hang 여부 판단을 위해 기록해둠 
			startTime = System.currentTimeMillis();
			status = PitcherStatus.DETECTION;
			List<Path> pathList = detect.detect(Paths.get(""));
			if(pathList == null) {
				//에러 알람 발송 호출
			}
			
			if(roguing != null) {
				status = PitcherStatus.ROGUING;
				pathList = roguing.process(pathList);
				if(pathList == null) {
					//에러 알람 발송 호출
				}
			}
			
			for(Path p : pathList) {
				HandlerEx h = new HandlerEx(p);
				h.setPreHandler(preHandler);
				h.setMainHandler(mainHandler);
				h.setPostHandler(postHandler);
				h.start();
				
//				if(preHandler != null) {
//					status = PitcherStatus.PRE_HANDLE;
//					isSuccess = preHandler.process(p);
//				}
//				
//				if(mainHandler != null) {
//					status = PitcherStatus.MAIN_HANDLE;
//					isSuccess = mainHandler.handle(p, isSuccess);
//				}
//				
//				if(postHandler != null) {
//					status = PitcherStatus.POST_HANDLE;
//					isSuccess = postHandler.process(p, isSuccess);
//				}
			}
			
			leadTime = System.currentTimeMillis() - startTime;
			status = PitcherStatus.DONE;
		}
	}

		
	public void changeMaster(boolean isMaster) { this.isMaster = isMaster;	}
	
	public long getStartTime() { return startTime; }
	
	public long getLeadTime() { return leadTime;}
	
	public PitcherStatus getStatus() { return status; }
	
}
