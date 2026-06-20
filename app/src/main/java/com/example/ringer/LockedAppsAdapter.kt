package com.example.ringer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ringer.data.AppInfo
import com.example.ringer.databinding.ItemLockedAppBinding

class LockedAppsAdapter(
    private val onRemove: (String) -> Unit
) : ListAdapter<AppInfo, LockedAppsAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo) = oldItem.packageName == newItem.packageName
        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) = oldItem == newItem
    }

    inner class ViewHolder(private val binding: ItemLockedAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo) {
            binding.appName.text = app.appName
            binding.appPackage.text = app.packageName

            // Try to load app icon
            try {
                val icon = itemView.context.packageManager.getApplicationIcon(app.packageName)
                binding.appIcon.setImageDrawable(icon)
            } catch (e: Exception) {
                binding.appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            binding.removeButton.setOnClickListener {
                // iOS-like haptic feel simulation via scale animation
                binding.removeButton.animate()
                    .scaleX(0.7f).scaleY(0.7f)
                    .setDuration(80)
                    .withEndAction {
                        binding.removeButton.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(120)
                            .start()
                    }
                    .start()

                onRemove(app.packageName)
            }

            // Smooth press effect on the whole card
            binding.root.setOnClickListener {
                binding.root.animate()
                    .scaleX(0.96f).scaleY(0.96f)
                    .setDuration(80)
                    .withEndAction {
                        binding.root.animate()
                            .scaleX(1f).scaleY(1f)
                            .setDuration(180)
                            .start()
                    }
                    .start()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLockedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}