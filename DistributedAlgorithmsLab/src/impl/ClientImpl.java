package impl;

import java.rmi.RemoteException;
import java.util.ArrayList;

import listeners.AckListener;
import listeners.MessageDeliveredListener;
import model.Ack;
import model.Client;
import model.Message;
import model.Peer;

public class ClientImpl implements Client<String>, AckListener, MessageDeliveredListener {
	private ArrayList<Peer> peers;
	private int pid;
	private ArrayList<AckListener> ackListeners;
	private ArrayList<MessageDeliveredListener> messageDeliveredListeners;
	
	public ClientImpl(){
		ackListeners = new ArrayList<AckListener>();
		messageDeliveredListeners = new ArrayList<MessageDeliveredListener>();
	}

	public void activate(ArrayList<ClientImpl> peers, int i) {
		// TODO Auto-generated method stub
		for (ClientImpl c : peers){
			this.peers.add(c);
		}
		this.pid = i;
	}

	@Override
	public void Peer_putMessage(Message<String> m) throws RemoteException {
		for(Peer p : peers){
			// TODO something clock
			// TODO something update clock in message?			
			//p.ackMessage(m);
		}
		// TODO Auto-generated method stub		
	}
	
	
	
	//someone ack'd a message
	@Override
	public void Peer_ackMessage(Ack a) throws RemoteException {
		// TODO Auto-generated method stub
		for(AckListener al: ackListeners){
			al.receiveAck(a);
		}
	}
	
	public static void main(String[] args){
		
	}

	@Override
	public Clock getClock() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addMessageDeliveredListener(MessageDeliveredListener mdl) {
		// TODO Auto-generated method stub
		messageDeliveredListeners.add(mdl);		
	}

	@Override
	public void addAckListener(AckListener al) {
		// TODO Auto-generated method stub
		ackListeners.add(al);
		
	}

	@Override
	public void deliverMessage(Message<String> m) {
		// TODO Auto-generated method stub
		
	}
	
	// DeliveredMessageListener
	@Override
	public void deliveredMessage(Message m) {
		// TODO Auto-generated method stub
		System.out.println("something got delivered in my inbox");
	}

	//Ack Listener
	@Override
	public void receiveAck(Ack a) {
		System.out.println("someone acked something to me");
		
	}

}
