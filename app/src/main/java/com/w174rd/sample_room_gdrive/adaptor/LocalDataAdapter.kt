package com.w174rd.sample_room_gdrive.adaptor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.w174rd.sample_room_gdrive.databinding.AdapterLocalDataBinding
import com.w174rd.sample_room_gdrive.model.Entity

class LocalDataAdapter : ListAdapter<Entity, LocalDataAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding = AdapterLocalDataBinding.inflate(
            LayoutInflater.from(viewGroup.context), viewGroup, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val data = getItem(position)

        viewHolder.binding.apply {
            txtName.text = data.name
        }
    }

    class ViewHolder(val binding: AdapterLocalDataBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Entity>() {
            override fun areItemsTheSame(oldItem: Entity, newItem: Entity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Entity, newItem: Entity): Boolean {
                return oldItem == newItem
            }
        }
    }
}


