package dev.fordewe.fohudmon

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.fordewe.fohudmon.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Overlay muncul otomatis via HudInitProvider (debug build only)
    }
}
