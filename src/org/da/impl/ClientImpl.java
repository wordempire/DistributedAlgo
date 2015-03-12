package org.da.impl;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.da.model.Ack;
import org.da.model.Client;
import org.da.model.Deliverable;
import org.da.model.Link;
import org.da.model.Message;
import org.da.model.Server;
import org.da.util.MessageDeliveryQueue;
import org.da.util.PeerEntry;

public class ClientImpl extends java.rmi.server.UnicastRemoteObject implements Client{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<Integer,Link> links;
	private MessageDeliveryQueue queue;
	private List<PeerEntry> peers;
	private Boolean loopFlag;
	
	private Integer pid;
	private TimeStamp clock;
	
	public ClientImpl() throws RemoteException {
		super();
		this.loopFlag = false;
		this.links = new HashMap<Integer, Link>();		
	}

	// Assume link already exists!
	@Override
	public void putMessage(Message m) throws RemoteException {
		// someone gives me a message
		Integer originPid = new Integer(m.getTimeStamp().getPid());
		Link placeholder = links.get(originPid);
		if(placeholder == null){
			throw new RuntimeException("Received a message before links were correctly setup");
		}
		System.out.println("Proc " + this.pid + " to (link)queue: " + m.toString());
		placeholder.addDeliverable(new DeliverableImpl(m));		
	}

	@Override
	public void putAck(Ack a) throws RemoteException {
		Integer originPid = new Integer(a.getTimeStamp().getPid());
		Link placeholder = links.get(originPid);
		if(placeholder == null){
			throw new RuntimeException("Received an Acknowledgement before links were correctly setup");
		}
		System.out.println("Proc " + this.pid + "to (link)queue: " + a.toString());
		placeholder.addDeliverable(new DeliverableImpl(a));		
	}

	@Override
	public void activate(List<PeerEntry> peers, Integer ownId)
			throws RemoteException {
		this.pid = ownId;
		this.peers = peers;		
		this.queue = new MessageDeliveryQueue(this.peers.size());
		setupLinks();
		this.loopFlag = true;
		this.clock = new TimeStamp(0, this.pid);
		// Grace setup period
		// kick off main loop (in different thread?)
	}
	
	private void send() throws RemoteException{
		this.clock = this.clock.inc();
		Message m = new MessageImpl(this.clock);
		System.out.println("Proc " + this.pid + " sending: " + m.toString());
		for(PeerEntry pe: peers){
			pe.p.putMessage(m);
		}
		
	}
	
	private boolean getRandBoolean(){
	    Random random = new Random();
	    return random.nextBoolean();
	}
	
	private int getRandSendInterval(){
		Random random = new Random();
		return random.nextInt(200);
	}
	
	public void mainLoop(int rounds, int todo) throws InterruptedException, RemoteException{
		
		while(true){
			synchronized(this.loopFlag){
				if (!this.loopFlag){
					break;
				}
			}
			updateQueue();
			handleQueue();	
			if(getRandBoolean() && todo-- > 0){
				Thread.sleep(getRandSendInterval());
				send();
			}
			if(rounds-- == 0){
				this.stop();
			}
			Thread.sleep(500);
		}
	}
	
	private void handleQueue(){
		Message m = queue.pop();
		if(m == null) {
			System.out.println("No delivery this round");
			Message m_ack = queue.peekNotAcked();
			if(m_ack != null){
				ackMessage(m_ack);
				queue.setAckedTop();
			}
		} else {
			this.clock = this.clock.sync(m.getTimeStamp());
			System.out.println("[DELIVERY] Proc " + this.pid + " delivery: " + m.toString());
		}
		
	}
	
	private void updateQueue(){
		for (Link l : links.values()){
			Deliverable d;
			while(null != (d = l.popDeliverable()) ){
				if(d.isAck()){
					queue.acknowledge(d.getAck());
				} else if (d.isMessage()){
					queue.insert(d.getMessage());
				}
			}
		}
	}
	
	private void ackMessage(Message m){
		this.clock = this.clock.inc();
		for(PeerEntry pe : peers){
			try {
				System.out.println("Proc " + this.pid + " acking " + m.toString() + " to Proc " + pe.peerId);
				pe.p.putAck(new AckImpl(m.getTimeStamp()));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	private void setupLinks(){
		for(PeerEntry pe: this.peers){
			links.put(pe.peerId, new LinkImpl());
		}
	}

	@Override
	public void stop() throws RemoteException {
		synchronized(this.loopFlag){
			this.loopFlag = false;
		}
		
	}

	public static void main(String[] args) throws InterruptedException, MalformedURLException, RemoteException, NotBoundException{
		// setup registry
		Server server = (Server) java.rmi.Naming.lookup("rmi://localhost:1089/register");				
		ClientImpl c = new ClientImpl();
		
		if(args.length < 2){
			throw new RuntimeException("You need to supply both numMessages and numRounds as arguments");
		}
		
		int messages = Integer.valueOf(args[0]);
		int rounds = Integer.valueOf(args[1]);
		
		
		server.register(c);
		int waitRounds = 20;
		while(!c.loopFlag && waitRounds > 0){
			Thread.sleep(500);
			waitRounds--;
		}
		Thread.sleep(2000); // settling time for other processes
		c.mainLoop(rounds, messages);
	}

}