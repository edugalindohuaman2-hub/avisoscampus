package com.galindo.avisoscampus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.galindo.avisoscampus.data.AvisosRepository

class AvisosViewModelFactory(
    private val repository: AvisosRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AvisosViewModel::class.java)) {
            return AvisosViewModel(repository) as T
        }
        throw IllegalArgumentException("Clase ViewModel no reconocida: ${modelClass.name}")
    }
}
