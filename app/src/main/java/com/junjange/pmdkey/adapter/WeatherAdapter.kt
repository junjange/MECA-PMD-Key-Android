package com.junjange.pmdkey.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.junjange.pmdkey.R
import com.junjange.pmdkey.data.ModelWeather


class WeatherAdapter (var items : Array<ModelWeather>) : RecyclerView.Adapter<WeatherAdapter.ViewHolder>() {
    // 뷰 홀더 만들어서 반환, 뷰릐 레이아웃은 list_item_weather.xml
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherAdapter.ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item_weather, parent, false)
        return ViewHolder(itemView)
    }

    // 전달받은 위치의 아이템 연결
    override fun onBindViewHolder(holder: WeatherAdapter.ViewHolder, position: Int) {
        val item = items[position]
        holder.setItem(item)
    }

    // 아이템 갯수 리턴
    override fun getItemCount() = items.count()

    // 뷰 홀더 설정
    inner class ViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("SetTextI18n")
        fun setItem(item : ModelWeather) {
            val imgWeather = itemView.findViewById<ImageView>(R.id.imgWeather)  // 날씨 이미지
            val tvTime = itemView.findViewById<TextView>(R.id.tvTime)           // 시각
            val tvRainType = itemView.findViewById<TextView>(R.id.tvRainType)   // 강수 형태
            val tvHumidity = itemView.findViewById<TextView>(R.id.tvHumidity)   // 습도
            val tvSky = itemView.findViewById<TextView>(R.id.tvSky)             // 하늘 상태
            val tvTemp = itemView.findViewById<TextView>(R.id.tvTemp)           // 온도

            Log.d("ttt", item.fcstTime)
            imgWeather.setImageResource(getRainImage(item.rainType, item.sky))
            tvTime.text = item.fcstTime
            tvRainType.text = getRainType(item.rainType)
            tvHumidity.text = item.humidity
            tvSky.text = getSky(item.sky)
            tvTemp.text = item.temp + "°"
        }
    }

    // 강수 형태
    fun getRainImage(rainType : String, sky: String) : Int {
        return when(rainType) {
            "0" -> getWeatherImage(sky)
            "1" -> R.drawable.rainy
            "2" -> R.drawable.hail
            "3" -> R.drawable.snowy
            else -> R.drawable.ic_launcher_foreground
        }
    }

    fun getWeatherImage(sky : String) : Int {
        // 하늘 상태
        return when(sky) {
            "1" -> R.drawable.sun                       // 맑음
            "3" ->  R.drawable.blur                     // 구름 많음
            "4" -> R.drawable.cloudy                    // 흐림
            else -> R.drawable.ic_launcher_foreground   // 오류
        }
    }

    // 강수 형태
    fun getRainType(rainType : String) : String {
        return when(rainType) {
            "0" -> "없음"
            "1" -> "비"
            "2" -> "비/눈"
            "3" -> "눈"
            else -> "오류 rainType : " + rainType
        }
    }

    // 하늘 상태
    fun getSky(sky : String) : String {
        return when(sky) {
            "1" -> "맑음"
            "3" -> "구름 많음"
            "4" -> "흐림"
            else -> "오류 rainType : " + sky
        }
    }

}