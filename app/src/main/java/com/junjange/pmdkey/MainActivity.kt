package com.junjange.pmdkey

import android.Manifest
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


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val REQUEST_ENABLE_BT = 1
    var pairedDevices: Set<BluetoothDevice>? = null
    var btArrayAdapter: ArrayAdapter<String?>? = null
    var deviceAddressArray: ArrayList<String>? = null

    // BluetoothAdapter를 가져오려면 정적 getDefaultAdapter() 메서드를 호출합니다.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = setContentView(this, R.layout.activity_main)
        binding.mainActivity = this

        // Get permission
        val permissionList = arrayOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        ActivityCompat.requestPermissions(this@MainActivity, permissionList, 1)

        // Enable bluetooth
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // show paired devices
        btArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceAddressArray = ArrayList()
        binding.listview.adapter = btArrayAdapter
        Log.d("Ddd", "ddd")

    }

    // 페어링 된 디바이스 목록 불러오기
    fun onClickButtonPaired(view : View){
        Log.d("Ddd", "ddd")
        btArrayAdapter?.clear()

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter ?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            btArrayAdapter?.add(deviceName)
            deviceAddressArray?.add(deviceHardwareAddress)
         }

        btArrayAdapter?.add("deviceName")
        deviceAddressArray?.add("deviceHardwareAddress")
    }

    // 주변 기기 검색하기
    fun onClickButtonSearch(view : View){


    }

    // 선택한 기기와 블루투스 연결하기
    fun onClickButtonSend(view: View){

    }
}