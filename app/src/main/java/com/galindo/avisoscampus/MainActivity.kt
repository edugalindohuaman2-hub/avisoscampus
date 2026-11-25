package com.galindo.avisoscampus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.galindo.avisoscampus.data.AvisosRepositoryLocal
import com.galindo.avisoscampus.ui.PantallaAvisos
import com.galindo.avisoscampus.ui.theme.AvisosCampusTheme
import com.galindo.avisoscampus.viewmodel.AvisosViewModel
import com.galindo.avisoscampus.viewmodel.AvisosViewModelFactory

class MainActivity : ComponentActivity() {

    private val repository by lazy {
        AvisosRepositoryLocal(applicationContext)
    }
    private val viewModel: AvisosViewModel by viewModels {
        AvisosViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AvisosCampusTheme {
                PantallaAvisos(viewModel = viewModel)
            }
        }
    }
}
