package com.example.ringer

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ringer.databinding.ItemAppPickerBinding

class AppPickerAdapter(
    private var apps: List<ApplicationInfo>,
    private val pm: PackageManager,
    private val onAppSelected: (ApplicationInfo) -> Unit
) : RecyclerView.Adapter<AppPickerAdapter.ViewHolder>() {

    fun updateApps(newApps: List<ApplicationInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemAppPickerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: ApplicationInfo) {
            binding.appName.text = pm.getApplicationLabel(app)
            binding.appPackage.text = app.packageName

            try {
                binding.appIcon.setImageDrawable(pm.getApplicationIcon(app.packageName))
            } catch (e: Exception) {
                binding.appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            binding.root.setOnClickListener {
                onAppSelected(app)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size
}
