package com.junjange.pmdkey.room


import android.telecom.Call
import android.util.Log
import androidx.lifecycle.LiveData

class PmdRepository(callDatabase: PmdDatabase) {

    private val pmdDao = callDatabase.pmdDao()
    val myPmdLocation: LiveData<List<PmdEntity>> = pmdDao.getAll()
    companion object {
        private var sInstance: PmdRepository? = null
        fun getInstance(database: PmdDatabase): PmdRepository {
            return sInstance
                ?: synchronized(this) {
                    val instance = PmdRepository(database)
                    sInstance = instance
                    instance
                }
        }
    }


    fun insert(pmdEntity: PmdEntity) {
        pmdDao.insert(pmdEntity)
    }

    fun update(pmdEntity: PmdEntity){
        pmdDao.update(pmdEntity)
    }

    fun delete(pmdEntity: PmdEntity) {
        pmdDao.delete(pmdEntity)
    }

    fun deleteAll(){
        pmdDao.deleteAll()
    }

}