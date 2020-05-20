package cn.woochen.functionsdk.util

import android.text.TextUtils
import android.widget.Toast
import cn.woochen.functionsdk.MyApplication


/**
 * info print utils
 *@author woochen
 *@time 2019/3/15 6:50 PM
 */
fun Any.toast( msg: String?) {
    if (TextUtils.isEmpty(msg)) return
    try {
        Toast.makeText(MyApplication.context, "", Toast.LENGTH_SHORT).apply {
            setText(msg)
        }.show()
    } catch (e: Exception) {
    }
}

