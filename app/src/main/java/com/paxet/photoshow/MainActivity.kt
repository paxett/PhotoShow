package com.paxet.photoshow

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.paxet.photoshow.databinding.ActivityMainBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject


val photoUrls = arrayListOf(
    "https://media-cdn.tripadvisor.com/media/photo-s/06/2b/13/32/bamburgh-castle.jpg",
    "https://media-cdn.tripadvisor.com/media/photo-s/06/35/74/22/bamburgh-castle.jpg",
    "https://media-cdn.tripadvisor.com/media/photo-s/06/2b/13/16/bamburgh-castle.jpg",
    "https://media-cdn.tripadvisor.com/media/photo-s/06/2b/12/f1/bamburgh-castle.jpg",
    "https://media-cdn.tripadvisor.com/media/photo-s/06/2b/12/62/bamburgh-castle.jpg",
    "https://media-cdn.tripadvisor.com/media/photo-s/06/24/e4/e6/general-view.jpg",
    "https://www.topdom.ru/uploaded/article/cons15/125-2.jpg",
    "https://st.depositphotos.com/2239225/5093/i/600/depositphotos_50930617-stock-photo-pelisor-castle-interior.jpg",
    "https://www.dizainvfoto.ru/wp-content/uploads/2018/09/goticheskij-stil.jpg",
    "https://idei.club/uploads/posts/2021-03/1616125596_19-p-srednevekovii-interer-21.jpg"
)

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager : ViewPager2
    private lateinit var recyclerView : RecyclerView
    private lateinit var binding: ActivityMainBinding
    private lateinit var subjectRv: PublishSubject<Int>
    private lateinit var subjectVp: PublishSubject<Int>
    private val TAG = " RxJava_2"
    private var selectedPos  = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewPager = binding.viewPager
        viewPager.adapter = ViewPagerAdapter()
        viewPager.setPageTransformer(ZoomOutPageTransformer())
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                subjectRv.onNext(position)
                super.onPageSelected(position)
            }
        })

        recyclerView = binding.rv
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = RvAdapter()


        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                }

        })

        subjectVp = PublishSubject.create()
        subjectRv = PublishSubject.create()
    }

    override fun onStart() {
        super.onStart()
        subjectRv.subscribe(getRvObserver())
        subjectVp.subscribe(getVpObserver())
    }

    inner class ViewPagerAdapter: RecyclerView.Adapter<ItemHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ItemHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_screen_slide_page, parent, false)
            return ItemHolder(view)
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            Glide.with(holder.itemView)
                .load(photoUrls[position])
                .centerCrop()
                .into(holder.ivPhoto)
        }

        override fun getItemCount(): Int = photoUrls.size
    }

    inner class RvAdapter() : RecyclerView.Adapter<ItemHolder>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ItemHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.recyclerview_item, parent, false
            )
            val holder = ItemHolder(view)
            holder.ivPhoto.setOnClickListener {
                subjectVp.onNext(holder.absoluteAdapterPosition)
            }
            return holder
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            Glide.with(holder.itemView)
                .load(photoUrls[position])
                .centerCrop()
                .into(holder.ivPhoto)
        }

        override fun getItemCount(): Int = photoUrls.size
    }

    inner class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
    }

    private fun getRvObserver(): Observer<Int> {
        return object : Observer<Int> {
            override fun onSubscribe(d: Disposable) {
                Log.d(TAG, "Rv onSubscribe : " + d.isDisposed)
            }

            override fun onNext(value: Int) {
                Log.d(TAG, "Rv onNext value : $value")
                recyclerView.smoothScrollToPosition(value)
                recyclerView.findViewHolderForAdapterPosition(value)?.itemView?.setBackgroundColor(Color.RED)
            }

            override fun onError(e: Throwable) {
                Log.d(TAG, "Rv onError : " + e.message)
            }

            override fun onComplete() {
                Log.d(TAG, "Rv onComplete")
            }
        }
    }

    private fun getVpObserver(): Observer<Int> {
        return object : Observer<Int> {
            override fun onSubscribe(d: Disposable) {
                Log.d(TAG, "Vp onSubscribe : " + d.isDisposed)
            }

            override fun onNext(value: Int) {
                Log.d(TAG, "Vp onNext value : $value")
                viewPager.setCurrentItem(value, true)
            }

            override fun onError(e: Throwable) {
                Log.d(TAG, "Vp onError : " + e.message)
            }

            override fun onComplete() {
                Log.d(TAG, "Vp onComplete")
            }
        }
    }
}

class ZoomOutPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        val pageWidth = view.width
        val pageHeight = view.height
        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.alpha = 0f
        } else if (position <= 1) { // [-1,1]
            // Modify the default slide transition to shrink the page as well
            val scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position))
            val vertMargin = pageHeight * (1 - scaleFactor) / 2
            val horMargin = pageWidth * (1 - scaleFactor) / 2
            if (position < 0) {
                view.translationX = horMargin - vertMargin / 2
            } else {
                view.translationX = -horMargin + vertMargin / 2
            }

            // Scale the page down (between MIN_SCALE and 1)
            view.scaleX = scaleFactor
            view.scaleY = scaleFactor

            // Fade the page relative to its size.
            view.alpha = MIN_ALPHA +
                    (scaleFactor - MIN_SCALE) /
                    (1 - MIN_SCALE) * (1 - MIN_ALPHA)
        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            view.alpha = 0f
        }
    }

    companion object {
        private const val MIN_SCALE = 0.85f
        private const val MIN_ALPHA = 0.5f
    }
}