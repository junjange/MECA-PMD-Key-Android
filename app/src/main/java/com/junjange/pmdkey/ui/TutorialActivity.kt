package com.junjange.pmdkey.ui

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.junjange.pmdkey.R
import com.junjange.pmdkey.databinding.ActivityTutorialBinding


class TutorialActivity : AppCompatActivity() {
    private var pagerAdapter: PagerAdapter? = null
    private lateinit var dots: Array<TextView?>
    private lateinit var layouts: IntArray
    private lateinit var binding : ActivityTutorialBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_tutorial)
        binding.tutorialActivity = this@TutorialActivity


        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        // 변화될 레이아웃들 주소
        // 원하는 경우 레이아웃을 몇 개 더 추가
        layouts = intArrayOf(
            R.layout.page0,
            R.layout.page1,
            R.layout.page2,
            R.layout.page3,
            R.layout.page4,
            R.layout.page5
        )

        // 하단 점 추가
        addBottomDots(0)

        // 알림 표시줄을 투명하게 만들기
        changeStatusBarColor()
        pagerAdapter = PagerAdapter()
        binding.viewPager.setAdapter(pagerAdapter)
        binding.viewPager.addOnPageChangeListener(viewPagerPageChangeListener)


        // 건너띄기 버튼 클릭시 메인화면으로 이동
        binding.btnSkip.setOnClickListener(View.OnClickListener {
            startActivity(Intent(this@TutorialActivity, MainActivity::class.java))
            finish()
        })

        // 조건문을 통해 버튼 하나로 두개의 상황을 실행
        binding.btnNext.setOnClickListener(View.OnClickListener {
            val current = getItem()
            if (current < layouts.size) {
                // 마지막 페이지가 아니라면 다음 페이지로 이동
                binding.viewPager.setCurrentItem(current)
            } else {
                // 마지막 페이지라면 메인페이지로 이동
                startActivity(Intent(this@TutorialActivity, MainActivity::class.java))
                finish()
            }
        })
    }

    // 하단 점(선택된 점, 선택되지 않은 점) 구현
    private fun addBottomDots(currentPage: Int) {
        dots = arrayOfNulls(layouts.size) // 레이아웃 크기만큼 하단 점 배열에 추가
        val colorsActive = resources.getIntArray(R.array.array_dot_active)
        val colorsInactive = resources.getIntArray(R.array.array_dot_inactive)
        binding.layoutDots.removeAllViews()
        for (i in dots.indices) {
            dots[i] = TextView(this)
            dots[i]?.text = Html.fromHtml("&#8226;")
            dots[i]?.textSize = 35f
            dots[i]?.setTextColor(colorsInactive[currentPage])
            binding.layoutDots.addView(dots[i])
        }
        if (dots.size > 0) dots[currentPage]?.setTextColor(colorsActive[currentPage])
    }

    private fun getItem(): Int {
        return binding.viewPager.currentItem + 1
    }

    // 뷰페이저 변경 리스너
    private var viewPagerPageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
        override fun onPageSelected(position: Int) {
            addBottomDots(position)

            // 다음 / 시작 버튼 바꾸기
            if (position == layouts.size - 1) {
                // 마지막 페이지에서는 다음 버튼을 시작버튼으로 교체
                binding.btnNext.text = getString(R.string.start) // 다음 버튼을 시작버튼으로 글자 교체
                binding.btnSkip.visibility = View.GONE
            } else {

                // 마지막 페이지가 아니라면 다음과 건너띄기 버튼 출력
                binding.btnNext.text  = getString(R.string.next)
                binding.btnSkip.visibility = View.VISIBLE
            }
        }

        override fun onPageScrolled(arg0: Int, arg1: Float, arg2: Int) {}
        override fun onPageScrollStateChanged(arg0: Int) {}
    }

    // 알림 표시줄을 투명하게 만들기
    private fun changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    // 호출기 어댑터
    inner class PagerAdapter : androidx.viewpager.widget.PagerAdapter() {
        private var layoutInflater: LayoutInflater? = null
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            layoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = layoutInflater!!.inflate(layouts[position], container, false)
            container.addView(view)
            return view
        }

        override fun getCount(): Int {
            return layouts.size
        }

        override fun isViewFromObject(view: View, obj: Any): Boolean {
            return view === obj
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val view = `object` as View
            container.removeView(view)
        }
    }
}