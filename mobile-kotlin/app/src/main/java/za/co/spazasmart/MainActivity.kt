package za.co.spazasmart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import za.co.spazasmart.data.repository.SpazaInventoryRepository
import za.co.spazasmart.ui.SpazaSmartApp
import za.co.spazasmart.ui.theme.SpazaSmartTheme

class MainActivity : ComponentActivity() {

    private val inventoryRepository = SpazaInventoryRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpazaSmartTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SpazaSmartApp(repository = inventoryRepository)
                }
            }
        }
    }
}
