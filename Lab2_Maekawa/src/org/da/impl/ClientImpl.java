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
import org.da.util.MessageDeliveryQueue;
import org.da.util.PeerEntry;

public class ClientImpl extends java.rmi.server.UnicastRemoteObject implements Client {

	private static final long serialVersionUID = 1L;
	private GrantData grantData;
	private List<PeerEntry> peers;
	private Map<Integer, Link> requestSet;
	private MessageDeliveryQueue requestBacklog;
	private MessageDeliveryQueue queue;
	private Boolean inCriticalState;
	private Boolean inquiring;
	private Boolean inquired;
	private Boolean postponed;
	private Boolean hasGrantedProcess;
	private Integer pid;
	private TimeStamp ts;
	private int noGrants;

	protected ClientImpl() throws RemoteException {
		super();
	}

	// upon receiving a request message from a process
	private void grantProccess(int pid) {
		// Send grantmsg to the process
	}
	
	private Message topMsg(){
		return null;
	}
	
	private Message pop(){
		return null;
	}
	
	// Upon receiving a release message from a process
	private void releaseProccess(int pid) {
		
	}
	
	// Enter critical state
	private void enterCriticalState(){
		this.inCriticalState = true;
	}
	
	@Override
	public void putMessage(Message msg) throws RemoteException{
		int id = msg.getPID();
		Link l = this.requestSet.get(id);
		l.addMessage(msg);
	}
	
	// msg should be either REQUEST OR RELEASE
	public void BroadCastMsg(Message msg) {
		this.ts = this.ts.inc();
		for (PeerEntry pe : this.peers) {
			try {
				pe.p.putMessage(msg);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
//		if(!this.hasGrantedProcess) {
//			Message msg = new MessageImpl(MessageType.REQUEST, this.ts, this.pid);
//
//		}
	}

	public void ReceiveMsg(Message msg) throws RemoteException {
		switch(msg.getMessageType()) {
			case REQUEST : 
				if(!this.hasGrantedProcess) {
					this.ts = this.ts.sync(msg.getTimeStamp());
					this.grantData.pid = msg.getPID();
					this.grantData.ts = msg.getTimeStamp();
					Message currentGrant = new MessageImpl(MessageType.GRANT, ts, this.pid);
					
					Link l = this.requestSet.get(msg.getPID());
					l.addMessage(currentGrant);				
					this.hasGrantedProcess = true;
				} else {
					this.requestBacklog.insert(msg);
					Message head = this.requestBacklog.peek();
					
					if (( this.grantData.ts.compareTo(msg.getTimeStamp()) < 0 ) ||
							head.getTimeStamp().compareTo(msg.getTimeStamp()) < 0) {
						sendMsg(MessageType.POSTPONED, msg.getPID());
					} else  {
						if (!this.inquiring) {
							this.inquiring = true;
							sendMsg(MessageType.INQUIRE, this.grantData.pid);
						}
					}
				}	
				break;
			case GRANT : 
				this.noGrants += 1;
				if(this.noGrants == this.requestSet.size()) {
					this.postponed = false;
					this.inCriticalState = true;
					for (PeerEntry pe : this.peers) {
						try {
							pe.p.putMessage(new MessageImpl(MessageType.RELEASE, this.ts, this.pid));
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				break;
			case INQUIRE : 
				// upon receipt of (inquire;j) do
					// wait until ((postponed) or (no grants = |R|))
					// if (postponed) then
							// no grants <- no grants-1
							// send(relinquish) to P j
				
				if(!this.postponed || (this.noGrants == this.requestSet.size())){
					this.inquired = true;
					break;
				} else {
					if(this.postponed) {
						this.noGrants += 1;
						for (PeerEntry pe : this.peers) {
							try {
								pe.p.putMessage(new MessageImpl(MessageType.RELINQUISH, this.ts, this.pid));
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
				break;
			case RELINQUISH : 
				this.inquiring = false;
				this.hasGrantedProcess = false;
				this.requestBacklog.insert(new MessageImpl(MessageType.REQUEST, this.grantData.ts, this.grantData.pid));
				this.grantData.pid = this.requestBacklog.peek().getPID();
				this.grantData.ts = this.requestBacklog.peek().getTimeStamp();	
				this.hasGrantedProcess = true;
				Peer p = findPeer(this.grantData.pid);
				p.putMessage(new MessageImpl(MessageType.GRANT, this.ts, this.pid));
				break;
			case RELEASE : 
				Release();
				Message request = null;
				if(null != (request = this.requestBacklog.peek())) {
					this.Grant(request);
				}
				break;
			case POSTPONED : 
				this.postponed = true;
				break;
		default:
			break;
		}
	}
	
	private void Grant(Message request) throws RemoteException{
		int id = request.getPID();
		Peer p = findPeer(id);
		p.putMessage(new MessageImpl(MessageType.GRANT, this.ts, this.pid));
		this.grantData.pid = request.getPID();
		this.grantData.ts = request.getTimeStamp();
		this.hasGrantedProcess = true;
	}
	
	private void Release(){
		this.hasGrantedProcess = false;
		this.inquiring = false;		
		this.requestBacklog.Remove(new MessageImpl(MessageType.REQUEST, this.grantData.ts, this.grantData.pid));
	}
	
	private Peer findPeer(int id) {
		Peer p = null;
		for (PeerEntry pe : this.peers) {
			if(pe.peerId == id) {
				p = pe.p;
			}
		}
		
		if(p == null) {
			throw new RuntimeException("Peer ID not known");
		}
		
		return p;
	}
	
	private void sendMsg(MessageType t, int peerId){
		this.ts = this.ts.inc();
		try {
			findPeer(peerId).putMessage(new MessageImpl(t, this.ts, this.pid));
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void activate(List<PeerEntry> peers, Integer ownId)
			throws RemoteException {
		this.peers = peers;
		this.inCriticalState = false;
		this.hasGrantedProcess = false;
		this.inquiring = false;
		this.inquired = false;
		this.postponed = false;
		this.pid = ownId;
		this.ts = new TimeStamp(0, this.pid);
		this.requestSet = new HashMap<Integer, Link>();
		this.requestBacklog = new MessageDeliveryQueue();
		this.queue= new MessageDeliveryQueue();
		this.grantData = null;
		this.noGrants = 0;
	}
	
	private void updateMessages(){
		for (Link l: this.requestSet.values()){
			Message m;
			while(null != (m = l.popMessage())){
				this.queue.insert(m);
			}
		}
	}
	
	private void startRequesting(){
		
	}
	
	public void mainLoop(int num_rounds) throws InterruptedException, RemoteException{
		while(num_rounds-- > 0){
			if(inquired){
				// do inquire receive msg body
			} else {
				if(inCriticalState) {
					// do random amount of work
					Thread.sleep(100);
					this.inCriticalState = false;
					// Release bcast
				} else {
					updateMessages();
					Message m = null;
					while (null != (m = this.queue.pop())){
						this.ReceiveMsg(m);
					}
				}
			} 
		}
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
	
	private class GrantData {
		public int pid;
		public TimeStamp ts;
	}
}
