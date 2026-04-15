package com.otpextractor.secureotp

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.otpextractor.secureotp.databinding.ActivityAppFilterBinding
import com.otpextractor.secureotp.utils.AppFilterRepository
import com.otpextractor.secureotp.utils.AppFilterState
import kotlinx.coroutines.*

class AppFilterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppFilterBinding
    private lateinit var filterRepo: AppFilterRepository
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filterRepo = AppFilterRepository(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tvEmpty.visibility = View.VISIBLE
        binding.tvEmpty.text = getString(R.string.app_filter_loading)
        binding.recyclerApps.layoutManager = LinearLayoutManager(this)

        activityScope.launch {
            val apps = withContext(Dispatchers.IO) { loadInstalledApps() }
            binding.tvEmpty.visibility = View.GONE
            binding.bulkActions.visibility = View.VISIBLE
            val adapter = AppFilterAdapter(apps, filterRepo)
            binding.recyclerApps.adapter = adapter

            val allPackages = apps.map { it.packageName }

            binding.btnAllAuto.setOnClickListener { applyBulk(allPackages, AppFilterState.DEFAULT, "All set to Auto", adapter) }
            binding.btnAllOff.setOnClickListener { applyBulk(allPackages, AppFilterState.BLACKLISTED, "All set to Off", adapter) }
            binding.btnAllMax.setOnClickListener { applyBulk(allPackages, AppFilterState.WHITELISTED, "All set to Max", adapter) }

            binding.etSearch.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    adapter.filter(s?.toString() ?: "")
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }

    private fun applyBulk(packages: List<String>, state: AppFilterState, message: String, adapter: AppFilterAdapter) {
        val previous = filterRepo.snapshot(packages)
        filterRepo.setAll(packages, state)
        adapter.notifyDataSetChanged()

        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                filterRepo.restore(previous)
                adapter.notifyDataSetChanged()
            }
            .show()
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val ownPackage = packageName

        // Query apps that have a launcher icon — this gives us every app
        // the user sees in their app drawer, including pre-installed ones
        // like Gmail, WhatsApp, banking apps, etc.
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != ownPackage }
            .map { appInfo ->
                AppInfo(
                    name = pm.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName,
                    icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                )
            }
            .sortedWith(
                compareByDescending<AppInfo> {
                    filterRepo.getState(it.packageName) != AppFilterState.DEFAULT
                }.thenBy { it.name.lowercase() }
            )
    }
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)
