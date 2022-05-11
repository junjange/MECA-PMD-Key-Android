package com.junjange.pmdkey.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil.setContentView
import com.junjange.pmdkey.databinding.ActivityMainBinding
import android.widget.Toast

import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.location.Location
import android.location.LocationManager
import android.media.tv.TvContract.Programs.Genres.encode
import android.net.Uri
import android.net.Uri.encode
import android.os.Build
import android.os.Looper
import android.util.Base64.encode
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.google.android.gms.common.util.Base64Utils.encode
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.junjange.pmdkey.R
import com.junjange.pmdkey.adapter.WeatherAdapter
import com.junjange.pmdkey.component.Common
import com.junjange.pmdkey.data.ITEM
import com.junjange.pmdkey.data.ModelWeather
import com.junjange.pmdkey.data.WEATHER
import com.junjange.pmdkey.network.WeatherObject
import retrofit2.Call
import retrofit2.Response
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.util.Base64
import androidx.activity.viewModels
import com.junjange.pmdkey.databinding.ActivityMapBinding
import com.junjange.pmdkey.room.PmdEntity
import com.kakao.sdk.navi.NaviClient
import com.kakao.sdk.navi.model.CoordType
import com.kakao.sdk.navi.model.NaviOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView

class MainActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1
    var pairedDevices: Set<BluetoothDevice>? = null
    var btArrayAdapter: ArrayAdapter<String?>? = null
    var deviceAddressArray: ArrayList<String>? = null
    var myPmd = listOf<PmdEntity>() // 전화번호부


    private var baseDate = "20210510"  // 발표 일자
    private var baseTime = "1400"      // 발표 시각
    private var curPoint : Point? = null    // 현재 위치의 격자 좌표를 저장할 포인트


    lateinit var binding: ActivityMainBinding
    var bluetoothAdapter: BluetoothAdapter? = null
    private val viewModel: PmdViewModel by viewModels()
    @SuppressLint("SetTextI18n", "MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = setContentView(this, R.layout.activity_main)
        binding.mainActivity = this


        // Get permission
        val permissionList = arrayOf<String>(
            // 위치 권한
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            // 블루투스 권한
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN
        )

        // 권한 요청
        ActivityCompat.requestPermissions(this@MainActivity, permissionList, 1)

        // 튜토리얼 최초 실행 여부를 판단 ->>>
        val pref = getSharedPreferences("checkFirst", MODE_PRIVATE)
        val checkFirst = pref.getBoolean("checkFirst", false)

        // false일 경우 최초 실행
        if (!checkFirst) {
            // 앱 최초 실행시 하고 싶은 작업
            val editor = pref.edit()
            editor.putBoolean("checkFirst", true)
            editor.apply()
            finish()
            val intent = Intent(this@MainActivity, TutorialActivity::class.java)
            startActivity(intent)
        }



        // Enable bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // 블루투스가 활성화 되어 있는지에 따라 toggle button 상태 변경
        // PMD 와 연동이 되면 이미지의 색이 들어오게끔 구현
        if(bluetoothAdapter!=null){

            if (bluetoothAdapter?.isEnabled == false){
                binding.bleOnOffBtn.isChecked = false
                binding.buggyImage.setImageResource(R.drawable.mobility_0)

            }else{

                binding.bleOnOffBtn.isChecked = true
                binding.buggyImage.setImageResource(R.drawable.mobility_1)
            }


        }

        // 페이링 된 디바이스 목록에 표시
        btArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceAddressArray = ArrayList()
        binding.listview.adapter = btArrayAdapter


//        binding.listview.setOnItemClickListener(myOnItemClickListener())


        // 오늘 날짜 텍스트뷰 설정
        binding.tvDate.text = SimpleDateFormat("MM월 dd일", Locale.getDefault()).format(Calendar.getInstance().time) + "날씨"


        // <새로고침> 버튼 누를 때 위치 정보 & 날씨 정보 다시 가져오기
        binding.btnRefresh.setOnClickListener {
            requestLocation()
        }

        CoroutineScope(Dispatchers.Main).launch{
            viewModel.getAll().observe(this@MainActivity, { pmd ->
                myPmd = pmd
            })

            // 내 위치 위경도 가져와서 날씨 정보 설정하기
            requestLocation()

        }


    }

    fun onClickButtonMap(view: View){
        startActivity(Intent(this@MainActivity,MapActivity::class.java))

    }

    fun onClickButtonTutorial(view: View){
        startActivity(Intent(this@MainActivity,TutorialActivity::class.java))

    }

    // 불루투스 활성화 토글 버튼
    @SuppressLint("MissingPermission")
    fun onClickButtonBluetoothOnOff(view : View){
        if (bluetoothAdapter == null) {
            Toast.makeText(applicationContext, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show()
            // Device doesn't support Bluetooth
            Log.d("bluetoothAdapter","Device doesn't support Bluetooth")
        }else{
            if (bluetoothAdapter?.isEnabled == false) { // 블루투스 꺼져 있으면 블루투스 활성화
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                binding.buggyImage.setImageResource(R.drawable.mobility_1)

            } else{ // 블루투스 켜져있으면 블루투스 비활성화
                bluetoothAdapter?.disable()
                binding.buggyImage.setImageResource(R.drawable.mobility_0)

                requestLocation()
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.update(myPmd[0])
                }



            }
        }
    }


    // 페어링 된 디바이스 목록 불러오기
    @SuppressLint("MissingPermission")
    fun onClickButtonPaired(view : View){
        btArrayAdapter?.clear()

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter ?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            btArrayAdapter?.add(deviceName)
            deviceAddressArray?.add(deviceHardwareAddress)
        }

    }

    // 주변 기기 검색하기
    @SuppressLint("MissingPermission")
    fun onClickButtonSearch(view : View){

        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        } else {

            // 블루투스 켜져있는지 확인
            if (bluetoothAdapter?.isEnabled == true) {
                bluetoothAdapter?.startDiscovery()
                btArrayAdapter?.clear()
                if (deviceAddressArray != null && deviceAddressArray?.isNotEmpty() == true) {
                    deviceAddressArray?.clear()
                }

                // Register for broadcasts when a device is discovered.
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(receiver, filter)

                // 블루투스가 켜지 있지 않다면
            } else {
                Toast.makeText(applicationContext, "bluetooth not on", Toast.LENGTH_SHORT).show()
            }
        }


    }


    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // 디스커버리에서 장치를 찾았습니다.
                    // Intent 에서 BluetoothDevice 개체 및 해당 정보를 가져옵니다.
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device!!.name
                    val deviceHardwareAddress = device.address // MAC address
                    if (deviceName != null){
                        btArrayAdapter!!.add(deviceName)
                        deviceAddressArray!!.add(deviceHardwareAddress)
                        btArrayAdapter!!.notifyDataSetChanged()

                    }

                }
            }

        }
    }

    // 날씨 가져와서 설정하기
    private fun setWeather(nx : Int, ny : Int) {
        // 준비 단계 : base_date(발표 일자), base_time(발표 시각)
        // 현재 날짜, 시간 정보 가져오기
        val cal = Calendar.getInstance()
        baseDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time) // 현재 날짜
        val timeH = SimpleDateFormat("HH", Locale.getDefault()).format(cal.time) // 현재 시각
        val timeM = SimpleDateFormat("HH", Locale.getDefault()).format(cal.time) // 현재 분
        // API 가져오기 적당하게 변환
        baseTime = Common().getBaseTime(timeH, timeM)
        // 현재 시각이 00시이고 45분 이하여서 baseTime이 2330이면 어제 정보 받아오기
        if (timeH == "00" && baseTime == "2330") {
            cal.add(Calendar.DATE, -1).toString()
            baseDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(cal.time)
        }

        // 날씨 정보 가져오기
        // (한 페이지 결과 수 = 60, 페이지 번호 = 1, 응답 자료 형식-"JSON", 발표 날싸, 발표 시각, 예보지점 좌표)
        val call = WeatherObject.getRetrofitService().getWeather(60, 1, "JSON", baseDate, baseTime, nx, ny)

        // 비동기적으로 실행하기
        call.enqueue(object : retrofit2.Callback<WEATHER> {
            // 응답 성공 시
            override fun onResponse(call: Call<WEATHER>, response: Response<WEATHER>) {
                if (response.isSuccessful) {
                    Log.d("ttt", response.body().toString())
                    // 날씨 정보 가져오기
                    val it: List<ITEM> = response.body()!!.response.body.items.item

                    // 현재 시각부터 1시간 뒤의 날씨 6개를 담을 배열
                    val weatherArr = arrayOf(ModelWeather(), ModelWeather(), ModelWeather(), ModelWeather(), ModelWeather(), ModelWeather())

                    // 배열 채우기
                    var index = 0
                    val totalCount = response.body()!!.response.body.totalCount - 1
                    for (i in 0..totalCount) {
                        index %= 6
                        when(it[i].category) {
                            "PTY" -> weatherArr[index].rainType = it[i].fcstValue     // 강수 형태
                            "REH" -> weatherArr[index].humidity = it[i].fcstValue     // 습도
                            "SKY" -> weatherArr[index].sky = it[i].fcstValue          // 하늘 상태
                            "T1H" -> weatherArr[index].temp = it[i].fcstValue         // 기온
                            else -> continue
                        }
                        index++
                    }

                    weatherArr[0].fcstTime = "지금"
                    // 각 날짜 배열 시간 설정
                    for (i in 1..5) weatherArr[i].fcstTime = it[i].fcstTime

                    // 리사이클러 뷰에 데이터 연결
                    binding.weatherRecyclerView.adapter = WeatherAdapter(weatherArr)

                    // 토스트 띄우기
//                    Toast.makeText(applicationContext, it[0].fcstDate + ", " + it[0].fcstTime + "의 날씨 정보입니다.", Toast.LENGTH_SHORT).show()
                }
            }

            // 응답 실패 시
            override fun onFailure(call: Call<WEATHER>, t: Throwable) {
                val tvError = findViewById<TextView>(R.id.tvError)
                tvError.text = "api fail : " +  t.message.toString() + "\n 다시 시도해주세요."
                tvError.visibility = View.VISIBLE
                Log.d("api fail", t.message.toString())
            }
        })
    }

    // 내 현재 위치의 위경도를 격자 좌표로 변환하여 해당 위치의 날씨정보 설정하기
    @SuppressLint("MissingPermission")
    private fun requestLocation() {
        val locationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)

        try {
            // 나의 현재 위치 요청
            val locationRequest = LocationRequest.create()
//            locationRequest.run {
//                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//                interval = 60 * 1000    // 요청 간격(1초)
//            }
            val locationCallback = object : LocationCallback() {
                // 요청 결과
                override fun onLocationResult(p0: LocationResult?) {
                    p0?.let {
                        for (location in it.locations) {
                            myPmd[0].myPmdLocationX = location.latitude.toString()
                            myPmd[0].myPmdLocationY = location.longitude.toString()


                            // 현재 위치의 위경도를 격자 좌표로 변환
                            curPoint = Common().dfsXyConv(location.latitude, location.longitude)

                            // 오늘 날짜 텍스트뷰 설정
                            binding.tvDate.text = SimpleDateFormat("MM월 dd일", Locale.getDefault()).format(Calendar.getInstance().time) + " 날씨"
                            // nx, ny지점의 날씨 가져와서 설정하기
                            setWeather(curPoint!!.x, curPoint!!.y)
                        }
                    }
                }
            }

            // 내 위치 실시간으로 감지
            locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())


        } catch (e : SecurityException) {
            e.printStackTrace()
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        // Don't forget to unregister the ACTION_FOUND receiver.
//        unregisterReceiver(receiver)
    }

    // 선택한 기기와 블루투스 연결하기
    fun onClickButtonSend(view: View){

    }

}