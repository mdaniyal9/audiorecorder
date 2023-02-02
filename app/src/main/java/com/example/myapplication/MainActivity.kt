package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {

    val TAG: String = "MainActivity"

    lateinit var audioManager: AudioManager

    var isRecording = false
    var record: AudioRecord? = null
    var track: AudioTrack? = null

    val RC_ALL_PERMISSIONS = 9005
    private val mPermissionsNotGranted: MutableList<String> = java.util.ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        allPermissionGranted()
        startHardwareTest()
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
            val echoCancler = AcousticEchoCanceler.create(record!!.getAudioSessionId())
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
        var num: Int? = 0
        audioManager.setMode(AudioManager.MODE_NORMAL)
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
            mPermissionsNotGranted.toTypedArray(), RC_ALL_PERMISSIONS)

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

        if(requestCode == RC_ALL_PERMISSIONS)
        {
            val j = 0
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