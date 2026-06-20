package com.example.ringer

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ringer.databinding.ItemAppPickerBinding

class AppPickerAdapter(
    private val apps: List<ApplicationInfo>,
    private val pm: PackageManager,
    private val onAppSelected: (ApplicationInfo) -> Unit
) : RecyclerView.Adapter<AppPickerAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemAppPickerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: ApplicationInfo) {
            binding.appName.text = pm.getApplicationLabel(app)
            binding.appPackage.text = app.packageName

            try {
                binding.appIcon.setImageDrawable(pm.getApplicationIcon(app.packageName))
            } catch (e: Exception) {
                binding.appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            // iOS-like smooth interaction
            binding.root.setOnClickListener {
                binding.root.animate()
                    .scaleX(0.93f).scaleY(0.93f)
                    .setDuration(70)
                    .withEndAction {
                        binding.root.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(160)
                            .start()
                        onAppSelected(app)
                    }
                    .start()
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