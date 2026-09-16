package com.example.moneytracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.chip.ChipGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moneytracker.data.AppDatabase
import com.example.moneytracker.data.TransactionRepository
import com.example.moneytracker.ui.TransactionViewModel
import com.example.moneytracker.ui.TransactionViewModelFactory
import java.text.NumberFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: TransactionAdapter
    
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { TransactionRepository(database.transactionDao()) }
    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textSaldo = findViewById<TextView>(R.id.textSaldo)
        val textPemasukan = findViewById<TextView>(R.id.textPemasukan)
        val textPengeluaran = findViewById<TextView>(R.id.textPengeluaran)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerTransaksi)
        val editSearch = findViewById<EditText>(R.id.editSearch)
        val chipGroup = findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupFilter)

        adapter = TransactionAdapter(onItemClick = { transaction ->
            val intent = Intent(this, AddTransactionActivity::class.java).apply {
                putExtra("EXTRA_ID", transaction.id)
                putExtra("EXTRA_TITLE", transaction.title)
                putExtra("EXTRA_AMOUNT", transaction.amount)
                putExtra("EXTRA_TYPE", transaction.type)
                putExtra("EXTRA_CATEGORY", transaction.category)
                putExtra("EXTRA_DATE", transaction.date)
            }
            startActivity(intent)
        })
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Search logic
        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            val type = when (checkedId) {
                R.id.chipIncome -> "Pemasukan"
                R.id.chipExpense -> "Pengeluaran"
                else -> "Semua"
            }
            adapter.filterByType(type)
        }

        // Observe data from ViewModel
        viewModel.allTransactions.observe(this) { transactions ->
            transactions?.let { adapter.setData(it) }
        }
        
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        
        viewModel.totalIncome.observe(this) { income ->
            textPemasukan.text = formatRupiah.format(income ?: 0L)
        }
        
        viewModel.totalExpense.observe(this) { expense ->
            textPengeluaran.text = formatRupiah.format(expense ?: 0L)
        }

        viewModel.currentBalance.observe(this) { balance ->
            textSaldo.text = formatRupiah.format(balance)
            if (balance < 0) {
                textSaldo.setTextColor(ContextCompat.getColor(this, R.color.expense_red))
            } else {
                textSaldo.setTextColor(ContextCompat.getColor(this, R.color.text_white))
            }
        }

        // Swipe to delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val transaction = adapter.getItemAt(position)
                
                androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                    .setTitle("Hapus Transaksi")
                    .setMessage("Apakah Anda yakin ingin menghapus transaksi ini?")
                    .setPositiveButton("Hapus") { _, _ ->
                        viewModel.delete(transaction)
                    }
                    .setNegativeButton("Batal") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .setCancelable(false)
                    .show()
            }
        }).attachToRecyclerView(recyclerView)

        val buttonTambah = findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.buttonTambah)
        buttonTambah.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }
    }
    
    // updateBalance removed as it is now handled by viewModel.currentBalance observer
}
