package com.example.zhimai // ⚠️ 请确保这里是你的实际包名

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.zhimai.databinding.ActivityMainBinding
import com.github.mikephil.charting.components.Legend
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

        // 无论什么模式，先初始化图表外观和下拉刷新逻辑
        setupEcgChart()
        setupSwipeRefresh()

        // 🚀 核心分流：根据 isDemoMode 决定走哪条路
        if (isDemoMode) {
            Log.i(TAG, "📺 当前为演示模式，加载动态模拟数据...")
            loadDemoData() // 加载带随机波动的假数据
        } else {
            Log.i(TAG, "⌚ 当前为真实模式，正在唤醒 OPPO SDK...")
            // 1. 唤醒 OPPO 健康 SDK 引擎
            HeytapHealthApi.init(this)
            HeytapHealthApi.setLoggable(true)
            // 2. 向用户申请读取健康数据的权限
            requestOppoHealthAuth()
        }

        // AI 按钮点击事件
        binding.btnSendAi.setOnClickListener {
            binding.tvAiResult.text = "🔄 融合多维健康数据，智脉 AI 引擎正在深度分析..."
            binding.btnSendAi.isEnabled = false
            sendRequestToServer()
        }
    }

    override fun onResume() {
        super.onResume()
        // 只有在真实模式下，回到页面时才去读真实数据
        if (!isDemoMode) {
            readRealHealthData()
        }
    }

    /**
     * 设置下拉刷新
     */
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light
        )

        binding.swipeRefreshLayout.setOnRefreshListener {
            Log.i(TAG, "用户触发下拉刷新...")

            // 根据模式刷新对应的数据
            if (isDemoMode) {
                loadDemoData() // 演示模式下，下拉会重新生成一次随机数据
            } else {
                readRealHealthData() // 真实模式下，重新去拉取手表数据
            }

            // 模拟同步动画：1.5秒后停止刷新小圆圈
            binding.swipeRefreshLayout.postDelayed({
                if (binding.swipeRefreshLayout.isRefreshing) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this, "数据已同步最新状态", Toast.LENGTH_SHORT).show()
                }
            }, 1500)
        }
    }

    /**
     * 💡 演示模式核心：生成逼真的动态随机数据
     */
    private fun loadDemoData() {
        runOnUiThread {
            // 1. 生成随机心率 (68 到 85 之间)
            val randomHeartRate = (68..85).random()
            binding.tvHeartRate.text = "$randomHeartRate bpm"
            binding.tvHeartRate.setTextColor(Color.parseColor("#E53935"))

            // 2. 生成随机血氧 (97% 到 99% 之间)
            val randomOxygen = (97..99).random()
            binding.tvBloodOxygen.text = "$randomOxygen %"

            // 3. 生成随机运动量 (10% 到 15% 之间)
            val randomActivity = (10..15).random()
            binding.tvActivity.text = "$randomActivity %"

            // 4. 随机压力和睡眠
            val randomStress = (25..35).random()
            binding.tvStress.text = "$randomStress 极佳"
            binding.tvSleep.text = "8小时10分 (深睡极佳)"

            // 5. 让图表也随机波动起来
            generateRandomChartData()
        }
    }

    /**
     * 💡 演示模式核心：为图表生成一段随机的波动波形
     */
    private fun generateRandomChartData() {
        val entries = ArrayList<Entry>()
        // 模拟 15 个时间点的数据波动
        for (i in 0 until 15) {
            // 在 70 到 85 之间随机波动
            val valY = (700..850).random() / 10f
            entries.add(Entry(i.toFloat(), valY))
        }

        val dataSet = LineDataSet(entries, "实时心率趋势")
        dataSet.color = Color.parseColor("#E53935") // 保持红色折线
        dataSet.lineWidth = 3.0f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // 平滑曲线
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#FFF5F5") // 淡红色渐变底
        dataSet.fillAlpha = 60

        binding.ecgChart.data = LineData(dataSet)
        binding.ecgChart.invalidate() // 刷新图表显示
    }

    /**
     * 初始化图表的静态外观
     */
    private fun setupEcgChart() {
        val chart = binding.ecgChart
        chart.setNoDataText("正在初始化健康监测引擎...")
        chart.setNoDataTextColor(Color.parseColor("#95A5A6"))

        // X轴配置
        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false) // 关掉网格线更清爽
        xAxis.textColor = Color.parseColor("#757575")

        // Y轴配置
        val leftAxis = chart.axisLeft
        leftAxis.axisMinimum = 50f
        leftAxis.axisMaximum = 110f
        leftAxis.setDrawGridLines(true)
        leftAxis.gridColor = Color.parseColor("#E0E0E0")
        leftAxis.textColor = Color.parseColor("#757575")

        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.legend.isEnabled = false // 隐藏图例
    }

    // =========================================================================
    // 👇 以下全是你原本的真实数据拉取逻辑，保持不变，保证随时可以切回真实模式 👇
    // =========================================================================

    private fun requestOppoHealthAuth() {
        val scopes = listOf(
            "READ_HEART_RATE",
            "READ_DAILY_ACTIVITY",
            "READ_BLOOD_OXYGEN",
            "READ_SLEEP"
        )

        Log.i(TAG, "正在请求 OPPO 健康授权...")
        HeytapHealthApi.getInstance().authorityApi().request(this, scopes[0], object : HResponse<AuthResult> {
            override fun onSuccess(result: AuthResult?) {
                Log.i(TAG, "✅ OPPO 授权成功！")
                readRealHealthData()
            }

            override fun onFailure(code: Int) {
                Log.e(TAG, "❌ OPPO 授权失败，错误码：$code")
            }
        })
    }

    private fun readRealHealthData() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000L

        // 拉取心率数据
        val heartRateRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE)
            .setTimeRange(startTime, endTime)
            .build()

        HeytapHealthApi.getInstance().dataApi().read(heartRateRequest, object : HResponse<List<DataSet>> {
            override fun onSuccess(dataSets: List<DataSet>?) {
                if (!dataSets.isNullOrEmpty()) {
                    parseAndUpdateHeartRateUI(dataSets)
                }
            }
            override fun onFailure(code: Int) {
                Log.e(TAG, "心率数据拉取失败: $code")
            }
        })

        // 拉取步数数据
        val stepRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_DAILY_ACTIVITY)
            .setTimeRange(startTime, endTime)
            .build()

        HeytapHealthApi.getInstance().dataApi().read(stepRequest, object : HResponse<List<DataSet>> {
            override fun onSuccess(dataSets: List<DataSet>?) {
                if (!dataSets.isNullOrEmpty()) {
                    parseAndUpdateStepUI(dataSets)
                }
            }
            override fun onFailure(code: Int) {
                Log.e(TAG, "步数数据拉取失败: $code")
            }
        })
    }

    private fun parseAndUpdateHeartRateUI(dataSets: List<DataSet>) {
        try {
            val lastDataSet = dataSets.last()
            if (lastDataSet.dataPoints.isNotEmpty()) {
                val latestPoint = lastDataSet.dataPoints.last()
                val heartRateValue = latestPoint.getValue(latestPoint.dataType.elements.get(0)) as Float

                runOnUiThread {
                    binding.tvHeartRate.text = "${heartRateValue.toInt()} bpm"
                    binding.tvHeartRate.setTextColor(Color.parseColor("#E53935"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析心率数据出错: ${e.message}")
        }
    }

    private fun parseAndUpdateStepUI(dataSets: List<DataSet>) {
        try {
            val lastDataSet = dataSets.last()
            if (lastDataSet.dataPoints.isNotEmpty()) {
                val latestPoint = lastDataSet.dataPoints.last()
                val stepValue = latestPoint.getValue(latestPoint.dataType.elements.get(0)) as Float

                runOnUiThread {
                    binding.tvActivity.text = "${stepValue.toInt()} 步"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析步数数据出错: ${e.message}")
        }
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
                    binding.tvAiResult.text = "❌ 网络请求失败！请检查局域网连接。\n错误信息: ${e.message}"
                    binding.btnSendAi.isEnabled = true
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    binding.btnSendAi.isEnabled = true
                    if (response.isSuccessful) {
                        binding.tvAiResult.text = "✨ 诊断完成：\n$responseData"
                    } else {
                        binding.tvAiResult.text = "⚠️ 引擎返回错误，状态码：${response.code}"
                    }
                }
            }
        })
    }
}