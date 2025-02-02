package cn.woochen.functionsdk

import android.app.Application
import android.util.Log
import com.tencent.smtt.sdk.QbSdk


class MyApplication:Application() {
    override fun onCreate() {
        super.onCreate()
        initX5()
    }


    private fun initX5() {
        QbSdk.initX5Environment(this,object: QbSdk.PreInitCallback {
            override fun onCoreInitFinished() {
                Log.e("x5log","onCoreInitFinished")
            }

            override fun onViewInitFinished(p0: Boolean) {
                Log.e("x5log","onCoreInitFinished->$p0")
            }
        })
    }
}