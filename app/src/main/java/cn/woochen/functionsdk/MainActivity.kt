package cn.woochen.functionsdk

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import cn.woochen.functionsdk.adapter.MainAdapter
import cn.woochen.functionsdk.shake.WxShakeActivity
import cn.woochen.functionsdk.x5.X5WebViewActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val mItemNames = mutableListOf("X5内核","摇一摇")
    private val mMainAdapter by lazy {
        MainAdapter(this, mItemNames)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    private fun initView() {
        rv.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rv.adapter = mMainAdapter
        mMainAdapter.itemClickListener = object : MainAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                itemClickEvent(position)
            }
        }

    }


    private fun itemClickEvent(position: Int) {
        when (position) {
            0 -> {
                start(X5WebViewActivity::class.java)
            }
            1 -> {
                start(WxShakeActivity::class.java)
            }
        }
    }


    private fun start(clazz: Class<*>) {
        val intent = Intent(this, clazz)
        startActivity(intent)
    }

}
