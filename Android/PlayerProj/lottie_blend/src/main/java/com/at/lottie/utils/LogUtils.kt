package com.at.lottie.utils

import android.util.Log
import com.at.lottie.BuildConfig
import com.blankj.utilcode.util.LogUtils

/**
 * Created by linmaoxin on 2021/12/27
 */

var logSwitch = BuildConfig.DEBUG
fun showLog(showLog: () -> Unit) {
    if (logSwitch) showLog()
}

fun loge(tag: String, msg: String?) = showLog { Log.e(tag, msg, null) }
fun loge(tag: String, e: Throwable, msg: String? = "") = showLog { Log.e(tag, msg, e) }
fun logd(tag: String, msg: String) = showLog { Log.d(tag, msg) }
fun logw(tag: String, msg: String) = showLog { Log.w(tag, msg) }
fun logv(tag: String, msg: String) = showLog { Log.v(tag, msg) }
fun logi(tag: String, msg: String) = showLog { Log.i(tag, msg) }