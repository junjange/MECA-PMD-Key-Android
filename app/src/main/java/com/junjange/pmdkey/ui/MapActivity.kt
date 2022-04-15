package com.junjange.pmdkey.ui

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_UP
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.junjange.pmdkey.BuildConfig
import com.junjange.pmdkey.R
import com.junjange.pmdkey.adapter.KakaoLocalAdapter
import com.junjange.pmdkey.data.ModelKakaoLocal
import com.junjange.pmdkey.data.ResultSearchKeyword
import com.junjange.pmdkey.databinding.ActivityMapBinding
import com.junjange.pmdkey.network.KakaoLocalInterface
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import okhttp3.internal.Internal.instance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MapActivity : AppCompatActivity() {

    companion object {
        const val BASE_URL = "https://dapi.kakao.com/"
        const val API_KEY = BuildConfig.KAKAO_MAP_REST_API_KEY  // REST API 키


    }

    private lateinit var binding : ActivityMapBinding
    private val listItems = arrayListOf<ModelKakaoLocal>()   // 리사이클러 뷰 아이템
    private val listAdapter = KakaoLocalAdapter(listItems)    // 리사이클러 뷰 어댑터
    private var pageNumber = 1      // 검색 페이지 번호
    private var keyword = ""        // 검색 키워드

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        // 현재 위치 추적
        binding.mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading


        // 리사이클러 뷰
        binding.rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvList.adapter = listAdapter

        // 리스트 아이템 클릭 시 해당 위치로 이동
        listAdapter.setItemClickListener(object: KakaoLocalAdapter.OnItemClickListener {
            override fun onClick(v: View, position: Int) {
                val mapPoint = MapPoint.mapPointWithGeoCoord(listItems[position].y, listItems[position].x)
                binding.mapView.setMapCenterPointAndZoomLevel(mapPoint, 1, true)
            }
        })

        binding.etSearchField.apply {
            this.hint = "장소/주소를 입력해주세요"

            // EditText 에 포커스가 갔을 때 ClearButton 활성화
            this.setOnFocusChangeListener { v, hasFocus ->
                Log.d("Ttt", hasFocus.toString())
                if (hasFocus) {
                    binding.textClearButton.visibility = View.VISIBLE
                } else {
                    binding.textClearButton.visibility = View.GONE
                }
            }
        }

        binding.etSearchField.setOnKeyListener { _, keyCode, event ->

            if ((event.action== KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {

                binding.rvList.visibility = View.VISIBLE
                keyword = binding.etSearchField.text.toString()
                pageNumber = 1
                searchKeyword(keyword, pageNumber)
                binding.mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff

                // 엔터가 눌릴 때 하고 싶은 일

                true

            } else {

                false

            }
        }
        binding.btnCurrentPosition.setOnClickListener {
            Log.d("ttt"," tttttt")

            binding.mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading
        }

        // ClearButton 눌렀을 때 쿼리 Clear
        binding.textClearButton.setOnClickListener {
            binding.etSearchField.text.clear()
            binding.rvList.visibility = View.GONE
        }

    }




    // 키워드 검색 함수
    private fun searchKeyword(keyword: String, page: Int) {
        val retrofit = Retrofit.Builder()          // Retrofit 구성
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(KakaoLocalInterface::class.java)            // 통신 인터페이스를 객체로 생성
        val call = api.getSearchKeyword(API_KEY, keyword, page)    // 검색 조건 입력

        // API 서버에 요청
        call.enqueue(object: Callback<ResultSearchKeyword> {
            override fun onResponse(call: Call<ResultSearchKeyword>, response: Response<ResultSearchKeyword>) {
                // 통신 성공
                addItemsAndMarkers(response.body())
            }

            override fun onFailure(call: Call<ResultSearchKeyword>, t: Throwable) {
                // 통신 실패
                Log.w("LocalSearch", "통신 실패: ${t.message}")
            }
        })
    }

    // 검색 결과 처리 함수
    fun addItemsAndMarkers(searchResult: ResultSearchKeyword?) {
        if (!searchResult?.documents.isNullOrEmpty()) {

            // 검색 결과 있음
            listItems.clear()                   // 리스트 초기화
            binding.mapView.removeAllPOIItems() // 지도의 마커 모두 제거
            for (document in searchResult!!.documents) {

                // 결과를 리사이클러 뷰에 추가
                val item = ModelKakaoLocal(document.place_name,
                    document.road_address_name,
                    document.address_name,
                    document.x.toDouble(),
                    document.y.toDouble())
                listItems.add(item)

                // 지도에 마커 추가
                val point = MapPOIItem()
                point.apply {
                    itemName = document.place_name
                    mapPoint = MapPoint.mapPointWithGeoCoord(
                        document.y.toDouble(),
                        document.x.toDouble()
                    )
                    markerType = MapPOIItem.MarkerType.BluePin
                    selectedMarkerType = MapPOIItem.MarkerType.RedPin
                }
                binding.mapView.addPOIItem(point)
            }
            listAdapter.notifyDataSetChanged()



        } else {

            // 검색 결과 없음
            Toast.makeText(this, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 키보드 이외의 영역을 터치했을 때, 키보드를 숨기는 동작
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val view = currentFocus
        if (view != null && (ev!!.action === ACTION_UP || MotionEvent.ACTION_MOVE === ev!!.action) &&
            view is EditText && !view.javaClass.name.startsWith("android.webkit.")
        ) {

            binding.noResultCard.visibility = View.GONE

            val scrcoords = IntArray(2)
            view.getLocationOnScreen(scrcoords)
            val x = ev!!.rawX + view.getLeft() - scrcoords[0]
            val y = ev!!.rawY + view.getTop() - scrcoords[1]
            if (x < view.getLeft() || x > view.getRight() || y < view.getTop() || y > view.getBottom()) (this.getSystemService(
                INPUT_METHOD_SERVICE
            ) as InputMethodManager).hideSoftInputFromWindow(
                this.window.decorView.applicationWindowToken, 0
            )
        }

        return super.dispatchTouchEvent(ev)
    }


    override fun onDestroy() {
        listItems.clear()                   // 리스트 초기화
        binding.mapView.removeAllPOIItems() // 지도의 마커 모두 제거
        super.onDestroy()
    }

}