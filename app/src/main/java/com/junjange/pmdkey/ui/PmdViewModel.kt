package com.junjange.pmdkey.ui

import com.junjange.pmdkey.room.PmdDatabase
import com.junjange.pmdkey.room.PmdEntity
import com.junjange.pmdkey.room.PmdRepository
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PmdViewModel(application: Application) : AndroidViewModel(application){
    private val pmdRepository: PmdRepository =
        PmdRepository(PmdDatabase.getDatabase(application, viewModelScope))

    var myPmdLocation: LiveData<List<PmdEntity>> = pmdRepository.myPmdLocation

    fun insert(pmdEntity: PmdEntity) = viewModelScope.launch(Dispatchers.IO) {
        pmdRepository.insert(pmdEntity)
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        pmdRepository.deleteAll()
    }

    fun update(pmdEntity: PmdEntity) = viewModelScope.launch(Dispatchers.IO) {
        pmdRepository.update(pmdEntity)
    }


    fun delete(pmdEntity: PmdEntity) = viewModelScope.launch(Dispatchers.IO) {
        pmdRepository.delete(pmdEntity)
    }

    fun getAll(): LiveData<List<PmdEntity>>{
        return myPmdLocation
    }


}