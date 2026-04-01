package com.example.zhimai

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.zhimai.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全沉浸式透明状态栏
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 执行入场动画：Header 下落，卡片浮现
        setupAnimations()

        // 登录逻辑
        binding.btnLogin.setOnClickListener { view ->
            // 点击反馈：微弱缩放
            view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()

                val user = binding.etUsername.text.toString()
                val pass = binding.etPassword.text.toString()

                if (user.isNotEmpty() && pass.isNotEmpty()) {
                    Toast.makeText(this, "智脉 AI 已就绪...", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                } else {
                    Toast.makeText(this, "请输入完整信息", Toast.LENGTH_SHORT).show()
                }
            }.start()
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        }
    }

    private fun setupAnimations() {
        // 1. 元素初始位置
        binding.llHeader.alpha = 0f
        binding.llHeader.translationY = -120f
        binding.loginCard.alpha = 0f
        binding.loginCard.translationY = 180f

        // 2. 入场序列
        binding.llHeader.animate().alpha(1f).translationY(0f).setDuration(800).start()
        binding.loginCard.animate().alpha(1f).translationY(0f)
            .setDuration(1000).setStartDelay(200).setInterpolator(DecelerateInterpolator()).start()

        // 3. Logo 呼吸动画
        ObjectAnimator.ofFloat(binding.tvLogo, "translationY", -20f, 20f).apply {
            duration = 3000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}