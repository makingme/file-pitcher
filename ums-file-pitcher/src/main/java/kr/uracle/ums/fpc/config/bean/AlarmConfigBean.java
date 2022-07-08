package kr.uracle.ums.fpc.config.bean;

import java.util.ArrayList;
import java.util.List;

import kr.uracle.ums.sdk.util.UmsAlarmSender.CHANNEL;

public class AlarmConfigBean {

	private CHANNEL SEND_CHANNEL;
	private String URL;
	private String PREFIX_MESSAGE = "";
	private List<String> TARGETS = new ArrayList<String>(10);
		
	public CHANNEL getSEND_CHANNEL() { return SEND_CHANNEL;	}
	public void setSEND_CHANNEL(CHANNEL sEND_CHANNEL) { SEND_CHANNEL = sEND_CHANNEL; }
	
	public String getURL() { return URL; }
	public void setURL(String uRL) { URL = uRL;	}
	
	public String getPREFIX_MESSAGE() { return PREFIX_MESSAGE;	}
	public void setPREFIX_MESSAGE(String pREFIX_MESSAGE) { PREFIX_MESSAGE = pREFIX_MESSAGE;	}	
	
	public List<String> getTARGETS() { return TARGETS; }
	public void setTARGETS(List<String> tARGETS) { TARGETS = tARGETS; }
	
	
}
