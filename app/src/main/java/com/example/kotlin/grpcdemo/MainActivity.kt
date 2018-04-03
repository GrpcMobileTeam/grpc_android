package com.example.kotlin.grpcdemo

import android.app.Activity
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.android.synthetic.main.activity_main.*
import user.UserGrpc
import user.UserOuterClass
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    lateinit var mActivity: MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mActivity = this

        btn_request.setOnClickListener {
            btn_request.isEnabled = false
            tv_result.text = ""
            GrpcTaskUser(this).execute()
        }
    }

    private class GrpcTaskUser(activity: Activity) : AsyncTask<String, Void, String>() {
        private val activityReference: WeakReference<Activity> = WeakReference(activity)
        private var channel: ManagedChannel? = null

        override fun doInBackground(vararg params: String): String {
            try {
                channel = ManagedChannelBuilder.forAddress("172.16.14.52",
                        50051).usePlaintext(true).build()
                val stub = UserGrpc.newBlockingStub(channel)

                val request = UserOuterClass.SmsCodeInput.newBuilder()
                request.phone = ""
                val reply = stub.streamSmsCode(request.build())

                val activity = activityReference.get()
                val resultText = activity!!.findViewById(R.id.tv_result) as TextView

                var resultStr = ""
                reply.forEach {
                    resultStr += it.toString()

                    resultText.post {
                        resultText.append(it.toString())
                    }
                }

                return resultStr
            } catch (e: Exception) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                pw.flush()
                return String.format("Failed... : %n%s", sw)
            }

        }

        override fun onPostExecute(result: String) {
            try {
                channel!!.shutdown().awaitTermination(1, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }

            val activity = activityReference.get() ?: return
            val resultText = activity.findViewById(R.id.tv_result) as TextView
            val sendButton = activity.findViewById(R.id.btn_request) as Button
            resultText.text = result
            sendButton.isEnabled = true
        }
    }


    fun setResult(result: String) {
        val resultText = findViewById<TextView>(R.id.tv_result)
        resultText.text = result
    }
}
