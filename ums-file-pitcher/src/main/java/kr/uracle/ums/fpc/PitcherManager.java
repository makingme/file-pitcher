package kr.uracle.ums.fpc;



import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kr.uracle.ums.fpc.bean.config.PitcherConfigBean;
import kr.uracle.ums.fpc.bean.config.RootConfigBean;
import kr.uracle.ums.fpc.bean.config.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.core.PitcherEx;
import kr.uracle.ums.fpc.tcpchecker.TcpAliveConManager;
import kr.uracle.ums.fpc.tps.TpsManager;
import kr.uracle.ums.fpc.utils.ConfigManager;

public class PitcherManager extends Thread{

	private final static Logger logger = LoggerFactory.getLogger(PitcherManager.class);
	
	private static List<PitcherEx> pitcherExList = new ArrayList<PitcherEx>(10);
	
	private final RootConfigBean rootConfigBean;
	
	public PitcherManager(RootConfigBean rootConfigBean) {
		this.rootConfigBean = rootConfigBean;
	}
	
	public boolean manage() {
		for(Entry<String, PitcherConfigBean> e : rootConfigBean.getPITCHERS().entrySet()) {
			String name = e.getKey();
			PitcherConfigBean config = e.getValue();
			PitcherEx px = new PitcherEx(name, rootConfigBean, config);
			boolean isOk = px.initailize();
			if(isOk == false) {
				logger.error("{} 초기화 중 에러 발생으로 기동 중지", name);
				return false;
			}
			pitcherExList.add(px);
			px.start();
		}
		
		return true;
	}
	
	@Override
	public void run() {
		logger.info("종료 요청 시그널에 따른 종료");
		for(PitcherEx px : pitcherExList) {
			px.close();
		}
		
	}
	public static void main(String[] args) {
		// 설정 매니저 로딩
		String configPath = args.length >0 ?args[0]:"";
		ConfigManager configManager = new ConfigManager(configPath, null);
		boolean isOk = configManager.load();
		if(isOk == false) {
			logger.error("설정 로드 실패로 인한 기동 중지");
			return;
		}
		
		// 설정 빈 가져오기
		RootConfigBean rootConfig = configManager.getRootConfig();
	
		// 모니터링 설정 가져오기
		UmsMonitoringConfigBean monitConfig = rootConfig.getUMS_MONIT();
		
		if(monitConfig != null) {
			// 모니터링 서버 체크 TCP 모듈 기동
			System.out.println("###########################################################################################");
			System.out.println("###########################################################################################");
			TcpAliveConManager.getInstance().init(null, monitConfig.getUMS_IPADREESS(), monitConfig.getCYCLE_TIME());
			System.out.println("###########################################################################################");
			System.out.println("###########################################################################################");
			// 모니터링 전송 쓰레드 기동
			TpsManager.initialize(monitConfig);
			System.out.println("###########################################################################################");
			System.out.println("###########################################################################################");
		}
		
		// 매니저 Instance 생성
		PitcherManager manager = new PitcherManager(rootConfig);
		// JVM Hook Add 
		Runtime.getRuntime().addShutdownHook(manager);
		
		// Pitchers Start & Monitoring Start
		isOk = manager.manage();
		if(isOk == false) {
			System.exit(0);
		}
		
	}
	

}
