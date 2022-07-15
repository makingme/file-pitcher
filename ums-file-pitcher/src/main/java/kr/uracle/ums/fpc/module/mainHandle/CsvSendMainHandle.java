package kr.uracle.ums.fpc.module.mainHandle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.opencsv.CSVWriter;

import kr.uracle.ums.fpc.bean.config.AlarmConfigBean;
import kr.uracle.ums.fpc.bean.config.ModuleConfigBean;
import kr.uracle.ums.fpc.core.MainHandle;
import kr.uracle.ums.fpc.tps.TpsManager;
import kr.uracle.ums.sdk.UmsPotalClient;
import kr.uracle.ums.sdk.vo.ResultVo;
import kr.uracle.ums.sdk.vo.TargetUserKind;
import kr.uracle.ums.sdk.vo.TransType;
import kr.uracle.ums.sdk.vo.UmsPotalParamVo;

/**
 * @author URACLE KKB
 * @see : 일반 파일을 UMS CSV 파일로 변경하여 UMS CSV 발송 요청 처리
 * @see : 사용자 지정 설정(PARAM_MAP)에 HEADER_LIST : CSV헤더 목록(LIST) 필수 지정, CSV 헤더 구성
 * @see : 사용자 지정 설정(PARAM_MAP)에 URL : UMS CSV 발송 URL 필수 지정 값
 * @see : 사용자 지정 설정(PARAM_MAP)에 TRANS_TYPE : REAL/BATCH 실시간 혹은 배치 여부 - BATCH 권장
 * @see : 사용자 지정 설정(PARAM_MAP)에 DELIMETER : 구분자 지정, 파일 데이터 부 파싱 구분자(기본값: |)
 * @see : 사용자 지정 설정(PARAM_MAP)에 ERROR_PATH : 절대경로 설정 정보 지정 시 에러 파일 지정 경로로 이동 시킴(기본값:PRCS_NAME_ERROR)
 * @see : 사용자 지정 설정(PARAM_MAP)에 CSV_PATH : 절대경로 설정 정보 지정 시 CSV 파일 지정 경로로 이동 시킴(기본값:PRCS_NAME_CSV)
 */
public class CsvSendMainHandle extends MainHandle{
	
	private final UmsPotalClient umsPotalClient = new UmsPotalClient();
	
	private List<String> HEADER_LIST;
	
	private String DELIMETER = "\\|";
	private String ERROR_PATH;
	private String CSV_PATH; 
	private String URL;
	private String TRANS_TYPE;
		
	private String CSV_FILE_PATH;
	
	private final Gson gson = new Gson();
		
	public CsvSendMainHandle(ModuleConfigBean MODULE_CONFIG, AlarmConfigBean ALARM_CONFIG) {
		super(MODULE_CONFIG, ALARM_CONFIG);
	}

	@Override
	public boolean initailize() {
		// 파일 구분자 취득
		Object dObject = PARAM_MAP.get("DELIMETER");
		DELIMETER = ObjectUtils.isEmpty(dObject)?DELIMETER:dObject.toString();

		// 불량 파일 옮길 경로 취득
		Object object = PARAM_MAP.get("ERROR_PATH");
		String errorPath = ObjectUtils.isEmpty(object)?"":object.toString();
		if(Paths.get(errorPath).isAbsolute() == false) {			
			errorPath = "";
		}
		ERROR_PATH = errorPath;
		
		//CSV 파일 옮길 경로 취득
		Object cbject = PARAM_MAP.get("CSV_PATH");
		String csvPath = ObjectUtils.isEmpty(cbject)?"":cbject.toString();
		if(Paths.get(csvPath).isAbsolute() == false) {			
			csvPath = "";
		}
		CSV_PATH = csvPath;
		
		Object hObject = PARAM_MAP.get("HEADER_LIST");
		if((hObject instanceof List<?>)==false) {
			logger.error("HEADER_LIST 설정이 누락됨");
			return false;
		}
		
		if(ObjectUtils.isEmpty(PARAM_MAP.get("URL")) ||StringUtils.isBlank(PARAM_MAP.get("URL").toString())) {
			logger.error("URL 설정이 누락됨");
			return false;
		}
		
		HEADER_LIST = (List<String>)hObject;
		URL = PARAM_MAP.get("URL").toString();
		TRANS_TYPE = PARAM_MAP.get("TRANS_TYPE")!=null? PARAM_MAP.get("TRANS_TYPE").toString():"BATCH";
		return true;
	}

	@Override
	public int process(Path path, int preResultCode) {
		int prcsCnt = -1;
		
		if(StringUtils.isBlank(ERROR_PATH)) {
			ERROR_PATH = path.getParent().getParent()+File.separator+PRCS_NAME+"_ERROR"+File.separator;
		}
		
		Path errorPath = Paths.get(ERROR_PATH);
		if(Files.exists(errorPath) == false) {
			try {
				Files.createDirectories(errorPath);
			} catch (IOException e) {
				e.printStackTrace();
				return -1;
			}
			logger.info("{} 디렉토리 생성", ERROR_PATH);
		}			
		
		try {			
			// 전처리 실패에된 파일을 에러 디렉토리 경로로 이동
			if(preResultCode < 0) {
				Files.move(path, Paths.get(ERROR_PATH+path.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
				return 1;
			}
		}catch (Exception e) {
			logger.error("{} 파일 전처리 실패 결과 처리(에러 경로로 이동) 중 에러 발생:{}", path, e);
			return -1;
		}
		
		Map<String, String> commonMap = new HashMap<String, String>();
		prcsCnt = parseFile(path, commonMap);
		
		if(prcsCnt <= 0) return prcsCnt;
		
		// 파일 헤더 내용 JSON STRING으로 변환
		UmsPotalParamVo umsVo = null;
		try {
			String jsonStr =gson.toJson(commonMap);
			umsVo = gson.fromJson(jsonStr, UmsPotalParamVo.class);
		}catch(JsonSyntaxException e) {
			logger.error("파일 헤더부 데이터 포맷 이상 JSON 변환 불가 - 실패 처리, 헤더부 :{} , 에러메시지:{}", commonMap.toString(), e);
			return -1;
		}

		umsVo.setTARGET_USER_TYPE(TargetUserKind.NC);
		umsVo.setUMS_URL(URL);
		umsVo.setREQ_TRAN_TYPE(TRANS_TYPE.equalsIgnoreCase("REAL")?TransType.REAL:TransType.BATCH);
		if(StringUtils.isBlank(commonMap.get("MSG_TYPE"))) umsVo.setMSG_TYPE("A");
		umsVo.setCSVFILE_ABS_SRC(CSV_FILE_PATH);
		
		ResultVo resultVo = umsPotalClient.umsSend(umsVo, 30);
		prcsCnt = resultVo.getRESULTCODE().equals("0000")?prcsCnt:-1; 			

		if(prcsCnt<0 &&  StringUtils.isNotBlank(resultVo.getRESULTMSG())) {
			logger.warn("{} 파일 본 처리 실패", path);
			try {
				String FILE_NAME =path.getFileName().toString();
				String errorLogFile = ERROR_PATH+File.separator+ FILE_NAME.substring(0, FILE_NAME.lastIndexOf("."))+"_error.log";
				String errorMsg = "결과코드:"+resultVo.getRESULTCODE()+" - "+resultVo.getRESULTMSG();
				Files.write(Paths.get(errorLogFile), errorMsg.getBytes());
			} catch (IOException e) { logger.error("무시 - 파일 본 처리 실패에 따른 원인 로그 작성 중 에러", path.toString());}
		}

		return prcsCnt;
	}
	
	private int parseFile(Path p, Map<String, String> commonMap) {

		// UMS CSV 발송을 위한 CSV 파일
		if(StringUtils.isBlank(CSV_PATH)) {
			CSV_PATH = p.getParent().getParent()+File.separator+ PRCS_NAME+"_CSV"+File.separator;
		}
		
		try {
			Path derectory = Paths.get(CSV_PATH);
			if(Files.exists(derectory) == false) {
				Files.createDirectories(derectory);
				logger.info("{} 디렉토리 생성", CSV_PATH);
			}			
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
		
		CSV_FILE_PATH = CSV_PATH+getCsvFileName(p);
		if(Files.exists(Paths.get(CSV_FILE_PATH))) {
			logger.error("{} - 동일 파일이 존재 합니다", CSV_FILE_PATH);
			return -1;
		}
		int lineCnt =0;
		try(BufferedReader reader = Files.newBufferedReader(p);
				CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(CSV_FILE_PATH)), ',', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);	
		){
			// CSV에 헤더 정보 쓰기
			String[] csvHeader = new String[HEADER_LIST.size()];
			int index =0;
			for(String s : HEADER_LIST) {
				csvHeader[index] = "#{"+s+"}";
				index +=1;
			}
			
			writer.writeNext(csvHeader);
						
			String line = null;
			boolean isHeader = true;
			while((line = reader.readLine()) != null) {
				// 개행 전 까지 헤더 영역
				if(StringUtils.isBlank(line)) {
					isHeader = false;
					continue;
				}
				if(isHeader) {
					Map<String, String> map = gson.fromJson(line, new TypeToken<Map<String, String>>(){}.getType());
					if(ObjectUtils.isEmpty(map)) {
						logger.error("{} 파일 처리 실패 - 헤더 정보 없음 : {}", p.toString(), line);
						return -1;
					}
					for(Entry<String, String> e: map.entrySet()) {
						commonMap.put(e.getKey(), e.getValue());
					}
					
				}else {
					String[] array = line.split(DELIMETER);
					writer.writeNext(array);
					lineCnt+=1;
				}
			}
			writer.flush();
			
		}catch(IOException e) {
			logger.error("{} 파일 처리 중 에러 발생:{}", p.toString(), e);
			return -1;
		}catch(JsonSyntaxException | IllegalStateException e) {
			logger.error("{} 파일 CSV로 변환 중 에러 발생:{}", p.toString(), e);
			return -1;
		}
		
		return lineCnt;
	}
	
	private String getCsvFileName(Path p) {
		String FILE_NAME = p.getFileName().toString();
		return FILE_NAME.substring(0, FILE_NAME.lastIndexOf("."))+".csv";
	}
	
}
