package kr.uracle.ums.fpc.core;

import java.nio.file.Path;

public class Handler extends Thread{
	
	private Path TARGET_PATH;
	
	private PreHandle preHandler = null;
	private MainHandle mainHandler = null;
	private PostHandle postHandler = null;
	
	public Handler(Path TARGET_PATH) {
		this.TARGET_PATH = TARGET_PATH;
	}

	@Override
	public void run() {
		int prcsCnt = 0;
		if(preHandler != null) {
			TARGET_PATH = preHandler.handle(TARGET_PATH);
			if(TARGET_PATH == null) prcsCnt = -1;
		}
		
		if(mainHandler != null) {
			prcsCnt = mainHandler.handle(TARGET_PATH, prcsCnt);	
		}
		
		if(postHandler != null) {
			prcsCnt = postHandler.handle(TARGET_PATH, prcsCnt);
		}
	}
	
	public void setPreHandler(PreHandle preHandler) { this.preHandler = preHandler;	}
	public void setMainHandler(MainHandle mainHandler) { this.mainHandler = mainHandler; }
	public void setPostHandler(PostHandle postHandler) { this.postHandler = postHandler; }
	
}
