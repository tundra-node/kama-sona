package com.elias.kamasona

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.elias.kamasona.audio.Speaker
import com.elias.kamasona.data.Dict
import com.elias.kamasona.data.Store
import com.elias.kamasona.notify.Reminder
import com.elias.kamasona.ui.CouplesFragment
import com.elias.kamasona.ui.DailyFragment
import com.elias.kamasona.ui.DictionaryFragment
import com.elias.kamasona.ui.SrsFragment
import com.elias.kamasona.ui.TrainerFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    lateinit var store: Store
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = Store(applicationContext)

        // Dictionary is ~130 KB; load off the main thread, then show the tabs.
        val firstLaunch = savedInstanceState == null
        thread {
            Dict.load(applicationContext)
            runOnUiThread { showTabs(firstLaunch) }
        }
        Speaker.init(this)

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101
                )
            }
        }
    }

    private fun showTabs(firstLaunch: Boolean) {
        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            val f: Fragment = when (item.itemId) {
                R.id.nav_dictionary -> DictionaryFragment()
                R.id.nav_srs -> SrsFragment()
                R.id.nav_trainer -> TrainerFragment()
                R.id.nav_daily -> DailyFragment()
                else -> CouplesFragment()
            }
            switchTo(f)
            true
        }
        if (firstLaunch) {
            nav.selectedItemId = R.id.nav_dictionary
        }
    }

    private fun switchTo(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, f)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        Reminder.ensureScheduled(this)
    }

    override fun onDestroy() {
        if (isFinishing) Speaker.release()
        super.onDestroy()
    }
}
