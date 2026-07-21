package com.dali951.blackscreen

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsManager
    private lateinit var switchClock: SwitchCompat
    private lateinit var switchDate: SwitchCompat
    private lateinit var switchBattery: SwitchCompat
    private lateinit var switchMedia: SwitchCompat
    private lateinit var spinnerTimeout: Spinner

    private val timeoutValues = longArrayOf(5_000L, 10_000L, 15_000L, 30_000L, 0L)
    private val timeoutLabels = arrayOf(
        "5 seconds", "10 seconds", "15 seconds", "30 seconds", "Until dismissed"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = PrefsManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)
        toolbar.setNavigationOnClickListener { finish() }

        switchClock = findViewById(R.id.switchClock)
        switchDate = findViewById(R.id.switchDate)
        switchBattery = findViewById(R.id.switchBattery)
        switchMedia = findViewById(R.id.switchMedia)
        spinnerTimeout = findViewById(R.id.spinnerTimeout)

        switchClock.isChecked = prefs.showClock
        switchDate.isChecked = prefs.showDate
        switchBattery.isChecked = prefs.showBattery
        switchMedia.isChecked = prefs.showMedia

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            timeoutLabels
        )
        spinnerTimeout.adapter = adapter
        val currentIndex = timeoutValues.indexOf(prefs.aodTimeout)
        spinnerTimeout.setSelection(if (currentIndex >= 0) currentIndex else 1)

        switchClock.setOnCheckedChangeListener { _, checked -> prefs.showClock = checked }
        switchDate.setOnCheckedChangeListener { _, checked -> prefs.showDate = checked }
        switchBattery.setOnCheckedChangeListener { _, checked -> prefs.showBattery = checked }
        switchMedia.setOnCheckedChangeListener { _, checked -> prefs.showMedia = checked }

        spinnerTimeout.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                prefs.aodTimeout = timeoutValues[position]
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
