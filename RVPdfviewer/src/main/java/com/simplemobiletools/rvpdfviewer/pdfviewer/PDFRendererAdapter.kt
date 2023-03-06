package com.simplemobiletools.rvpdfviewer.pdfviewer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.rvpdfviewer.R
import com.simplemobiletools.rvpdfviewer.pdfviewer.PDFRendererAdapter.PDFPageViewHolder
import com.simplemobiletools.rvpdfviewer.pdfviewer.subscaleview.ImageSource
import com.simplemobiletools.rvpdfviewer.pdfviewer.subscaleview.SubsamplingScaleImageView

class PDFRendererAdapter(private val mContext: Context, val onPageClick: () -> Unit = {}) :
    RecyclerView.Adapter<PDFPageViewHolder>() {
    private val mPages: MutableList<PDFPage> = ArrayList()
    fun addPage(page: PDFPage) {
        mPages.add(page)
        notifyDataSetChanged()
    }

    fun close() {
        mPages.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PDFPageViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_pdf_view, parent, false)
        return PDFPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PDFPageViewHolder, position: Int) {
        val page = mPages[position]
        holder.bind(page)
    }

    override fun getItemCount(): Int {
        return mPages.size
    }

    inner class PDFPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(page: PDFPage) {
            (itemView.findViewById<View>(R.id.imageView) as ImageView).setImageBitmap(page.bitmap)
            val scaleImageView: SubsamplingScaleImageView = itemView.findViewById(R.id.subsamplingImageView)
            scaleImageView.setImage(ImageSource.bitmap(page.bitmap))
            scaleImageView.setOnClickListener { v: View? -> onPageClick() }
        }
    }
}
