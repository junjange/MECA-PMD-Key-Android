package com.junjange.pmdkey.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import com.kakao.sdk.navi.Constants
import com.kakao.sdk.navi.NaviClient
import com.kakao.sdk.navi.model.CoordType
import com.kakao.sdk.navi.model.Location
import com.kakao.sdk.navi.model.NaviOption
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView


// 마커 클릭 이벤트 리스너
class MarkerEventListener(val context: Context): MapView.POIItemEventListener {

    override fun onPOIItemSelected(mapView: MapView?, poiItem: MapPOIItem?) {
        // 마커 클릭 시

    }

    override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?) {
        // 말풍선 클릭 시 (Deprecated)
        // 이 함수도 작동하지만 그냥 아래 있는 함수에 작성하자
    }

    override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?, buttonType: MapPOIItem.CalloutBalloonButtonType?) {

        // 말풍선 클릭 시
        val builder = AlertDialog.Builder(context)
        val itemList = arrayOf("토스트", "길 찾기", "취소")
        builder.setTitle("${poiItem?.itemName}")
        builder.setItems(itemList) { dialog, which ->
            when(which) {
                0 -> Toast.makeText(context, "토스트", Toast.LENGTH_SHORT).show()  // 토스트
                1 -> kakaoNavi(poiItem?.itemName.toString(), poiItem?.mapPoint?.mapPointGeoCoord?.longitude.toString(), poiItem?.mapPoint?.mapPointGeoCoord?.latitude.toString())
                2 -> dialog.dismiss()   // 대화상자 닫기
            }
        }
        builder.show()
    }

    override fun onDraggablePOIItemMoved(mapView: MapView?, poiItem: MapPOIItem?, mapPoint: MapPoint?) {
        // 마커의 속성 중 isDraggable = true 일 때 마커를 이동시켰을 경우
    }

    private fun kakaoNavi(name: String, y : String, x : String){

        // 카카오내비 앱으로 길 안내
        if (NaviClient.instance.isKakaoNaviInstalled(context)) {
            // 카카오내비 앱으로 길 안내 - WGS84
            val intent = Intent(  NaviClient.instance.navigateIntent(
                Location(name, y, x),
                NaviOption(coordType = CoordType.WGS84)

            ))

            startActivity(context, intent,null)

        } else {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(Constants.WEB_NAVI_INSTALL)).
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            // 카카오내비 설치 페이지로 이동
            startActivity(context, intent,null)

        }
    }
}