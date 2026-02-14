package logic;

import java.io.Serializable;
import java.util.Random;

/**
 * This class is used for a cleaner communication between server and client
 */
public class Message implements Serializable {	
	// Id used to track requests
	private int requestId;

	// The name of the action for identifying what to do with the message.
	private String actionName;
	
	// The object the request holds. May be a Subscriber, Book, etc. Depends on the actionName.
	private Object object;
	
	// Whether this message is an error. object will be a string containing the reason for the error
	private boolean isError;

	public Message() {
		
	}

	public Message(String actionName) {
		this.actionName = actionName;
	}

	public Message(String actionName, Object object) {
		this.actionName = actionName;
		this.object = object;
	}

	public Message(String actionName, Object object, boolean isError) {
		this(actionName, object);
		this.isError = isError;
	}

	/**
	 * Replies to the message by changing the object and isError value
	 * @param object
	 * @param isError
	 * @return Message
	 */
	public Message reply(Object object) {
		this.object = object;
		this.isError = false;
		return this;
	}
	
	public Message errorReply(Object object) {
		this.object = object;
		this.isError = true;
		return this;
	}
	
	public int getRequestId() {
		return requestId;
	}
	
	public void setRequestId(int requestId) {
		this.requestId = requestId;
	}
	
	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public boolean isError() {
		return isError;
	}

	public void setIsError(boolean isError) {
		this.isError = isError;
	}
}
