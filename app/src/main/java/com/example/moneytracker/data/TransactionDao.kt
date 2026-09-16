package com.example.moneytracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Pemasukan'")
    fun getTotalIncome(): Flow<Long?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'Pengeluaran'")
    fun getTotalExpense(): Flow<Long?>
}
