package com.tencent.qgame.playerproj

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.ToastUtils
import com.blankj.utilcode.util.UriUtils
import com.tencent.qgame.playerproj.databinding.ActivitySampleAgingBlendBinding

class SampleAgingBlendActivity : AppCompatActivity() {
    lateinit var bind: ActivitySampleAgingBlendBinding

    var grant = true
    var srcImagePath: String = ""

    val permissionResult = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions: MutableMap<String, Boolean> ->
        permissions.forEach {
            grant = grant && it.value
        }
        if (!grant) {
            ToastUtils.showShort("请授权读写权限")
            finish()
        } else {
            bind.btnChooseImage.visibility = View.VISIBLE
        }
    }

    val getContentResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val file = UriUtils.uri2File(uri)
        srcImagePath = file.absolutePath
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivitySampleAgingBlendBinding.inflate(layoutInflater)
        setContentView(bind.root)

        permissionResult.launch(arrayOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ))

        bind.btnChooseImage.setOnClickListener {
            getContentResult.launch("image/*")
        }
    }

    private fun launchAging(srcPath: String) {

    }

}