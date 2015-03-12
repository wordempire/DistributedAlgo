package org.da.impl;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.da.model.Client;
import org.da.model.Link;
import org.da.model.Message;
import org.da.model.MessageType;
import org.da.model.Peer;
import org.da.model.Server;
import org.da.model.TimeStamp;
import org.da.util.PeerEntry;

public class ClientImpl extends java.rmi.server.UnicastRemoteObject implements Client {

	private static final long serialVersionUID = 1L;
	private List<PeerEntry> peers;
	private Map<Integer, Link> requestSet;
	private List<Message> backlog;
	private Message currentGrant;
	private Boolean inCriticalState;
	private Boolean hasGrantedProcess;
	private Integer pid;
	private TimeStamp ts;

	protected ClientImpl() throws RemoteException {
		super();
	}

	// upon receiving a request message from a process
	private void grantProccess(int pid) {
		// Send grantmsg to the process
	}
	
	// Upon receiving a release message from a process
	private void releaseProccess(int pid) {
		
	}
	
	// Enter critical state
	private void enterCriticalState(){
		this.inCriticalState = true;
	}
	
	private boolean isAllowdToEnterCS() {
		return false;
	}
	
	@Override
	public void putMessage(Message msg) throws RemoteException{
		int id = msg.getPID();
		Link l = this.requestSet.get(id);
		l.addMessage(msg);
	}
	
	public void BroadCastMsg() {
		if(!this.hasGrantedProcess) {
			Message msg = new MessageImpl(MessageType.REQUEST, this.ts, this.pid);
			
			for (PeerEntry pe : this.peers) {
				try {
					pe.p.putMessage(msg);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void ReceiveMsg(Message msg) throws RemoteException {
		if(msg.getMessageType() == MessageType.REQUEST) {
			if(!this.hasGrantedProcess) {
				this.ts = this.ts.sync(msg.getTimeStamp());
				this.currentGrant = new MessageImpl(MessageType.GRANT, ts, this.pid);
				
				Link l = this.requestSet.get(msg.getPID());
				l.addMessage(this.currentGrant);				
				this.hasGrantedProcess = true;
			} else {
				this.backlog.add(msg);
			}
		} else if (msg.getMessageType() == MessageType.GRANT) {
						
		}
	}
	
	@Override
	public void activate(List<PeerEntry> peers, Integer ownId)
			throws RemoteException {
		this.peers = peers;
		this.inCriticalState = false;
		this.hasGrantedProcess = false;
		this.pid = ownId;
		this.ts = new TimeStamp(0, this.pid);
		this.requestSet = new HashMap<Integer, Link>();
		this.backlog = new ArrayList<Message>();
		this.currentGrant = new MessageImpl(MessageType.GRANT, ts, this.pid);
	}
	
	public static void main(String[] args) throws InterruptedException, MalformedURLException, RemoteException, NotBoundException{
		// setup registry
		Server server = (Server) java.rmi.Naming.lookup("rmi://localhost:1089/register");				
		ClientImpl c = new ClientImpl();
		
		server.register(c);
//		int waitRounds = 20;
//		while(!c.loopFlag && waitRounds > 0){
//			Thread.sleep(500);
//			waitRounds--;
//		}
//		Thread.sleep(2000); // settling time for other processes
//		c.mainLoop();
	}
}
