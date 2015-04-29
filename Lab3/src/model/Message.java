package model;

import java.io.Serializable;

public interface Message extends Serializable{
	public Integer getLevel();
	public Integer getPId();
	public MessageType getMessageType();
}