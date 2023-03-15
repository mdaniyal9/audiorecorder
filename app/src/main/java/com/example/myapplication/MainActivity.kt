package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {

    private val TAG: String = "MainActivity"

    private lateinit var audioManager: AudioManager

    private var isRecording = false
    private var record: AudioRecord? = null
    private var track: AudioTrack? = null

    private val rcAllPermission = 9005
    private val mPermissionsNotGranted: MutableList<String> = java.util.ArrayList()

    private lateinit var btn: Button
    private var isSpeakerOn = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn = findViewById(R.id.btnSwitch)
        btn.setOnClickListener {
            switchSpeakerState()
        }
        allPermissionGranted()
        startHardwareTest()
        switchSpeakerState()
    }

    private fun startHardwareTest(){

        audioManager = applicationContext.getSystemService(AUDIO_SERVICE) as AudioManager
        initRecordAndTrack()

        audioManager.isSpeakerphoneOn = true

        object : Thread() {
            override fun run() {
                recordAndPlay()
            }
        }.start()
        startRecordAndPlay()
    }

    private fun initRecordAndTrack() {
        val min = AudioRecord.getMinBufferSize(
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC),
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC),
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
            min
        )
        if (AcousticEchoCanceler.isAvailable()) {
            val echoCancler = AcousticEchoCanceler.create(record!!.audioSessionId)
            echoCancler.enabled = true
        }

        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC))
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(min)
            .build()
    //        track = AudioTrack(
//            AudioManager.STREAM_MUSIC,
//            8000,
//            AudioFormat.CHANNEL_IN_STEREO,
//            AudioFormat.ENCODING_PCM_16BIT,
//            min,
//            AudioTrack.MODE_STREAM
//        )
//        track!!.playbackRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)

    }

    private fun recordAndPlay() {
        val min = AudioRecord.getMinBufferSize(
            AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC),
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val lin = ShortArray(min)
//        var num: Int? = 0
        audioManager.mode = AudioManager.MODE_NORMAL
        while (true) {
            if (isRecording) {
//                num = (record?.read(lin, 0, lin.size) ?: track?.write(lin, 0, num!!))
                record?.read(lin, 0, lin.size)

                for (i in lin.indices) {
                    lin[i] = (lin[i].toInt() * 1).coerceAtMost(Int.MAX_VALUE).toShort()
                }
                track?.write(lin, 0, lin.size)
            }
        }
    }

    private fun switchSpeakerState() {
        if (isSpeakerOn) {
            isSpeakerOn = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                setCommunicationDevice(this@MainActivity, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
            } else {
                audioManager.isSpeakerphoneOn = false
            }
        } else {
            isSpeakerOn = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                audioManager.mode = AudioManager.MODE_NORMAL
                setCommunicationDevice(this@MainActivity, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
            } else {
                audioManager.isSpeakerphoneOn = true
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun setCommunicationDevice(context: Context, targetDeviceType: Int) {
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        val devices = audioManager.availableCommunicationDevices
        for (device in devices) {
            if (device.type == targetDeviceType) {
                val result = audioManager.setCommunicationDevice(device)
                Log.d(TAG, result.toString())
            }
        }
    }

    private fun startRecordAndPlay() {
        record?.startRecording()
        track?.play()
        isRecording = true
    }

    private fun stopRecordAndPlay() {
        record?.stop()
        track?.pause()
        isRecording = false
    }

    private fun allPermissionGranted(): Boolean
    {
        mPermissionsNotGranted.clear()
        micCheckGranted()

        if(mPermissionsNotGranted.size == 0)
        {
            return true
        }

        ActivityCompat.requestPermissions(
            this,
            mPermissionsNotGranted.toTypedArray(), rcAllPermission)

        return false
    }

    private fun micCheckGranted()
    {
        val res = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (res == PackageManager.PERMISSION_GRANTED)
            return

        mPermissionsNotGranted.add(Manifest.permission.RECORD_AUDIO)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e(TAG, "onRequestPermissionsResult: ${permissions.contentToString()}++++++++++++${grantResults.contentToString()}")

        var deniedAny = false

        if(requestCode == rcAllPermission)
        {
//            val j = 0
            for(i in grantResults)
            {
                Log.e(TAG, "onRequestPermissionsResult: $i")

                if(i != PackageManager.PERMISSION_GRANTED)
                    deniedAny = true
            }

            if(deniedAny)
            {
                Log.e(TAG, "onRequestPermissionsResult: Permission Denied")
            }
        }
    }

    override fun onDestroy() {
        stopRecordAndPlay()
        super.onDestroy()
    }

}