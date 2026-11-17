package com.ttmod.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var web: WebView
    private val prefs by lazy { getSharedPreferences("ttmod", MODE_PRIVATE) }
    private val countries = arrayOf("Украина 🇺🇦", "Германия 🇩🇪", "Польша 🇵🇱", "США 🇺🇸", "Великобритания 🇬🇧", "Франция 🇫🇷")
    private val codes = arrayOf("UA", "DE", "PL", "US", "GB", "FR")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showFeed()
    }

    private fun showFeed() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFF000000.toInt()) }
        val top = TextView(this).apply {
            text = "TT MOD  •  ${prefs.getString("region", "UA")}"
            textSize = 18f; setTextColor(-1); gravity = Gravity.CENTER
            setPadding(12, 14, 12, 14)
        }
        root.addView(top, LinearLayout.LayoutParams(-1, 54))
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            loadUrl("https://www.tiktok.com/")
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(8, 8, 8, 8) }
        bar.addView(Button(this).apply { text = "Главная"; setOnClickListener { web.loadUrl("https://www.tiktok.com/") } }, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(Button(this).apply { text = "Настройки"; setOnClickListener { openSettings() } }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(bar)
        setContentView(root)
    }

    private fun openSettings() {
        val options = arrayOf("🌍 Регион: ${prefs.getString("region", "UA")}", "🎨 Тема: ${prefs.getString("theme", "Тёмная")}", "⚙️ Дополнительные настройки", "ℹ️ О приложении")
        AlertDialog.Builder(this).setTitle("TT MOD — настройки").setItems(options) { _, which ->
            when (which) {
                0 -> chooseRegion()
                1 -> chooseTheme()
                2 -> AlertDialog.Builder(this).setTitle("Дополнительные настройки")
                    .setMessage("Язык интерфейса: системный\nРегион клиента: ${prefs.getString("region", "UA")}\nОткрытие ссылок: внутри приложения\n\nРегион TikTok на сервере может зависеть от SIM, IP и аккаунта.")
                    .setPositiveButton("ОК", null).show()
                3 -> AlertDialog.Builder(this).setTitle("TT MOD 1.0")
                    .setMessage("Неофициальная оболочка для открытия TikTok. Не связана с ByteDance.")
                    .setPositiveButton("ОК", null).show()
            }
        }.setNegativeButton("Закрыть", null).show()
    }

    private fun chooseRegion() {
        val selected = codes.indexOf(prefs.getString("region", "UA")).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle("Регион по умолчанию")
            .setSingleChoiceItems(countries, selected) { dialog, which ->
                prefs.edit().putString("region", codes[which]).apply()
                dialog.dismiss()
                showFeed()
            }.setNegativeButton("Отмена", null).show()
    }

    private fun chooseTheme() {
        val themes = arrayOf("Тёмная", "Светлая", "Как в системе")
        val selected = themes.indexOf(prefs.getString("theme", "Тёмная")).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle("Оформление")
            .setSingleChoiceItems(themes, selected) { dialog, which ->
                prefs.edit().putString("theme", themes[which]).apply()
                dialog.dismiss()
                AlertDialog.Builder(this).setMessage("Настройка сохранена. Перезапусти приложение для применения.")
                    .setPositiveButton("ОК", null).show()
            }.setNegativeButton("Отмена", null).show()
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
