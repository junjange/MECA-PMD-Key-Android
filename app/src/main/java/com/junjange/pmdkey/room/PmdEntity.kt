package com.junjange.pmdkey.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pmd")
data class PmdEntity(
    @PrimaryKey(autoGenerate = true)// PrimaryKey 를 자동적으로 생성
    var id : Int,
    var myPmdLocationX: String,
    var myPmdLocationY : String

)
