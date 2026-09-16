package com.example.moneytracker.ui

import androidx.lifecycle.*
import com.example.moneytracker.data.Transaction
import com.example.moneytracker.data.TransactionRepository
import kotlinx.coroutines.launch

class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {
    val allTransactions: LiveData<List<Transaction>> = repository.allTransactions.asLiveData()

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }

    val totalIncome: LiveData<Long?> = repository.totalIncome.asLiveData()
    val totalExpense: LiveData<Long?> = repository.totalExpense.asLiveData()

    val currentBalance: LiveData<Long> = MediatorLiveData<Long>().apply {
        addSource(totalIncome) { income ->
            value = (income ?: 0L) - (totalExpense.value ?: 0L)
        }
        addSource(totalExpense) { expense ->
            value = (totalIncome.value ?: 0L) - (expense ?: 0L)
        }
    }
}

class TransactionViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
