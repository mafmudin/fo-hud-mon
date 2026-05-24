package dev.fordewe.fohudmon

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import dev.fordewe.fohudmon.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val stress = StressTests()
    private val heavyDrawView by lazy { HeavyDrawView(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        } else {
            DevHud.install(application)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stress.stopAll()
        stopFpsJankView()
    }

    private fun setupButtons() {
        binding.btnCpuSingle.setOnClickListener {
            toggle(stress.isCpuSingleRunning, stress::startCpuSingle, stress::stopCpuSingle,
                binding.btnCpuSingle, binding.statusCpuSingle)
        }
        binding.btnCpuMulti.setOnClickListener {
            toggle(stress.isCpuMultiRunning, stress::startCpuMulti, stress::stopCpuMulti,
                binding.btnCpuMulti, binding.statusCpuMulti)
        }
        binding.btnRamAlloc.setOnClickListener {
            if (!stress.isRamAllocRunning) {
                stress.startRamAlloc { markIdle(binding.btnRamAlloc, binding.statusRamAlloc) }
                markRunning(binding.btnRamAlloc, binding.statusRamAlloc)
            } else {
                stress.stopRamAlloc()
                markIdle(binding.btnRamAlloc, binding.statusRamAlloc)
            }
        }
        binding.btnRamLeak.setOnClickListener {
            if (!stress.isRamLeakRunning) {
                stress.startRamLeak { markIdle(binding.btnRamLeak, binding.statusRamLeak) }
                markRunning(binding.btnRamLeak, binding.statusRamLeak)
            } else {
                stress.stopRamLeak()
                markIdle(binding.btnRamLeak, binding.statusRamLeak)
            }
        }
        binding.btnFpsJank.setOnClickListener { toggleFpsJank() }
        binding.btnNetwork.setOnClickListener {
            toggle(stress.isNetworkRunning, stress::startNetwork, stress::stopNetwork,
                binding.btnNetwork, binding.statusNetwork)
        }
        binding.btnStopAll.setOnClickListener {
            stress.stopAll()
            stopFpsJankView()
            resetAllCards()
        }
        binding.btnExportLog.setOnClickListener {
            val file = DevHud.exportLog()
            if (file != null) {
                binding.tvLogPath.text = file.absolutePath
                Toast.makeText(this, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "HUD not installed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggle(
        running: Boolean,
        start: () -> Unit,
        stop: () -> Unit,
        btn: Button,
        status: TextView,
    ) {
        if (!running) {
            start()
            markRunning(btn, status)
        } else {
            stop()
            markIdle(btn, status)
        }
    }

    private fun toggleFpsJank() {
        if (!heavyDrawView.active) {
            (window.decorView as ViewGroup).addView(
                heavyDrawView, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
            heavyDrawView.active = true
            markRunning(binding.btnFpsJank, binding.statusFpsJank)
        } else {
            stopFpsJankView()
            markIdle(binding.btnFpsJank, binding.statusFpsJank)
        }
    }

    private fun stopFpsJankView() {
        if (heavyDrawView.active) {
            heavyDrawView.active = false
            (window.decorView as? ViewGroup)?.removeView(heavyDrawView)
        }
    }

    private fun markRunning(btn: Button, status: TextView) {
        btn.text = "STOP"
        status.text = "● Running"
        status.setTextColor(getColor(android.R.color.holo_green_dark))
    }

    private fun markIdle(btn: Button, status: TextView) {
        btn.text = "START"
        status.text = "Idle"
        status.setTextColor(getColor(android.R.color.darker_gray))
    }

    private fun resetAllCards() {
        listOf(
            binding.btnCpuSingle to binding.statusCpuSingle,
            binding.btnCpuMulti  to binding.statusCpuMulti,
            binding.btnRamAlloc  to binding.statusRamAlloc,
            binding.btnRamLeak   to binding.statusRamLeak,
            binding.btnFpsJank   to binding.statusFpsJank,
            binding.btnNetwork   to binding.statusNetwork,
        ).forEach { (btn, status) -> markIdle(btn, status) }
    }
}
