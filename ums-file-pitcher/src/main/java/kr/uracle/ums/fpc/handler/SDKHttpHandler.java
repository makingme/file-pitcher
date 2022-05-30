package kr.uracle.ums.fpc.handler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

import kr.uracle.ums.fpc.config.bean.PitcherConfigBean;
import kr.uracle.ums.fpc.config.bean.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.core.Handler;
import kr.uracle.ums.sdk.UmsPotalClient;
import kr.uracle.ums.sdk.vo.ResultVo;
import kr.uracle.ums.sdk.vo.TargetUserKind;
import kr.uracle.ums.sdk.vo.TransType;
import kr.uracle.ums.sdk.vo.UmsPotalParamVo;

public class SDKHttpHandler extends Handler{
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private final UmsPotalClient umsPotalClient = new UmsPotalClient();
	
	public SDKHttpHandler(PitcherConfigBean pitcherConfig, UmsMonitoringConfigBean monitConfig, Path source, Integer index) {
		super(pitcherConfig, monitConfig, source, index);
		this.setName(index+"th "+this.getClass().getSimpleName());
	}


	@Override
	protected boolean preProcess(Path source) {
		// 파일 옮기기
		Path target = Paths.get(PITCHER_CONFIG.getTARGET_PATH()+source.getFileName().toString());
		try {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			log.error("파일 이동 중 에러 발생:{}", e);
			e.printStackTrace();
			return false;
		}
		// 옮긴 파일을 소스 파일로 지정
		SOURCE_PATH = target;
		
		// 파일 파싱하기
		return parseFile(SOURCE_PATH);
	}
	
	@Override
	public boolean hadle(Path source, boolean isSuccess){
		// 선처리기 실패 시 바로 실패 처리
		if(isSuccess == false) {
			log.info("{} 선처리 결과 실패로 실패 처리", source.toString());
			return isSuccess;
		}
		
		// 설정의 커스텀 설정 값 가져오기 
		String url = CUSTOM_MAP.get("UMS_URL");
		String transTyep = CUSTOM_MAP.get("TRANS_TYPE");
		// 커스텀 설정값이 없으면 실패 처리
		if(StringUtils.isAnyBlank(url, transTyep)) {
			log.error("{} 처리 중 에러 발생 - CUSTOM_MAP의 UMS_URL({}) 또는 TRANS_TYPE({}) 설정 값이 없음", source.toString(), url, transTyep);
			return false;
		}
		
		// 파일 헤더 내용 JSON STRING으로 변환
		UmsPotalParamVo umsVo = null;
		try {
			String jsonStr =gson.toJson(headerMap);
			umsVo = gson.fromJson(jsonStr, UmsPotalParamVo.class);
		}catch(JsonSyntaxException e) {
			log.error("파일 헤더부 데이터 포맷 이상 JSON 변환 불가 - 실패 처리, 헤더부 :{} , 에러메시지:{}", headerMap.toString(), e);
			return false;
		}
	
		umsVo.setTARGET_USER_TYPE(TargetUserKind.NC);
		umsVo.setUMS_URL(url);
		umsVo.setREQ_TRAN_TYPE(transTyep.equalsIgnoreCase("REAL")?TransType.REAL:TransType.BATCH);
		if(StringUtils.isBlank(headerMap.get("MSG_TYPE"))) umsVo.setMSG_TYPE("A");
		umsVo.setCSVFILE_ABS_SRC(convertFilePath.toString());
		
		
		ResultVo resultVo = umsPotalClient.umsSend(umsVo, 30);
		isSuccess = resultVo.getRESULTCODE().equals("0000");
		if(isSuccess == false && StringUtils.isNotBlank(resultVo.getRESULTMSG())) {
			try {
				String errorLogFile = FAIL_PATH.toString()+File.separator+ FILE_NAME.substring(0, FILE_NAME.lastIndexOf("."))+"_error.log";
				Files.write(Paths.get(errorLogFile), resultVo.getRESULTMSG().getBytes());
			} catch (IOException e) { log.error("무시 - 실패 로그({}) 작성 중 에러", source.toString());}
		}
		
		return isSuccess;
	}


	@Override
	protected boolean postProcess(Path source, boolean isSuccess) {
		return move(source, isSuccess);
	}







	
}
