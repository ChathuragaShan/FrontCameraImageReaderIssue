package com.chathuranga.shan.imagereaderissue

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.chathuranga.shan.imagereaderissue.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val navHostFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.hostFragment)
    }
    private val navigationController by lazy {
        Navigation.findNavController(this, R.id.hostFragment)
    }

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initialization()
    }

    private fun initialization() {
        //NavigationUI.setupActionBarWithNavController(this, navigationController)
    }

    override fun onBackPressed() {
        navigationController.navigateUp()
        onSupportNavigateUp()
    }

}