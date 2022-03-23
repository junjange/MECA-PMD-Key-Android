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
import android.widget.Toast

import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    var bluetoothAdapter: BluetoothAdapter? = null
    private val REQUEST_ENABLE_BT = 1
    var pairedDevices: Set<BluetoothDevice>? = null
    var btArrayAdapter: ArrayAdapter<String?>? = null
    var deviceAddressArray: ArrayList<String>? = null

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

        // Enable bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // 블루투스가 활성화 되어 있는지에 따라 toggle button 상태 변경
        // PMD 와 연동이 되면 이미지의 색이 들어오게끔 구현
        if(bluetoothAdapter!=null){

            if (bluetoothAdapter?.isEnabled == false){
                binding.bleOnOffBtn.isChecked = false
                binding.buggyImage.setImageResource(R.drawable.buggy_0)

            }else{

                binding.bleOnOffBtn.isChecked = true
                binding.buggyImage.setImageResource(R.drawable.buggy_1)
            }


        }

        // 페이링 된 디바이스 목록에 표시
        btArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        deviceAddressArray = ArrayList()
        binding.listview.adapter = btArrayAdapter

        // 페어링 된 디바이스 목록 불러오기
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter ?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            btArrayAdapter?.add(deviceName)
            deviceAddressArray?.add(deviceHardwareAddress)
        }

//        binding.listview.setOnItemClickListener(myOnItemClickListener())


    }

    // 불루투스 활성화 토글 버튼
    fun onClickButtonBluetoothOnOff(view : View){
        if (bluetoothAdapter == null) {
            Toast.makeText(applicationContext, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show()
            // Device doesn't support Bluetooth
            Log.d("bluetoothAdapter","Device doesn't support Bluetooth")
        }else{
            if (bluetoothAdapter?.isEnabled == false) { // 블루투스 꺼져 있으면 블루투스 활성화
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                binding.buggyImage.setImageResource(R.drawable.buggy_1)

            } else{ // 블루투스 켜져있으면 블루투스 비활성화
                bluetoothAdapter?.disable()
                binding.buggyImage.setImageResource(R.drawable.buggy_0)

            }
        }
    }


    // 페어링 된 디바이스 목록 불러오기
    fun onClickButtonPaired(view : View){
        btArrayAdapter?.clear()

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter ?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            btArrayAdapter?.add(deviceName)
            deviceAddressArray?.add(deviceHardwareAddress)
         }

        Log.d("ddd", btArrayAdapter.toString())
    }

    // 주변 기기 검색하기
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
    override fun onDestroy() {
        super.onDestroy()

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
    }

    // 선택한 기기와 블루투스 연결하기
    fun onClickButtonSend(view: View){

    }
}
