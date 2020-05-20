package cn.woochen.functionsdk.shake

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.SoundPool
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.TranslateAnimation
import cn.woochen.functionsdk.R
import cn.woochen.functionsdk.util.toast
import kotlinx.android.synthetic.main.activity_wx_shake.*
import java.lang.ref.WeakReference
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 *微信摇一摇
 *@author woochen
 *@time 2020/5/20 5:12 PM
 */
class WxShakeActivity : AppCompatActivity(), SensorEventListener {

    private val TAG = "MainActivity"
    private val START_SHAKE = 0x1
    private val AGAIN_SHAKE = 0x2
    private val END_SHAKE = 0x3

    private val mSensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private var mAccelerometerSensor: Sensor? = null
    private val mVibrator: Vibrator? by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    } //手机震动
    private val mSoundPool by lazy {
        SoundPool(1, AudioManager.STREAM_SYSTEM, 5)
    }//摇一摇音效

    private var mHandler: MyHandler? = null
    private val mWeiChatAudio by lazy {
        mSoundPool.load(this, R.raw.weichat_audio, 1)
    }
    //记录摇动状态
    private var isShake = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wx_shake)
        mHandler = MyHandler(this)
    }

    override fun onStart() {
        super.onStart()
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (mAccelerometerSensor != null) {
            mSensorManager.registerListener(this, mAccelerometerSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onStop(){
        mSensorManager.unregisterListener(this)
        super.onStop()
    }

    /**
     * 开启 摇一摇动画
     *
     * @param isBack 是否是返回初识状态
     */
    private fun startAnimation(isBack: Boolean) {
        //动画坐标移动的位置的类型是相对自己的
        val type = Animation.RELATIVE_TO_SELF
        val topFromY: Float
        val topToY: Float
        val bottomFromY: Float
        val bottomToY: Float
        if (isBack) {
            topFromY = -0.5f
            topToY = 0f
            bottomFromY = 0.5f
            bottomToY = 0f
        } else {
            topFromY = 0f
            topToY = -0.5f
            bottomFromY = 0f
            bottomToY = 0.5f
        }

        //上面图片的动画效果
        val topAnim = TranslateAnimation(type, 0f, type, 0f, type, topFromY, type, topToY)
        topAnim.duration = 200
        //动画终止时停留在最后一帧~不然会回到没有执行之前的状态
        topAnim.fillAfter = true

        //底部的动画效果
        val bottomAnim = TranslateAnimation(type, 0f, type, 0f, type, bottomFromY, type, bottomToY)
        bottomAnim.duration = 200
        bottomAnim.fillAfter = true

        //大家一定不要忘记, 当要回来时, 我们中间的两根线需要GONE掉
        if (isBack) {
            bottomAnim.setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation) {}
                override fun onAnimationRepeat(animation: Animation) {}
                override fun onAnimationEnd(animation: Animation) {
                    //当动画结束后 , 将中间两条线GONE掉, 不让其占位
                    main_shake_top_line.visibility = View.GONE
                    main_shake_bottom_line.visibility = View.GONE
                }
            })
        }
        //设置动画
        main_linear_top.startAnimation(topAnim)
        main_linear_bottom.startAnimation(bottomAnim)
    }


    inner class MyHandler(activity: WxShakeActivity) : Handler() {
        private val mReference: WeakReference<WxShakeActivity>?
        private var mActivity: WxShakeActivity? = null
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
               START_SHAKE -> {
                   mActivity?.let {
                      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                          val effect =  VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                           mActivity?.mVibrator?.vibrate(effect)
                       } else {
                           mActivity?.mVibrator?.vibrate(300)
                       }
                       //发出提示音
                       //mActivity?.mSoundPool?.play(mActivity?.mWeiChatAudio!!, 1f, 1f, 0, 0, 1f)
                       main_shake_top_line.visibility = View.VISIBLE
                       main_shake_bottom_line.visibility = View.VISIBLE
                       mActivity?.startAnimation(false) //参数含义: (不是回来) 也就是说两张图片分散开的动画
                   }
                }
               AGAIN_SHAKE -> {
                   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                       val effect =  VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                       mActivity?.mVibrator?.vibrate(effect)
                   } else {
                       mActivity?.mVibrator?.vibrate(300)
                   }
               }
               END_SHAKE -> {
                   toast("摇一摇成功！")
                    mActivity?.isShake = false
                    mActivity?.startAnimation(true)
                }
            }
        }

        init {
            mReference = WeakReference<WxShakeActivity>(activity)
            mActivity = mReference.get()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        val type = event!!.sensor.type
        if (type == Sensor.TYPE_ACCELEROMETER) {
            //获取三个方向值
            val values = event.values
            val x = values[0]
            val y = values[1]
            val z = values[2]
            if ((abs(x) > 17 || abs(y) > 17 || abs(z) > 17) && !isShake) {
                isShake = true
             thread {
                 try {
                     Log.d(TAG, "onSensorChanged: 摇动")
                     //开始震动 发出提示音 展示动画效果
                     mHandler!!.obtainMessage(START_SHAKE).sendToTarget()
                     Thread.sleep(500)
                     //再来一次震动提示
                     mHandler!!.obtainMessage(AGAIN_SHAKE).sendToTarget()
                     Thread.sleep(500)
                     mHandler!!.obtainMessage(END_SHAKE).sendToTarget()
                 } catch (e: InterruptedException) {
                     e.printStackTrace()
                 }
             }
            }
        }
    }
}