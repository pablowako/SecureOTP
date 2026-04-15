package com.otpextractor.secureotp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.otpextractor.secureotp.databinding.ItemAppFilterBinding
import com.otpextractor.secureotp.utils.AppFilterRepository
import com.otpextractor.secureotp.utils.AppFilterState

class AppFilterAdapter(
    private val allApps: List<AppInfo>,
    private val filterRepo: AppFilterRepository
) : RecyclerView.Adapter<AppFilterAdapter.ViewHolder>() {

    private var filteredApps: List<AppInfo> = allApps

    init {
        setHasStableIds(true)
    }

    fun filter(query: String) {
        filteredApps = if (query.isBlank()) {
            allApps
        } else {
            val lower = query.lowercase()
            allApps.filter {
                it.name.lowercase().contains(lower) ||
                    it.packageName.lowercase().contains(lower)
            }
        }
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemAppFilterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemId(position: Int) = filteredApps[position].packageName.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filteredApps[position]
        val b = holder.binding

        b.imgAppIcon.setImageDrawable(app.icon)
        b.tvAppName.text = app.name
        b.tvPackageName.text = app.packageName

        val buttons = listOf(
            b.btnOff to AppFilterState.BLACKLISTED,
            b.btnAuto to AppFilterState.DEFAULT,
            b.btnMax to AppFilterState.WHITELISTED
        )

        val state = filterRepo.getState(app.packageName)
        for ((btn, s) in buttons) {
            styleButton(btn, s == state)
        }

        for ((btn, s) in buttons) {
            btn.setOnClickListener {
                filterRepo.setState(app.packageName, s)
                for ((b2, s2) in buttons) {
                    styleButton(b2, s2 == s)
                }
            }
        }
    }

    private fun styleButton(tv: TextView, selected: Boolean) {
        if (selected) {
            tv.setBackgroundResource(R.drawable.bg_segment_selected)
            tv.setTextColor(Color.WHITE)
        } else {
            tv.setBackgroundResource(R.drawable.bg_segment_unselected)
            tv.setTextColor(ContextCompat.getColor(tv.context, R.color.text_primary))
        }
    }

    override fun getItemCount() = filteredApps.size
}
