package com.example.tcpip_client

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.Socket
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    var streamMsg = ""
    var nickName = ""
    lateinit var showText: TextView
    lateinit var inputIP: EditText
    lateinit var inputPort: EditText
    lateinit var inputName: EditText
    lateinit var inputMessage: EditText
    lateinit var btnConnect: Button
    lateinit var btnSend: Button
    lateinit var scrollView: ScrollView

    var socket: Socket = Socket()
    lateinit var client: SocketClient
    lateinit var receive: ReceiveThread
    lateinit var send: SendThread

    var sendStream: PipedInputStream? = null
    var receiveStream: PipedOutputStream? = null

    var msgHandler = Handler(Looper.getMainLooper())
    var threadList: LinkedList<SocketClient> = LinkedList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initUI()

        inputIP.setText("")
        inputPort.setText("5001")

        // 핸들
        msgHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)

                if (msg.what == 1111) {
                    showText.append(msg.obj.toString() + "\n")
                    scrollView.fullScroll(View.FOCUS_DOWN)
                }
            }
        }
    }

    fun initUI() {
        showText = findViewById(R.id.tv_show)
        inputIP = findViewById(R.id.et_ip)
        inputPort = findViewById(R.id.et_port)
        inputName = findViewById(R.id.et_name)
        inputMessage = findViewById(R.id.et_send)

        scrollView = findViewById(R.id.scrollView)

        btnConnect = findViewById(R.id.btn_connect)
        btnSend = findViewById(R.id.btn_send)
        btnConnect.setOnClickListener(this)
        btnSend.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_connect -> {
                nickName = inputName.text.toString()
                client = SocketClient(inputIP.text.toString(), inputPort.text.toString())
                threadList.add(client)
                client.start()
            }
            R.id.btn_send -> {
                if (inputMessage.text.toString() != "") {
                    send = SendThread(socket)
                    send.start()

                    inputMessage.setText("")
                    val imm: InputMethodManager =
                        getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(inputMessage.windowToken, 0);
                }
            }
        }
    }

    inner class SocketClient constructor(ip: String, port: String) : Thread() {
        var threadAlive = false
        var ip = ""
        var port = ""
        var mac = ""

        var outputStream: DataOutputStream? = null
        var bufferedReader: BufferedReader? = null

        init {
            this.threadAlive = true
            this.ip = ip
            this.port = port
        }

        override fun run() {
            try {
                // 연결
                socket = Socket(ip, port.toInt())

                outputStream = DataOutputStream(socket.getOutputStream())
                receive = ReceiveThread(socket)
                receive.start()

//                val manager: WifiManager =
//                    applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
//                var info = manager.connectionInfo
//                mac = info.macAddress

                outputStream!!.writeUTF(nickName)
            } catch (e: IOException) {
            }
        }
    }

    inner class ReceiveThread constructor(socket: Socket) : Thread() {
        var socket: Socket? = null
        var input: DataInputStream? = null

        init {
            this.socket = socket
            try {
                input = DataInputStream(socket.getInputStream())
            } catch (e: IOException) {
            }
        }

        override fun run() {
            try {
                while (input != null) {
                    val msg = input!!.readUTF()
                    if (msg != null) {
                        val hdMsg: Message = msgHandler.obtainMessage()
                        hdMsg.what = 1111
                        hdMsg.obj = msg
                        msgHandler.sendMessage(hdMsg)
                    }
                }
            } catch (e: IOException) {
            }
        }
    }

    inner class SendThread constructor(socket: Socket) : Thread() {
        var socket: Socket? = null
        var sendMsg = inputMessage.text
        var output: DataOutputStream? = null

        init {
            this.socket = socket
            try {
                output = DataOutputStream(socket.getOutputStream())
            } catch (e: IOException) {
            }
        }

        @SuppressLint("HardwareIds")
        override fun run() {
            try {
                if (output != null) {
                    if (sendMsg != null) {
                        output!!.writeUTF("$nickName : $sendMsg")
                    }
                }
            } catch (e: IOException) {
            }
        }
    }
}