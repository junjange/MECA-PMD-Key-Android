package com.junjange.pmdkey.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PmdDao {
    // 데이터 베이스 불러오기
    @Query("SELECT * from pmd")
    fun getAll(): LiveData<List<PmdEntity>>

    // 데이터 추가
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(pmdEntity: PmdEntity)

    // 데이터 전체 삭제
    @Query("DELETE FROM pmd")
    fun deleteAll()

    // 데이터 업데이트
    @Update
    fun update(pmdEntity: PmdEntity)

    // 데이터 삭제
    @Delete
    fun delete(pmdEntity: PmdEntity)


}