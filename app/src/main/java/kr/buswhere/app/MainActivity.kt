package kr.buswhere.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kr.buswhere.app.ui.Bus어디가App
import kr.buswhere.app.ui.theme.BUS어디가Theme
import kr.buswhere.app.data.FirebaseSession
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FirebaseSession().ensureSignedIn { result ->
            result.onSuccess { uid -> Log.i("BUS어디가", "Firebase connected: ${uid.take(8)}") }
            result.onFailure { error -> Log.e("BUS어디가", "Firebase authentication failed", error) }
        }
        setContent {
            BUS어디가Theme {
                Bus어디가App()
            }
        }
    }
}
