package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by naina on 4/14/17.
 */

public class Message  {
    String msg_id;
    String msg;
    String type;
    String sender;
    String receiver;
    Boolean status;
    float sequence;
    //public Message(){}

    public Message(String msg_id,String msg,String type,String sender,String receiver,Boolean status,float sequence){

        this.msg_id=msg_id;
        this.msg=msg;
        this.type=type;
        this.sender=sender;
        this.receiver=receiver;
        this.status=status;
        this.sequence=sequence;



    }
}
