package com.example.audiorecorder

import android.icu.text.Transliterator.Position

interface OnItemClickListener {
    fun onItemClickListener(position: Int)
    fun onItemLongClickListener(position: Int)
}