package com.example.ringer

import android.view.LayoutInflater
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

            try {
                val icon = itemView.context.packageManager.getApplicationIcon(app.packageName)
                binding.appIcon.setImageDrawable(icon)
            } catch (e: Exception) {
                binding.appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            binding.removeButton.setOnClickListener {
                onRemove(app.packageName)
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
