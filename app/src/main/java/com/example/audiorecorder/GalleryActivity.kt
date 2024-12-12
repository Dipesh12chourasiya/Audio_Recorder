package com.example.audiorecorder

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GalleryActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var records: ArrayList<AudioRecord>
    private lateinit var myAdapter: Adapter
    private lateinit var db: AppDatabase
    private lateinit var recyclerview: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        records = ArrayList()

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        myAdapter = Adapter(records, this)

        recyclerview = findViewById(R.id.recyclerview)

        recyclerview.apply {
            adapter = myAdapter
            layoutManager = LinearLayoutManager(context)
        }

        fetchAll()
    }

    private fun fetchAll(){
        GlobalScope.launch {
            records.clear()
            var queryResult = db.audioRecordDao().getAll()
            records.addAll(queryResult)

            myAdapter.notifyDataSetChanged()
        }
    }

    override fun onItemClickListener(position: Int) {
        var audioRecord = records[position]
        var intent = Intent(this, AudioPlayerActivity::class.java)

        intent.putExtra("filepath",audioRecord.filePath)
        intent.putExtra("finename",audioRecord.filename)
        startActivity(intent)
    }

    override fun onItemLongClickListener(position: Int) {
        Toast.makeText(this, "Long Click", Toast.LENGTH_SHORT).show()
    }
}