package ru.dpastukhov.voicerecorder.record

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.dpastukhov.voicerecorder.MainActivity
import ru.dpastukhov.voicerecorder.R
import ru.dpastukhov.voicerecorder.database.RecordDatabase
import ru.dpastukhov.voicerecorder.database.RecordDatabaseDao
import ru.dpastukhov.voicerecorder.database.RecordingItem
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class RecordService : Service() {

    private var mFileName: String? = null
    private var mFilePath: String? = null
    private var mCountRecords: Int? = null

    private var mRecorder: MediaRecorder? = null
    private var mStartingTimeMillis: Long = 0
    private var mElapsedMillis: Long = 0
    private var mIncrementTimerTasks: TimerTask? = null

    private var mDatabase: RecordDatabaseDao? = null

    private val mJob = Job()
    private val mUiScope = CoroutineScope(Dispatchers.Main + mJob)

    private val CHANNEL_ID = "RecordService"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        mDatabase = RecordDatabase.getInstance(application).recordDatabaseDao
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mCountRecords = intent?.extras!!["COUNT"] as Int?

        return START_STICKY
    }

    private fun startRecording() {

        setFileNameAndPath()

        mRecorder = MediaRecorder()
        mRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(mFilePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioEncodingBitRate(192000)
        }

        try {
            mRecorder?.prepare()
            mRecorder?.start()
            mStartingTimeMillis = System.currentTimeMillis()
            startForeground(1, createNotification())
        } catch (e: IOException) {
            Log.e("RecordService", "prepare failed")
        }
    }

    private fun createNotification(): Notification? {
        val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(
            applicationContext,
            getString(R.string.notification_channel_id)
        )
            .setSmallIcon(R.drawable.ic_mic_white_36dp)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_recording))
            .setOngoing(true)

        mBuilder.setContentIntent(
            PendingIntent.getActivities(
                applicationContext,
                0, arrayOf(Intent(applicationContext, MainActivity::class.java)),
                0
            )
        )
        return mBuilder.build()
    }

    private fun setFileNameAndPath() {
        var count = 0
        var f: File
        val dateTime = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(System.currentTimeMillis())

        do {
            mFileName = (getString(R.string.default_file_name)) + "_" + dateTime + count + ".mp4"

            mFilePath = application.getExternalFilesDir(null)?.absolutePath
            mFilePath += "/$mFileName"

            count++

            f = File(mFilePath)
        } while (f.exists() && !f.isDirectory)
    }

    private fun stopRecording() {
        val recordingItem = RecordingItem()

        mRecorder?.stop()
        mElapsedMillis = System.currentTimeMillis() - mStartingTimeMillis
        mRecorder?.release()

        Toast.makeText(this, getString(R.string.toast_recording_finish), Toast.LENGTH_SHORT).show()

        recordingItem.apply {
            name = mFileName.toString()
            filePath = mFilePath.toString()
            length = mElapsedMillis
            time = System.currentTimeMillis()
        }

        mRecorder = null

        try {
            mUiScope.launch {
                mDatabase?.insert(recordingItem)
            }
        } catch (e: Exception){
            Log.e("RecordService", "exception", e)
        }
    }

    override fun onDestroy() {
        if (mRecorder != null){
            stopRecording()
        }
        super.onDestroy()
    }

}