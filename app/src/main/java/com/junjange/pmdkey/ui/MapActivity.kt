package com.junjange.pmdkey.ui

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_UP
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.junjange.pmdkey.BuildConfig
import com.junjange.pmdkey.R
import com.junjange.pmdkey.adapter.CustomBalloonAdapter
import com.junjange.pmdkey.adapter.KakaoLocalAdapter
import com.junjange.pmdkey.adapter.MarkerEventListener
import com.junjange.pmdkey.data.ModelKakaoLocal
import com.junjange.pmdkey.data.ResultSearchKeyword
import com.junjange.pmdkey.databinding.ActivityMapBinding
import com.junjange.pmdkey.network.KakaoLocalInterface
import com.junjange.pmdkey.util.textChangesToFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.coroutines.CoroutineContext


@DelicateCoroutinesApi
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
    val eventListener = MarkerEventListener(this)   // 마커 클릭 이벤트 리스너
    private val viewModel: PmdViewModel by viewModels()
    private var parkingPMDX : Double = 0.0
    private var parkingPMDY : Double = 0.0

    private var myCoroutineJob : Job = Job()
    private val myCoroutineContext: CoroutineContext
        get() = Dispatchers.IO + myCoroutineJob

    // 서치뷰 에딧 텍스트
    private lateinit var mySearchViewEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        val customBalloonAdapter = CustomBalloonAdapter(layoutInflater)
        val view = binding.root
        setContentView(view)



        binding.mapView.setPOIItemEventListener(eventListener)  // 마커 클릭 이벤트 리스너 등록
        binding.mapView.setCalloutBalloonAdapter(customBalloonAdapter)  // 커스텀 말풍선 등록
        addMyPmdMarker()


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
                if (hasFocus) {
                    binding.textClearButton.visibility = View.VISIBLE
                } else {
                    binding.textClearButton.visibility = View.GONE
                }
            }

            // 서치뷰에서 에딧텍스트를 가져온다.
            mySearchViewEditText = binding.etSearchField

            binding.rvList.visibility = View.VISIBLE
            GlobalScope.launch(context = myCoroutineContext){

                // editText 가 변경되었을때
                val editTextFlow = mySearchViewEditText.textChangesToFlow()

                editTextFlow
                    // 연산자들
                    // 입려되고 나서 0.2초 뒤에 받는다
                    .debounce(800)
                    .filter {
                        it?.length!! > 0
                    }
                    .onEach {
                        Log.d(TAG, "flow로 받는다 $it")

                        // 해당 검색어로 api 호출
                        keyword = it.toString()
                        pageNumber = 1
                        searchKeyword(keyword, pageNumber)

//                        searchPhotoApiCall(it.toString())
                    }
                    .launchIn(this)
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

            binding.mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading
        }

        binding.btnMyPmdCurrentPosition.setOnClickListener {
            binding.mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff
            val mapPoint = MapPoint.mapPointWithGeoCoord(parkingPMDX, parkingPMDY)
            binding.mapView.setMapCenterPointAndZoomLevel(mapPoint, 1, true)

        }

        // ClearButton 눌렀을 때 쿼리 Clear
        binding.textClearButton.setOnClickListener {
            binding.etSearchField.text.clear()
            binding.rvList.visibility = View.GONE
        }
    }

    private fun addMyPmdMarker(){
        CoroutineScope(Dispatchers.Main).launch{
            viewModel.getAll().observe(this@MapActivity, { pmd ->
                parkingPMDX = pmd[0].myPmdLocationX.toDouble()
                parkingPMDY = pmd[0].myPmdLocationY.toDouble()


                // 지도에 마커 추가
                val point = MapPOIItem()
                point.apply {
                    itemName = "MY PMD"// 마커 이름
                    mapPoint = MapPoint.mapPointWithGeoCoord( // 좌표
                        parkingPMDX,
                        parkingPMDY
                    )
                    markerType = MapPOIItem.MarkerType.CustomImage // 마커 모양
                    customImageResourceId = R.drawable.pmd_marker  // 커스텀 마커 이미지
                    selectedMarkerType = MapPOIItem.MarkerType.CustomImage // 클릭 시 마커 모양
                    customSelectedImageResourceId = R.drawable.pmd_marker // 클릭 시 커스텀 마커 이미지
                    isCustomImageAutoscale = true      // 커스텀 마커 이미지 크기 자동 조정
                    setCustomImageAnchor(0.5f, 1.0f)    // 마커 이미지 기준점
                }
                binding.mapView.addPOIItem(point)

            })
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
    @SuppressLint("NotifyDataSetChanged")
    fun addItemsAndMarkers(searchResult: ResultSearchKeyword?) {
        if (!searchResult?.documents.isNullOrEmpty()) {
            binding.rvList.visibility = View.VISIBLE
            binding.noResultCard.visibility = View.GONE

            // 검색 결과 있음
            listItems.clear()                   // 리스트 초기화
            binding.mapView.removeAllPOIItems() // 지도의 마커 모두 제거
            addMyPmdMarker() // My PMD 마커 추가

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
                    itemName = document.place_name // 마커 이름
                    mapPoint = MapPoint.mapPointWithGeoCoord( // 좌표
                        document.y.toDouble(),
                        document.x.toDouble()
                    )
                    markerType = MapPOIItem.MarkerType.BluePin // 마커 모양
                    selectedMarkerType = MapPOIItem.MarkerType.RedPin // 클릭 시 마커 모양
                }
                binding.mapView.addPOIItem(point)
            }
            listAdapter.notifyDataSetChanged()



        } else {
            // 검색 결과 없음
            binding.rvList.visibility = View.GONE
            binding.noResultCard.visibility = View.VISIBLE

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
        myCoroutineContext.cancel()  // MemoryLeak 방지를 위해 myCoroutineContext 해제
        super.onDestroy()
    }

}
