package com.example.audiorecorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.os.postDelayed
import androidx.room.Room
import com.example.audiorecorder.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.Date

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.onTimerTickListner {

    private lateinit var amplitudes: ArrayList<Float>
    private var permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    private lateinit var binding: ActivityMainBinding

    private lateinit var recorder:MediaRecorder
    private var dirPath = ""
    private var fileName = ""
    private var isRecording = false
    private var isPaused = false

    private var duration = ""

    private lateinit var vibrator: Vibrator

    private lateinit var timer: Timer

    private lateinit var db: AppDatabase

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheet: LinearLayout
    private lateinit var filenameInput: TextInputEditText
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnOk: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED

        if(!permissionGranted)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)

        db = Room.databaseBuilder(
            this, AppDatabase::class.java, "audioRecords"
        ).build()

        // Linear layout view of bottom Sheet
        bottomSheet = findViewById(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        binding.btnRecord.setOnClickListener {
            when{
                isPaused -> resumeRecorder()
                isRecording -> pauseRecorder()
                else -> startRecording()
            }
            vibrator.vibrate(VibrationEffect.createOneShot(50,VibrationEffect.DEFAULT_AMPLITUDE))
        }

        binding.btnList.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        binding.btnDone.setOnClickListener {
            stopRecorder()
            Toast.makeText(this, "Record Saved", Toast.LENGTH_SHORT).show()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            binding.bottomSheetBG.visibility = View.VISIBLE

            filenameInput = findViewById(R.id.filenameInput)
            filenameInput.setText(fileName)
        }

        btnCancel = findViewById(R.id.btnCancel)
        btnOk = findViewById(R.id.btnOk)

        btnCancel.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        btnOk.setOnClickListener {
            // dismiss()
            binding.bottomSheetBG.visibility = View.GONE
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            hideKeyboard(filenameInput)

            save()
        }

        binding.bottomSheetBG.setOnClickListener {
            File("$dirPath$fileName.mp3").delete()
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            stopRecorder()
            File("$dirPath$fileName.mp3").delete()
            Toast.makeText(this, "Record Deleted", Toast.LENGTH_SHORT).show()
        }

        binding.btnDelete.isClickable = false
    }

    private fun save(){
        val newFilename = filenameInput.text.toString()
        if(newFilename != fileName){
            var newFile = File("$dirPath$newFilename.mp3")
            File("$dirPath$fileName.mp3").renameTo(newFile) // copy the content of old file name into new file name and remove old fileName
        }

        var filepath = "$dirPath$newFilename.mp3"
        var timestamp = Date().time
        var ampsPath = "$dirPath$newFilename"

        try{
            var fos = FileOutputStream(ampsPath)
            var out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        } catch (e: IOException){}

        // database entity object
        var record = AudioRecord(newFilename, filepath, timestamp, duration, ampsPath)

        // insert audio in the database using background thread
        GlobalScope.launch {
            db.audioRecordDao().insert(record)
        }
    }

    private fun dismiss(){
        binding.bottomSheetBG.visibility = View.GONE
        hideKeyboard(filenameInput)

        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 100)
    }

    private fun hideKeyboard(view: View){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_CODE)
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED // grantResults[0] returns int
    }

    private fun pauseRecorder(){
        recorder.pause()
        isPaused = true
        binding.btnRecord.setImageResource(R.drawable.ic_record)

        timer.pause()
    }

    private fun resumeRecorder(){
        recorder.resume()
        isPaused = false
        binding.btnRecord.setImageResource(R.drawable.round_pause_24)

        timer.start()
    }

    private fun startRecording(){
        if(!permissionGranted){
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }
        // start recording
        recorder = MediaRecorder()
        dirPath = "${externalCacheDir?.absolutePath}"

        var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())
        fileName = "audio_record_$date"

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$fileName.mp3")

            try{
                prepare()
            } catch (e: IOException){}

            start()
        }

        binding.btnRecord.setImageResource(R.drawable.round_pause_24)
        isRecording = true
        isPaused = false

        timer.start()

        binding.btnDelete.isClickable = true
        binding.btnDelete.setImageResource(R.drawable.round_delete)

        binding.btnList.visibility = View.GONE
        binding.btnDone.visibility = View.VISIBLE
    }

    private fun stopRecorder(){
        timer.stop()

        recorder.apply {
            stop()
            release()
        }

        isPaused = false
        isRecording = false
        binding.btnList.visibility = View.VISIBLE
        binding.btnDone.visibility = View.GONE

        binding.btnDelete.isClickable = false
        binding.btnDelete.setImageResource(R.drawable.round_delete_disabled)

        binding.btnRecord.setImageResource(R.drawable.ic_record)

        binding.tvTimer.text = "00:00.00"
        amplitudes =  binding.waveformView.clear()
    }

    override fun onTimerTic(duration: String) {
        binding.tvTimer.text = duration
        this.duration = duration.dropLast(3)
        binding.waveformView.addAmplitude(recorder.maxAmplitude.toFloat())
    }
}