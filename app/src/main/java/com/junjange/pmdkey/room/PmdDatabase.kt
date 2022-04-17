package com.junjange.pmdkey.room


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.Executors




@Database(entities = [PmdEntity::class], version = 7, exportSchema = false)
abstract class PmdDatabase : RoomDatabase(){
    abstract fun pmdDao(): PmdDao
    // 데이터 베이스 인스턴스를 싱글톤으로 사용하기 위해 companion object 사용
    companion object {
        @Volatile
        private var INSTANCE: PmdDatabase? = null
        fun getDatabase(
            context: Context,
            scope: CoroutineScope,
        ): PmdDatabase {
            // Room 인스턴스 생성
            // 데이터 베이스가 갱신될 때 기존의 테이블을 버리고 새로 사용하도록 설정
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PmdDatabase::class.java,
                    "pmd"
                )   .fallbackToDestructiveMigration()
                    .addCallback(dbCallback)
                    .build()
                INSTANCE = instance
                // 만들어지는 DB 인스턴스는 Repository 에서 호출되어 사용
                // return instance
                instance
            }

        }
        // DB 초기 값
        private var dbCallback: RoomDatabase.Callback = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Executors.newSingleThreadExecutor()
                    .execute { db.execSQL("insert into pmd values (0, '37.5642135','127.0016985')")}
            }

        }
    }

}

