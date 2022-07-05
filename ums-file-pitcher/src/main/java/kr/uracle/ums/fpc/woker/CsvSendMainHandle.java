package kr.uracle.ums.fpc.woker;

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
 * @see : 사용자 지정 설정(PARAM_MAP)에 ERROR_PATH : 절대경로 설정 정보 지정 시 필터링 파일 지정 경로로 이동 시킴(기본값:PRCS_NAME_ERROR)
 */
public class CsvSendMainHandle extends MainHandle{
	
	private final UmsPotalClient umsPotalClient = new UmsPotalClient();
	
	private List<String> HEADER_LIST;
	
	private String DELIMETER = "\\|";
	private String ERROR_PATH; 
	private String URL;
	private String TRANS_TYPE;
	
	private final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMdd_HHmmss");
	
	private final Gson gson = new Gson();
		
	public CsvSendMainHandle(String PRCS_NAME, Map<String, Object> PARAM_MAP) {
		super(PRCS_NAME, PARAM_MAP);	
	}
	

	@Override
	public boolean initailize() {
		// 불량 파일 옮길 경로 취득
		Object object = PARAM_MAP.get("ERROR_PATH");
		String errorPath = ObjectUtils.isEmpty(object)?"":object.toString();
		if(Paths.get(errorPath).isAbsolute() == false) {			
			errorPath = "";
		}
		ERROR_PATH = errorPath;
		

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
	public int process(Path path, boolean isPreSuccess) {
		int prcsCnt = 0;
		
		if(StringUtils.isBlank(URL)) {
			logger.error("URL 정보 없음, 설정 확인");
			return -1;
		}
		
		if(ObjectUtils.isEmpty(HEADER_LIST)) {
			logger.error("헤더 정보 누락 됨");
			return -1;
		}

		try {			
			// 전처리 실패에된 파일을 에러 디렉토리 경로로 이동
			if(isPreSuccess == false) {
				String errorPath = StringUtils.isBlank(ERROR_PATH)?path.getParent().getParent()+File.separator+PRCS_NAME+"_ERROR":ERROR_PATH;
				if(errorPath.endsWith(File.separator) == false ) errorPath+=File.separator;
				Files.move(path, Paths.get(errorPath+path.getFileName().toString()), StandardCopyOption.REPLACE_EXISTING);
				return 0;
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
		
		ResultVo resultVo = umsPotalClient.umsSend(umsVo, 30);
		prcsCnt = resultVo.getRESULTCODE().equals("0000")?prcsCnt:-1; 
		if(prcsCnt<0 && StringUtils.isNotBlank(resultVo.getRESULTMSG())) {
			try {
				String FILE_NAME =path.getFileName().toString();
				String errorLogFile = ERROR_PATH+File.separator+ FILE_NAME.substring(0, FILE_NAME.lastIndexOf("."))+"_error.log";
				Files.write(Paths.get(errorLogFile), resultVo.getRESULTMSG().getBytes());
			} catch (IOException e) { logger.error("무시 - 실패 로그({}) 작성 중 에러", path.toString());}
		}

		return prcsCnt;
	}
	
	private int parseFile(Path p, Map<String, String> commonMap) {

		// UMS CSV 발송을 위한 CSV 파일 
		String csvFile = p.getParent().getParent()+File.separator+ PRCS_NAME+"_CSV"+File.separator+getNewFileName(p);
		int lineCnt =0;
		try(BufferedReader reader = Files.newBufferedReader(p);
				CSVWriter writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(csvFile)), ',', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);	
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
			while((line = reader.readLine()) != null) {
				// 첫줄은 공통 정보 - JSON 규격
				if(lineCnt == 0) {
					commonMap = gson.fromJson(line, new TypeToken<Map<String, String>>(){}.getType());
					if(commonMap == null || commonMap.size() <= 0) {
						logger.error("{} 파일 처리 실패 - 헤더 정보 없음 : {}", p.toString(), line);
						return -1;
					}
				}else {
					String[] array = line.split(DELIMETER);
					writer.writeNext(array);
				}
				lineCnt+=1;
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
	
	private String getNewFileName(Path p) {
		String FILE_NAME = p.getFileName().toString();
		return FILE_NAME.substring(0, FILE_NAME.lastIndexOf("."))+"_"+DATE_TIME_FORMAT.format(LocalDateTime.now())+".csv";
	}
	
}
