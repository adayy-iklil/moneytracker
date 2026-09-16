package com.example.moneytracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Long,
    val type: String, // "Pemasukan" or "Pengeluaran"
    val category: String,
    val date: Long
)
