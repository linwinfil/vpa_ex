/*
 * Tencent is pleased to support the open source community by making vap available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the MIT License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tencent.qgame.playerproj

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.tencent.qgame.playerproj.databinding.ActivityMainBinding
import com.tencent.qgame.playerproj.player.AnimActiveDemoActivity
import com.tencent.qgame.playerproj.player.AnimSimpleDemoActivity
import com.tencent.qgame.playerproj.player.AnimSpecialSizeDemoActivity
import com.tencent.qgame.playerproj.player.AnimVapxDemoActivity


class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)
        bind.btn1.setOnClickListener {
            startActivity(Intent(this, AnimSimpleDemoActivity::class.java))
        }
        bind.btn2.setOnClickListener {
            startActivity(Intent(this, AnimVapxDemoActivity::class.java))
        }
        bind.btn3.setOnClickListener {
            startActivity(Intent(this, AnimActiveDemoActivity::class.java))
        }
        bind.btn4.setOnClickListener {
            startActivity(Intent(this, AnimSpecialSizeDemoActivity::class.java))
        }
        bind.btn5.setOnClickListener {
            startActivity(Intent(this, SampleLottieAnimationActivity::class.java))
        }

        bind.btn1.performClick()
    }


}
