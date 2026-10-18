package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.SafeGateRepository

class SafeGateViewModelFactory(
    private val repository: SafeGateRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SafeGateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SafeGateViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
