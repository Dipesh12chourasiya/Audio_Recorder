package com.example.audiorecorder

import android.media.MediaPlayer
import android.media.ResourceBusyException
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.postDelayed
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.chip.Chip

class AudioPlayerActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var btnPlay: ImageButton
//    private lateinit var btnBackward: ImageButton
//    private lateinit var btnForward: ImageButton
//    private lateinit var speedChip: Chip
    private lateinit var seekBar: SeekBar

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)

        var filePath = intent.getStringExtra("filepath")
        var filename = intent.getStringExtra("filename")

        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }


        btnPlay = findViewById(R.id.btnPlay)
//        btnBackward = findViewById(R.id.btnBackward)
//        btnForward = findViewById(R.id.btnForward)
        //        speedChip = findViewById(R.id.chip)
        seekBar = findViewById(R.id.seekBar)


        handler = Handler(Looper.getMainLooper())
        runnable = Runnable {
            seekBar.progress = mediaPlayer.currentPosition
            handler.postDelayed(runnable,delay)
        }

        btnPlay.setOnClickListener{
            playPausePlayer()
        }

        playPausePlayer()
        seekBar.max = mediaPlayer.duration

        mediaPlayer.setOnCompletionListener {
            btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_play, theme)
            handler.removeCallbacks(runnable)
        }
    }

    private fun playPausePlayer(){
        if(!mediaPlayer.isPlaying){
            mediaPlayer.start()
            btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_pause_circle_24,theme)
            handler.postDelayed(runnable,0)
        } else{
            mediaPlayer.pause()
            btnPlay.background = ResourcesCompat.getDrawable(resources,R.drawable.ic_play, theme)
            handler.removeCallbacks(runnable)
        }
    }
}