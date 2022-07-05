package kr.uracle.ums.fpc;



import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.config.ConfigExManager;
import kr.uracle.ums.fpc.config.bean.PitcherExConfigBean;
import kr.uracle.ums.fpc.config.bean.RootConfigExBean;
import kr.uracle.ums.fpc.config.bean.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.core.PitcherEx;
import kr.uracle.ums.fpc.tcpchecker.TcpAliveConManager;
import kr.uracle.ums.fpc.tps.TpsManager;

public class PitcherManager extends Thread{

	private final static Logger logger = LoggerFactory.getLogger(PitcherManager.class);
	
	private static List<PitcherEx> pitcherExList = new ArrayList<PitcherEx>(10);
	
	private final RootConfigExBean rootConfigBean;
	
	public PitcherManager(RootConfigExBean rootConfigBean) {
		this.rootConfigBean = rootConfigBean;
	}
	
	public boolean manage() {
		for(Entry<String, PitcherExConfigBean> e : rootConfigBean.getPITCHERS().entrySet()) {
			String name = e.getKey();
			PitcherExConfigBean config = e.getValue();
			PitcherEx px = new PitcherEx(name, rootConfigBean, config);
			pitcherExList.add(px);
			px.start();
		}
		
		return true;
	}
	
	@Override
	public void run() {
		for(PitcherEx px : pitcherExList) {
			px.close();
		}
		
	}
	public static void main(String[] args) {
		// 설정 매니저 로딩
		String configPath = args.length >0 ?args[0]:"";
		ConfigExManager configManager = new ConfigExManager(configPath, null);
		boolean isOk = configManager.load();
		if(isOk == false) {
			logger.error("설정 로드 실패로 인한 기동 중지");
			return;
		}
		
		// 설정 빈 가져오기
		RootConfigExBean rootConfig = configManager.getRootConfig();
	
		// 모니터링 설정 가져오기
		UmsMonitoringConfigBean monitConfig = rootConfig.getUMS_MONIT();
		
		if(monitConfig != null) {
			// 모니터링 서버 체크 TCP 모듈 기동
			TcpAliveConManager.getInstance().init(null, monitConfig.getUMS_IPADREESS(), monitConfig.getCYCLE_TIME());
			// 모니터링 전송 쓰레드 기동
			TpsManager.initialize(monitConfig);
		}
		
		// 매니저 Instance 생성
		PitcherManager manager = new PitcherManager(rootConfig);
		// JVM Hook Add 
		Runtime.getRuntime().addShutdownHook(manager);
		
		// Pitchers Start & Monitoring Start
		isOk = manager.manage();
		if(isOk ==false) {
			return;
		}
		
	}
	

}
