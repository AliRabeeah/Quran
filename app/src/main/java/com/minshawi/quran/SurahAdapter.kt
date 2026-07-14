package com.minshawi.quran

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class SurahAdapter(
    private val context: android.content.Context,
    private val onPlayClick: (Surah) -> Unit,
    private val onDownloadClick: (Surah) -> Unit,
    private val onFavoriteClick: (Surah) -> Unit
) : RecyclerView.Adapter<SurahAdapter.VH>() {

    private var downloadingIds = HashSet<Int>()
    var displayList: List<Surah> = QuranData.surahs

    var currentlyPlaying: Int? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.tvNumber)
        val name: TextView = view.findViewById(R.id.tvName)
        val downloadBtn: ImageButton = view.findViewById(R.id.btnDownload)
        val playBtn: ImageButton = view.findViewById(R.id.btnPlay)
        val favBtn: ImageButton = view.findViewById(R.id.btnFavorite)
        val progress: ProgressBar = view.findViewById(R.id.progressDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_surah, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = displayList.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val surah = displayList[position]
        holder.number.text = surah.paddedNumber
        holder.name.text = "سورة ${surah.arabicName}"

        val downloaded = StorageHelper.isDownloaded(context, surah)
        val isDownloading = downloadingIds.contains(surah.number)

        holder.progress.visibility = if (isDownloading) View.VISIBLE else View.GONE
        holder.downloadBtn.visibility = if (downloaded || isDownloading) View.GONE else View.VISIBLE
        holder.playBtn.visibility = if (downloaded) View.VISIBLE else View.GONE

        val isFav = StorageHelper.isFavorite(context, surah)
        holder.favBtn.setImageResource(if (isFav) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
        holder.favBtn.setOnClickListener {
            onFavoriteClick(surah)
            notifyItemChanged(position)
        }

        val isPlaying = currentlyPlaying == surah.number
        holder.playBtn.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        holder.name.setTextColor(
            ContextCompat.getColor(context, if (isPlaying) R.color.primary else R.color.primaryText)
        )

        holder.downloadBtn.setOnClickListener {
            downloadingIds.add(surah.number)
            notifyItemChanged(position)
            onDownloadClick(surah)
        }
        holder.playBtn.setOnClickListener { onPlayClick(surah) }
    }

    fun markDownloadFinished(surah: Surah) {
        downloadingIds.remove(surah.number)
        val idx = displayList.indexOf(surah)
        if (idx >= 0) notifyItemChanged(idx)
    }

    fun updateList(newList: List<Surah>) {
        displayList = newList
        notifyDataSetChanged()
    }
}
