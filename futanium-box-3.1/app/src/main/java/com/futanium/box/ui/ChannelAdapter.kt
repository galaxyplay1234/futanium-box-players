package com.futaniumbox.players.ui

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.futaniumbox.players.MainActivity
import com.futaniumbox.players.PlayerActivity
import com.futaniumbox.players.R
import com.futaniumbox.players.WebViewActivity
import com.futaniumbox.players.databinding.ItemChannelBinding
import com.futaniumbox.players.model.Channel

class ChannelAdapter : RecyclerView.Adapter<ChannelAdapter.VH>() {

    private val items = mutableListOf<Channel>()
    private val allItems = mutableListOf<Channel>()

    inner class VH(
        val binding: ItemChannelBinding
    ) : RecyclerView.ViewHolder(binding.root)


    fun submit(list: List<Channel>) {

    allItems.clear()
    allItems.addAll(list)

    items.clear()
    items.addAll(
        list.sortedBy {
            it.name?.lowercase()
        }
    )

    notifyDataSetChanged()
}

private fun normalize(text: String?): String {

    return java.text.Normalizer.normalize(
        text ?: "",
        java.text.Normalizer.Form.NFD
    )
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        .lowercase()
}

fun filter(query: String) {

    val search = normalize(query)

    items.clear()

    if (search.isBlank()) {

        items.addAll(
            allItems.sortedBy {
                it.name?.lowercase()
            }
        )

    } else {

        items.addAll(
            allItems.filter {

                normalize(it.name).contains(search) ||
                normalize(it.link).contains(search)

            }.sortedBy {
                it.name?.lowercase()
            }
        )
    }

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

        holder.binding.apply {
    tvName.text = channel.name ?: ""
    tvLink.text = channel.link ?: ""
    tvIcon.text = "▶️"
}

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