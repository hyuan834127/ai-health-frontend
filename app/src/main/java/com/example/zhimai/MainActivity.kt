package com.example.zhimai

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.zhimai.databinding.ActivityMainBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.heytap.databaseengine.HeytapHealthApi
import com.heytap.databaseengine.apiv2.HResponse
import com.heytap.databaseengine.apiv2.auth.AuthResult
import com.heytap.databaseengine.apiv3.DataReadRequest
import com.heytap.databaseengine.apiv3.data.DataSet
import com.heytap.databaseengine.apiv3.data.DataType
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val client = OkHttpClient()
    private val TAG = "ZhiMai_OPPO_SDK"

    // 🚩 演示开关：true = 动态随机完美假数据，false = 连接真实 OPPO 手表
    private val isDemoMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化图表外观、下拉刷新以及主题切换逻辑
        setupEcgChart()
        setupSwipeRefresh()
        setupThemeToggle() // 🚀 新增：主题切换初始化

        // 核心分流
        if (isDemoMode) {
            Log.i(TAG, "📺 当前为演示模式，加载动态模拟数据...")
            loadDemoData()
        } else {
            Log.i(TAG, "⌚ 当前为真实模式，正在唤醒 OPPO SDK...")
            HeytapHealthApi.init(this)
            HeytapHealthApi.setLoggable(true)
            requestOppoHealthAuth()
        }

        // AI 按钮点击事件
        binding.btnSendAi.setOnClickListener {
            binding.tvAiResult.text = "🔄 融合多维健康数据，智脉 AI 引擎正在深度分析..."
            binding.btnSendAi.isEnabled = false
            sendRequestToServer()
        }
    }

    /**
     * 🚀 核心新增：亮暗模式切换逻辑
     */
    private fun setupThemeToggle() {
        // 1. 获取当前系统模式（通过位运算判断是否为深色模式）
        val currentMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNight = currentMode == Configuration.UI_MODE_NIGHT_YES

        // 2. 初始化 Emoji：黑夜模式显示太阳（提示切回白昼），白天模式显示月亮
        // 注意：ViewBinding 会将 btn_theme_toggle 转为 btnThemeToggle
        binding.btnThemeToggle.text = if (isNight) "☀️" else "🌙"

        // 3. 设置点击切换逻辑
        binding.btnThemeToggle.setOnClickListener { view ->
            val isCurrentlyNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

            if (isCurrentlyNight) {
                // 当前是深色模式 -> 切换到浅色模式
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                // 当前是浅色模式 -> 切换到深色模式
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            // 4. 炫酷动画：Emoji 旋转 360 度
            // 旋转动画会让切换过程看起来更像经过了“深度计算”，符合“智脉” App 的调性
            view.animate()
                .rotationBy(360f)
                .setDuration(500)
                .start()
        }
    }
    override fun onResume() {
        super.onResume()
        if (!isDemoMode) {
            readRealHealthData()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            if (isDemoMode) {
                loadDemoData()
            } else {
                readRealHealthData()
            }

            binding.swipeRefreshLayout.postDelayed({
                if (binding.swipeRefreshLayout.isRefreshing) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this, "数据已同步最新状态", Toast.LENGTH_SHORT).show()
                }
            }, 1500)
        }
    }

    private fun loadDemoData() {
        runOnUiThread {
            val randomHeartRate = (68..85).random()
            binding.tvHeartRate.text = "$randomHeartRate bpm"

            val randomOxygen = (97..99).random()
            binding.tvBloodOxygen.text = "$randomOxygen %"

            val randomActivity = (10..15).random()
            binding.tvActivity.text = "$randomActivity %"

            val randomStress = (25..35).random()
            binding.tvStress.text = "$randomStress 极佳"
            binding.tvSleep.text = "8小时10分 (深睡极佳)"

            generateRandomChartData()
        }
    }

    private fun generateRandomChartData() {
        val entries = ArrayList<Entry>()
        for (i in 0 until 15) {
            val valY = (700..850).random() / 10f
            entries.add(Entry(i.toFloat(), valY))
        }

        val dataSet = LineDataSet(entries, "实时心率趋势")
        dataSet.color = Color.parseColor("#E53935")
        dataSet.lineWidth = 3.0f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)

        // 根据当前模式调整填充色透明度，防止太刺眼
        dataSet.fillAlpha = 60
        dataSet.fillColor = Color.parseColor("#E53935")

        binding.ecgChart.data = LineData(dataSet)
        binding.ecgChart.invalidate()
    }

    private fun setupEcgChart() {
        val chart = binding.ecgChart
        chart.setNoDataText("正在初始化健康监测引擎...")

        // 动态适配：根据是否是深色模式调整图表轴线颜色
        val isNight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val axisColor = if (isNight) Color.WHITE else Color.parseColor("#757575")
        val gridColor = if (isNight) Color.parseColor("#33FFFFFF") else Color.parseColor("#E0E0E0")

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.textColor = axisColor

        val leftAxis = chart.axisLeft
        leftAxis.axisMinimum = 50f
        leftAxis.axisMaximum = 110f
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = gridColor
        leftAxis.textColor = axisColor

        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
    }

    // =========================================================================
    // OPPO SDK 真实数据逻辑（保持不变）
    // =========================================================================

    private fun requestOppoHealthAuth() {
        val scopes = listOf("READ_HEART_RATE", "READ_DAILY_ACTIVITY", "READ_BLOOD_OXYGEN", "READ_SLEEP")
        HeytapHealthApi.getInstance().authorityApi().request(this, scopes[0], object : HResponse<AuthResult> {
            override fun onSuccess(result: AuthResult?) {
                readRealHealthData()
            }
            override fun onFailure(code: Int) {
                Log.e(TAG, "❌ OPPO 授权失败：$code")
            }
        })
    }

    private fun readRealHealthData() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000L

        val heartRateRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE)
            .setTimeRange(startTime, endTime)
            .build()

        HeytapHealthApi.getInstance().dataApi().read(heartRateRequest, object : HResponse<List<DataSet>> {
            override fun onSuccess(dataSets: List<DataSet>?) {
                if (!dataSets.isNullOrEmpty()) parseAndUpdateHeartRateUI(dataSets)
            }
            override fun onFailure(code: Int) { Log.e(TAG, "心率数据失败: $code") }
        })

        val stepRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_DAILY_ACTIVITY)
            .setTimeRange(startTime, endTime)
            .build()

        HeytapHealthApi.getInstance().dataApi().read(stepRequest, object : HResponse<List<DataSet>> {
            override fun onSuccess(dataSets: List<DataSet>?) {
                if (!dataSets.isNullOrEmpty()) parseAndUpdateStepUI(dataSets)
            }
            override fun onFailure(code: Int) { Log.e(TAG, "步数数据失败: $code") }
        })
    }

    private fun parseAndUpdateHeartRateUI(dataSets: List<DataSet>) {
        try {
            val latestPoint = dataSets.last().dataPoints.last()
            val heartRateValue = latestPoint.getValue(latestPoint.dataType.elements[0]) as Float
            runOnUiThread { binding.tvHeartRate.text = "${heartRateValue.toInt()} bpm" }
        } catch (e: Exception) { Log.e(TAG, "解析出错: ${e.message}") }
    }

    private fun parseAndUpdateStepUI(dataSets: List<DataSet>) {
        try {
            val latestPoint = dataSets.last().dataPoints.last()
            val stepValue = latestPoint.getValue(latestPoint.dataType.elements[0]) as Float
            runOnUiThread { binding.tvActivity.text = "${stepValue.toInt()} 步" }
        } catch (e: Exception) { Log.e(TAG, "解析出错: ${e.message}") }
    }

    private fun sendRequestToServer() {
        val url = "http://10.157.220.102:8080/api/v1/health/analyze"
        val jsonString = """
            {
              "user_id": "oppo_user_001",
              "timestamp": ${System.currentTimeMillis() / 1000},
              "health_data": {
                "heart_rate": 76,
                "blood_oxygen": 98,
                "activity_level": 13,
                "stress_index": 32,
                "sleep_minutes": 440,
                "ecg_abnormal": false
              }
            }
        """.trimIndent()

        val requestBody = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.tvAiResult.text = "❌ 网络连接失败\n错误: ${e.message}"
                    binding.btnSendAi.isEnabled = true
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    binding.btnSendAi.isEnabled = true
                    binding.tvAiResult.text = if (response.isSuccessful) "✨ 诊断完成：\n$responseData" else "⚠️ 错误：${response.code}"
                }
            }
        })
    }
}