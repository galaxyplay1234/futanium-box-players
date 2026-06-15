package com.futanium.box.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.futanium.box.MainActivity
import com.futanium.box.PlayerActivity
import com.futanium.box.R
import com.futanium.box.WebViewActivity
import com.futanium.box.databinding.ItemChannelBinding
import com.futanium.box.model.Channel

class ChannelAdapter(
    private val items: MutableList<Channel> = mutableListOf()
) : RecyclerView.Adapter<ChannelAdapter.VH>() {

    inner class VH(
        val binding: ItemChannelBinding
    ) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<Channel>) {
        items.clear()
        items.addAll(
            list.sortedBy {
                it.name?.lowercase()
            }
        )
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {

        return VH(
            ItemChannelBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {

        val channel = items[position]

        holder.binding.tvChannelName.text =
            channel.name ?: ""

        holder.binding.tvChannelLink.text =
            channel.link ?: ""

        holder.binding.tvOnline.text = "🟢 Online"

        holder.binding.tvIcon.text = "▶️"

        holder.itemView.setOnClickListener {

            val url = channel.link ?: return@setOnClickListener

            MainActivity.navigatingInsideApp = true

            if (
                url.endsWith(".m3u8") ||
                url.endsWith(".mp4") ||
                url.endsWith(".ts")
            ) {

                val intent = Intent(
                    it.context,
                    PlayerActivity::class.java
                )

                intent.putExtra(
                    PlayerActivity.EXTRA_URL,
                    url
                )

                intent.putExtra(
                    PlayerActivity.EXTRA_TITLE,
                    channel.name ?: ""
                )

                it.context.startActivity(intent)

            } else {

                val intent = Intent(
                    it.context,
                    WebViewActivity::class.java
                )

                intent.putExtra(
                    WebViewActivity.EXTRA_URL,
                    url
                )

                it.context.startActivity(intent)
            }
        }
    }
}