package com.tencent.qgame.playerproj

import android.app.Application
import com.at.lottie.utils.logi
import org.opencv.android.Constant
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader

/**
 * Created by linmaoxin on 2022/1/4
 */
class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, object : LoaderCallbackInterface {
            override fun onManagerConnected(status: Int) {
                logi(Constant.TAG, "onManagerConnected, status:${status}")
            }

            override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
                logi(Constant.TAG, "onPackageInstall, operation:$operation")
                callback?.apply {
                    logi(Constant.TAG, "onPackageInstall, ${this.packageName}")
                }
            }
        })
        OpenCVLoader.initDebug(true)
    }
}