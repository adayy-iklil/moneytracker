package com.example.moneytracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.data.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private var listTransaksi: List<Transaction> = emptyList(),
    private val onItemClick: (Transaction) -> Unit = {},
    private val onDeleteClick: (Transaction) -> Unit = {}
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var filteredList: List<Transaction> = emptyList()
    private var currentQuery: String = ""
    private var currentType: String = "Semua"

    fun setData(newList: List<Transaction>) {
        listTransaksi = newList
        applyFilters()
    }

    fun filter(query: String) {
        currentQuery = query
        applyFilters()
    }

    fun filterByType(type: String) {
        currentType = type
        applyFilters()
    }

    private fun applyFilters() {
        filteredList = listTransaksi.filter {
            val matchesQuery = it.title.contains(currentQuery, ignoreCase = true) || 
                               it.category.contains(currentQuery, ignoreCase = true)
            val matchesType = if (currentType == "Semua") true else it.type == currentType
            matchesQuery && matchesType
        }
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): Transaction {
        return filteredList[position]
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJudul: TextView = itemView.findViewById(R.id.textJudul)
        val tvNominal: TextView = itemView.findViewById(R.id.textNominal)
        val tvKategori: TextView = itemView.findViewById(R.id.textKategori)
        val tvTanggal: TextView = itemView.findViewById(R.id.textTanggal)
        val imageIcon: ImageView = itemView.findViewById(R.id.imageIcon)
        val btnHapus: ImageView = itemView.findViewById(R.id.buttonHapus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = filteredList[position]

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.btnHapus.setOnClickListener { onDeleteClick(item) }

        holder.tvJudul.text = item.title
        holder.tvKategori.text = item.category

        val iconRes = when (item.category) {
            "Makanan & Minuman" -> android.R.drawable.ic_menu_today
            "Transportasi" -> android.R.drawable.ic_dialog_map
            "Belanja" -> android.R.drawable.ic_menu_set_as
            "Hiburan" -> android.R.drawable.ic_menu_slideshow
            "Kesehatan" -> android.R.drawable.ic_menu_mylocation
            "Pendidikan" -> android.R.drawable.ic_menu_sort_by_size
            "Gaji" -> android.R.drawable.ic_menu_save
            else -> android.R.drawable.ic_menu_agenda
        }
        holder.imageIcon.setImageResource(iconRes)

        val color = if (item.type == "Pemasukan") {
            ContextCompat.getColor(holder.itemView.context, R.color.income_green)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.expense_red)
        }
        holder.tvNominal.setTextColor(color)

        val localeID = Locale("in", "ID")
        val formatRupiah = NumberFormat.getCurrencyInstance(localeID)
        val prefix = if (item.type == "Pemasukan") "+" else "-"
        holder.tvNominal.text = String.format("%s %s", prefix, formatRupiah.format(item.amount))

        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", localeID)
        holder.tvTanggal.text = sdf.format(Date(item.date))
    }

    override fun getItemCount(): Int = filteredList.size
}
