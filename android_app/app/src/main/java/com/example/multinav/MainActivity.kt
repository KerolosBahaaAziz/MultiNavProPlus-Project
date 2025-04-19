package com.example.multinav

        import BluetoothService
        import android.os.Bundle
        import androidx.activity.ComponentActivity
        import androidx.activity.compose.setContent
        import androidx.activity.viewModels
        import com.example.multinav.ui.theme.MultiNavTheme

        class MainActivity : ComponentActivity() {
            private val bluetoothService by lazy { BluetoothService(this) }
            private val bluetoothViewModel by viewModels<BluetoothViewModel> {
                BluetoothViewModelFactory(bluetoothService)
            }

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent {
                    MultiNavTheme {
                        Navigation(bluetoothViewModel)
                    }
                }
            }
        }