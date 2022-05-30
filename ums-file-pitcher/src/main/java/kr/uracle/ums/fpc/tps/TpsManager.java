package kr.uracle.ums.fpc.tps;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import kr.uracle.ums.fpc.config.bean.UmsMonitoringConfigBean;
import kr.uracle.ums.fpc.tcpchecker.TcpAliveConManager;
import kr.uracle.ums.sdk.httppoolclient.HttpPoolClient;
import kr.uracle.ums.sdk.httppoolclient.ResponseBean;

/**
 * Created by Y.B.H(mium2) on 17. 3. 17..
 */
public class TpsManager {
    private Logger logger = LoggerFactory.getLogger(TpsManager.class);
    private int ums_queue_size = 0;
    private int ums_max_input_cnt = 0;
    private int ums_max_process_cnt = 0;
    private int ums_pre_input_cnt = 0;
    private int ums_now_input_cnt = 0;
    private int ums_pre_process_cnt = 0;
    private int ums_now_process_cnt = 0;
    private List<TpsInfoBean> ums_tpsHistoryList = new ArrayList<TpsInfoBean>();

    private String chek_date = "";
    private static TpsManager instance = null;

    private final String PROGRAM_ID;
    private final String SERVER_ID;
    private final String SERVER_NAME;
    private final long CYCLE_TIME;
    
    private final Timer scheduler = new Timer();;
    private Gson gson = new Gson();


    public enum TPSSERVERKIND {UMS_FILEPITCHER};

    private TpsManager(UmsMonitoringConfigBean config){
    	this.PROGRAM_ID = config.getPROGRAM_ID();
        this.SERVER_ID = config.getSERVER_ID();
        this.SERVER_NAME = config.getSERVER_NAME();
        this.CYCLE_TIME = config.getCYCLE_TIME();
        startScheduler();
    }

    public synchronized static TpsManager getInstance(UmsMonitoringConfigBean config) {
        if(instance==null){
            instance = new TpsManager(config);
        }
        return instance;
    }

    public int getPre_input_cnt(){
        return ums_pre_input_cnt;
    }

    public int getPre_process_cnt() {
        return ums_pre_process_cnt;
    }

    public int getQueue_size() {
        return ums_queue_size;
    }

    public String getChek_date() {
        return chek_date;
    }

    
    public synchronized void addInputCnt() {
        this.ums_now_input_cnt = ums_now_input_cnt+1;
    }

    public synchronized void addProcessCnt() {
        this.ums_now_process_cnt = ums_now_process_cnt+1;
    }

    public synchronized void addInputCnt(int addCnt) {
        this.ums_now_input_cnt = ums_now_input_cnt+addCnt;
    }

    public synchronized void addProcessCnt(int addCnt) {
        this.ums_now_process_cnt = ums_now_process_cnt+addCnt;
    }

    protected synchronized void tpsCountReset(){

        // 파일 처리 TPS를 알기위해 처리
        if (ums_max_input_cnt < ums_now_input_cnt) {
            ums_max_input_cnt = ums_now_input_cnt;
        }
        ums_pre_input_cnt = ums_now_input_cnt;
        ums_now_input_cnt = 0;

        // 최대 TPS를 알기 위해
        if (ums_max_process_cnt < ums_now_process_cnt) {
            ums_max_process_cnt = ums_now_process_cnt;
        }
        ums_pre_process_cnt = ums_now_process_cnt;
        ums_now_process_cnt = 0;

        //TODO:: 파일 사이즈로 변경 하자
        ums_queue_size = 0;
        

        // 현재시간 셋팅
        SimpleDateFormat formatter=new SimpleDateFormat("yyyyMMddHHmmss");
        chek_date = formatter.format(new java.util.Date());
    }

    public int getMax_input_cnt(TPSSERVERKIND tpsserverkind) {
        return ums_max_input_cnt;
    }

    public long getMax_process_cnt(TPSSERVERKIND tpsserverkind) {
        return ums_max_process_cnt;
    }

    protected void putTpsInfo(TPSSERVERKIND tpsserverkind, TpsInfoBean tpsInfoBean){
        //TPS 히스토리 정보를 최대 30개 까지 저장한다.
        if (ums_tpsHistoryList.size() >= 30) {
            ums_tpsHistoryList.remove(0);
        }
        ums_tpsHistoryList.add(tpsInfoBean);
    }

    public List<TpsInfoBean> getPush_tpsHistoryList() {
        return ums_tpsHistoryList;
    }

    // SUMMARY  정보
    public Map<String,Object> getSummaryData(){
        Map<String,Object> rootSummaryMap = new HashMap<>();
        Map<String,String> summaryMap = new HashMap<>();
        summaryMap.put("INPUT",""+ums_pre_input_cnt);
        summaryMap.put("OUTPUT",""+ums_pre_process_cnt);
        summaryMap.put("MAX_INPUT",""+ums_max_input_cnt);
        summaryMap.put("MAX_OUTPUT",""+ums_max_process_cnt);
        summaryMap.put("CHKDATE", chek_date);

        rootSummaryMap.put(TPSSERVERKIND.UMS_FILEPITCHER.toString(), summaryMap);
        return rootSummaryMap;
    }

    // HISTORY CHART	
    public Map<String,Object> getLineChartDatas(){
        SimpleDateFormat formatter=new SimpleDateFormat("HH:mm");
        String nowDateStr = formatter.format(new java.util.Date());

        Map<String,Object> chartDataMap = new HashMap<String,Object>();

        List<String> inputDatas = new ArrayList<String>();
        List<String> outputDatas = new ArrayList<String>();
        List<String> labelDatas = new ArrayList<String>();
        for(TpsInfoBean tpsInfoBean : ums_tpsHistoryList){
            inputDatas.add(tpsInfoBean.getINPUT_CNT());
            outputDatas.add(tpsInfoBean.getOUT_CNT());
            labelDatas.add(tpsInfoBean.getCHECK_DATE().substring(8,10)+":"+tpsInfoBean.getCHECK_DATE().substring(10,12));
        }
        //조회한 싯점의 실시간 발송카운트 등록
        inputDatas.add(""+ums_now_input_cnt);
        outputDatas.add(""+ums_now_process_cnt);
        labelDatas.add(nowDateStr);

        List<Map<String, List<String>>> chartDataList = new ArrayList<>();
        Map<String, List<String>> inputMap = new HashMap<>();
        Map<String, List<String>> outputMap = new HashMap<>();
        Map<String, List<String>> labelMap = new HashMap<>();
        inputMap.put("INPUT",inputDatas);
        outputMap.put("OUTPUT",outputDatas);
        labelMap.put("LABEL",labelDatas);

        chartDataList.add(inputMap);
        chartDataList.add(outputMap);
        chartDataList.add(labelMap);
        chartDataMap.put(TPSSERVERKIND.UMS_FILEPITCHER.toString(), chartDataList);

        return chartDataMap;
    }

    public void startScheduler() {
        logger.info("### TPS MANAGER 스케줄 START!");
        scheduler.schedule(new TimerTask() {
            @Override
            public void run() {
                tpsCountReset();
                logger.debug("### TPS MANAGER  one minute Sent total count: {}", ums_pre_process_cnt);
    
                TpsInfoBean tpsInfoBean = new TpsInfoBean();
                tpsInfoBean.setSERVER_KIND(TPSSERVERKIND.UMS_FILEPITCHER.toString());
                tpsInfoBean.setINPUT_CNT("" + ums_pre_input_cnt);
                tpsInfoBean.setOUT_CNT("" + ums_pre_process_cnt);
                tpsInfoBean.setMAX_INPUT_CNT("" + ums_max_input_cnt);
                tpsInfoBean.setMAX_OUTPUT_CNT("" + ums_max_process_cnt);
                tpsInfoBean.setQUEUE_SIZE("" + ums_queue_size);
                tpsInfoBean.setCHECK_DATE(chek_date);
                putTpsInfo(TPSSERVERKIND.UMS_FILEPITCHER,tpsInfoBean);

                try {
                    // UMS에 모니터링정보 수집할수 있도록 호출
                    String ums_host_url = TcpAliveConManager.getInstance().getConHostName();
                    String umsCallApi = ums_host_url+"monit/report.ums";
                    Map<String,Object> reqParam = new HashMap<>();
                    reqParam.put("PROGRAM_ID"	,	PROGRAM_ID);
                    reqParam.put("SERVER_ID"	,	SERVER_ID);
                    reqParam.put("SERVER_NAME"	,	SERVER_NAME);
                    reqParam.put("MONITOR_URL"	,	"");
                    reqParam.put("REQUESTER_ID"	,	PROGRAM_ID);
                    reqParam.put("CHART"		,	getLineChartDatas());
                    reqParam.put("SUMMARY"		,	getSummaryData());
                    ResponseBean responseBean = HttpPoolClient.getInstance().sendJsonPost(umsCallApi,reqParam);
                    if(responseBean.getStatusCode()==200 || responseBean.getStatusCode()==201){
                        logger.info("UMS 모니터링 정보 호출 성공 :"+responseBean.getBody());
                    }else{
                        logger.info("UMS 모니터링 정보 호출 에러 : HTTP ERROCODE:"+responseBean.getStatusCode()+" HTTP BODY:" +responseBean.getBody());
                    }
                }catch (Exception e){
                    logger.error("UMS 모니터링 정보 호출시 에러발생. 이유 : "+e.toString());
                }
            }
        }, 0, 1 * 60 * 1000);
    }
    
    public void stopScheduler() {
        scheduler.cancel();
        logger.info("### TPS MANAGER 스케줄 STOP!");
    }

}