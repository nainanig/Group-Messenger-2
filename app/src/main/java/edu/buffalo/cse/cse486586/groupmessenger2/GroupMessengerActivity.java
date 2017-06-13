package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String KEY_FIELD = "key";
    static final String VALUE_FIELD = "value";

    static final int SERVER_PORT = 10000;
    static final String REMOTE_PORT[] = {"11108", "11112", "11116", "11120", "11124"};
    int sequence_counter = -1;
    float[] local_priority = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
    String msg_id;
    int count = 0;
    float local_seq;
    int c = 0;

    PriorityBlockingQueue<Message> mqueue;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        mqueue = new PriorityBlockingQueue<Message>(11, new Comparator<Message>() {
            @Override
            public int compare(Message lhs, Message rhs) {

                Float s1 = lhs.sequence;
                Float s2 = rhs.sequence;
                return s1.compareTo(s2);
            }
        });

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            e.printStackTrace();
            return;
        }


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button button = (Button) findViewById(R.id.button4);

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                TextView editText = (TextView) findViewById(R.id.textView1);
                editText.append("\t" + msg);
                msg_id = (Integer.parseInt(getMyPort()) / 2) + ":" + (count++);
                Log.d(TAG, "Sender is" + getMyPort());
                Log.d(TAG, "Msg id is" + msg_id);


                String msgToSend = msg_id + "~" + msg + "~" + "N" + "~" + getMyPort() + "~" + "" + "~" + false + "~" + 0.0f;
                Log.d(TAG, "Sending by" + getMyPort());
                Log.d(TAG, "Message sent to client " + msgToSend);

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
            }
        });

    }

    String getMyPort() {
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        return myPort;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, Void, Void> {

        @Override

        protected Void doInBackground(ServerSocket... sockets) {
            while (true) {
                ServerSocket serverSocket = sockets[0];


                try {

                    Socket socket = serverSocket.accept();
                    Log.d(TAG, "server socket accepted");
                    DataInputStream in_server = new DataInputStream(socket.getInputStream());
                    DataOutputStream output;
                    String from_client = in_server.readUTF();
                    String[] msg2 = from_client.split("~");
                    Log.d(TAG, "message from client");

                    Message m_server = new Message(msg2[0], msg2[1], msg2[2], msg2[3], msg2[4], Boolean.parseBoolean(msg2[5]), Float.parseFloat(msg2[6]));

                    if (m_server.type.equals("N")) {
                        if (getMyPort().equals(REMOTE_PORT[0])) local_seq = (++local_priority[0]);
                        else if (getMyPort().equals(REMOTE_PORT[1]))
                            local_seq = (++local_priority[1]);
                        else if (getMyPort().equals(REMOTE_PORT[2]))
                            local_seq = (++local_priority[2]);
                        else if (getMyPort().equals(REMOTE_PORT[3]))
                            local_seq = (++local_priority[3]);
                        else if (getMyPort().equals(REMOTE_PORT[4]))
                            local_seq = (++local_priority[4]);
                        msg2[6] = String.valueOf(local_seq);
                        msg2[4] = getMyPort();
                        msg2[2] = "P";
                        m_server.sequence = local_seq;
                        m_server.type = "P";
                        m_server.receiver = getMyPort();
                        String on_proposed = msg2[0] + "~" + msg2[1] + "~" + msg2[2] + "~" + msg2[3] + "~" + msg2[4] + "~" + msg2[5] + "~" + msg2[6];
                        mqueue.add(m_server);
                        output = new DataOutputStream(socket.getOutputStream());
                        Log.d(TAG, "Sending on proposal" + on_proposed);
                        output.writeUTF(on_proposed);
                        in_server.close();
                    }


                    if (m_server.type.equals("A")) {
                        output = new DataOutputStream(socket.getOutputStream());
                        output.writeUTF("Acknowledgement");


                        if (local_seq < m_server.sequence) {
                            local_seq = m_server.sequence;
                        }
                        mqueue.add(m_server);

                        Message m_q;
                        if (mqueue.size() == 50) {
                            while (mqueue.size() > 0) {
                                m_q = mqueue.poll();
                                if (m_q.status == true) {

                                    ContentValues keyValueToInsert = new ContentValues();
                                    keyValueToInsert.put(KEY_FIELD, ++sequence_counter);
                                    keyValueToInsert.put(VALUE_FIELD, m_q.msg);
                                    getContentResolver().insert(GroupMessengerProvider.URI, keyValueToInsert);

                                }
                            }
                        }


                        System.out.flush();
                    }
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Emulator stopped" + e);
                }

            }
        }


        protected void onProgressUpdate(String... strings) {

            Log.d(TAG, "in onProgressUpdate" + (c++));

            String strReceived = strings[0].trim();
            TextView test = (TextView) findViewById(R.id.textView1);
            test.append(strReceived + "\t\n");


            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put(KEY_FIELD, ++sequence_counter);
            keyValueToInsert.put(VALUE_FIELD, strReceived);
            Log.d("InsertingTest", Integer.toString(sequence_counter));
            getContentResolver().insert(GroupMessengerProvider.URI, keyValueToInsert);
            System.out.print(GroupMessengerProvider.URI);


        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            //try {
            Log.d(TAG, "Entered in client");
            float max_cal = 0.0f;
            int c = 0;
            Message m2 = null;
            String msgToSend = msgs[0];
            for (int i = 0; i < 5; i++) {
                try {
                    Log.d(TAG, "Multicasting to" + REMOTE_PORT[i]);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[i]));
                    DataOutputStream out_client = new DataOutputStream(socket.getOutputStream());
                    out_client.writeUTF(msgToSend);

                    DataInputStream in_client = new DataInputStream(socket.getInputStream());
                    String from_server = in_client.readUTF();
                    String[] msg3 = from_server.split("~");
                    Log.d(TAG, "Message from server" + from_server);
                    m2 = new Message(msg3[0], msg3[1], msg3[2], msg3[3], msg3[4], Boolean.parseBoolean(msg3[5]), Float.parseFloat(msg3[6]));
                    if (msg3[2].equals("P")) {
                        if (max_cal < Float.parseFloat(msg3[6])) {
                            max_cal = Float.parseFloat(msg3[6]);
                        }
                    }
                    c++;
                    if (c == 5) {
                        Log.d(TAG, "Max agreed seq=" + max_cal);
                        m2.sequence = max_cal;
                    }
                    in_client.close();
                    socket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
            Log.d(TAG, "Message" + m2.msg + "with agreed priority" + m2.sequence);
            for (int k = 0; k < 5; k++) {
                try {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(REMOTE_PORT[k]));
                    String agreed_message = m2.msg_id + "~" + m2.msg + "~" + "A" + "~" + m2.sender + "~" + m2.receiver + "~" + String.valueOf(true) + "~" + String.valueOf(m2.sequence);
                    Log.d(TAG, "Agreed msg" + agreed_message);
                    DataOutputStream out_to_server = new DataOutputStream(socket.getOutputStream());
                    out_to_server.writeUTF(agreed_message);
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    String s = in.readUTF();
                    if (s.equals("Acknowledgement")) {
                        System.out.flush();
                        out_to_server.close();
                        socket.close();
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }
}
