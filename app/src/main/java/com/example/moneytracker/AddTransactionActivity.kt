package com.example.moneytracker

import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.moneytracker.data.AppDatabase
import com.example.moneytracker.data.Transaction
import com.example.moneytracker.data.TransactionRepository
import com.example.moneytracker.ui.TransactionViewModel
import com.example.moneytracker.ui.TransactionViewModelFactory
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private var tanggalDipilih: Long = System.currentTimeMillis()
    private var currentBalance: Long = 0
    private var transactionId: Int = 0
    private var isEditMode: Boolean = false
    
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { TransactionRepository(database.transactionDao()) }
    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        val editKeterangan = findViewById<EditText>(R.id.editKeterangan)
        val editNominal = findViewById<EditText>(R.id.editNominal)
        val groupTipe = findViewById<RadioGroup>(R.id.groupTipe)
        val spinnerKategori = findViewById<Spinner>(R.id.spinnerKategori)
        val buttonPilihTanggal = findViewById<Button>(R.id.buttonPilihTanggal)
        val textTanggalDipilih = findViewById<TextView>(R.id.textTanggalDipilih)
        val buttonSimpan = findViewById<Button>(R.id.buttonSimpan)

        // Check for edit mode
        if (intent.hasExtra("EXTRA_ID")) {
            isEditMode = true
            transactionId = intent.getIntExtra("EXTRA_ID", 0)
            editKeterangan.setText(intent.getStringExtra("EXTRA_TITLE"))
            editNominal.setText(intent.getLongExtra("EXTRA_AMOUNT", 0).toString())
            
            val type = intent.getStringExtra("EXTRA_TYPE")
            if (type == "Pemasukan") {
                groupTipe.check(R.id.radioPemasukan)
            } else {
                groupTipe.check(R.id.radioPengeluaran)
            }
            
            val category = intent.getStringExtra("EXTRA_CATEGORY")
            val adapter = spinnerKategori.adapter
            for (i in 0 until adapter.count) {
                if (adapter.getItem(i).toString() == category) {
                    spinnerKategori.setSelection(i)
                    break
                }
            }
            
            tanggalDipilih = intent.getLongExtra("EXTRA_DATE", System.currentTimeMillis())
            toolbar.title = "Edit Transaksi"
            buttonSimpan.text = "Perbarui Transaksi"
        }

        textTanggalDipilih.text = formatTanggal(tanggalDipilih)

        viewModel.currentBalance.observe(this) { balance ->
            currentBalance = balance
        }

        buttonPilihTanggal.setOnClickListener {
            pilihTanggalDanJam { millis ->
                tanggalDipilih = millis
                textTanggalDipilih.text = formatTanggal(millis)
            }
        }

        buttonSimpan.setOnClickListener {
            val keterangan = editKeterangan.text.toString().trim()
            val nominalText = editNominal.text.toString().trim()

            if (keterangan.isEmpty() || nominalText.isEmpty()) {
                Toast.makeText(this, "Keterangan & nominal wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nominal = nominalText.toLongOrNull()
            if (nominal == null || nominal <= 0) {
                Toast.makeText(this, "Nominal tidak valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tipe = if (groupTipe.checkedRadioButtonId == R.id.radioPemasukan) {
                "Pemasukan"
            } else {
                "Pengeluaran"
            }

            if (tipe == "Pengeluaran" && nominal > currentBalance) {
                val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                Toast.makeText(this, "Saldo tidak cukup! Saldo saat ini: ${formatRupiah.format(currentBalance)}", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val kategori = spinnerKategori.selectedItem.toString()

            val transaction = Transaction(
                id = transactionId,
                title = keterangan,
                amount = nominal,
                type = tipe,
                category = kategori,
                date = tanggalDipilih
            )
            
            if (isEditMode) {
                viewModel.update(transaction)
                Toast.makeText(this, "Transaksi diperbarui", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.insert(transaction)
                Toast.makeText(this, "Transaksi disimpan", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun pilihTanggalDanJam(onSelesai: (Long) -> Unit) {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(tanggalDipilih)
            .setCalendarConstraints(batasTidakMasaDepan())
            .build()

        datePicker.addOnPositiveButtonClickListener { pilihanTanggalUtc ->
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utc.timeInMillis = pilihanTanggalUtc

            val awal = Calendar.getInstance().apply { timeInMillis = tanggalDipilih }
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(awal.get(Calendar.HOUR_OF_DAY))
                .setMinute(awal.get(Calendar.MINUTE))
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val cal = Calendar.getInstance()
                cal.set(
                    utc.get(Calendar.YEAR),
                    utc.get(Calendar.MONTH),
                    utc.get(Calendar.DAY_OF_MONTH),
                    timePicker.hour,
                    timePicker.minute,
                    0
                )
                cal.set(Calendar.MILLISECOND, 0)

                val sekarang = System.currentTimeMillis()
                if (cal.timeInMillis > sekarang) {
                    onSelesai(sekarang)
                } else {
                    onSelesai(cal.timeInMillis)
                }
            }
            timePicker.show(supportFragmentManager, "pemilih_jam")
        }
        datePicker.show(supportFragmentManager, "pemilih_tanggal")
    }

    private fun batasTidakMasaDepan(): CalendarConstraints {
        return CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()
    }

    private fun formatTanggal(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
