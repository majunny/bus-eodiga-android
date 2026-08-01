package kr.buswhere.app.data

import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Firebase 익명 인증과 Render 전송용 ID Token을 관리합니다. */
class FirebaseSession(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    /** 로그인 사용자가 없으면 시연용 익명 계정을 생성합니다. */
    fun ensureSignedIn(onResult: (Result<String>) -> Unit) {
        auth.currentUser?.let { user ->
            onResult(Result.success(user.uid))
            return
        }
        auth.signInAnonymously().addOnCompleteListener { task ->
            val user = task.result?.user
            if (task.isSuccessful && user != null) {
                onResult(Result.success(user.uid))
            } else {
                onResult(Result.failure(task.exception ?: IllegalStateException("Firebase 익명 로그인에 실패했습니다.")))
            }
        }
    }

    /** Render API의 Authorization 헤더에 사용할 Firebase ID Token을 반환합니다. */
    fun getIdToken(onResult: (Result<String>) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(Result.failure(IllegalStateException("Firebase 로그인이 필요합니다.")))
            return
        }
        user.getIdToken(false).addOnCompleteListener { task ->
            val token = task.result?.token
            if (task.isSuccessful && token != null) {
                onResult(Result.success(token))
            } else {
                onResult(Result.failure(task.exception ?: IllegalStateException("Firebase Token 발급에 실패했습니다.")))
            }
        }
    }

    /** 코루틴 기반 API 호출에서 사용할 Firebase ID Token을 반환합니다. */
    suspend fun awaitIdToken(): String = suspendCancellableCoroutine { continuation ->
        fun loadToken() {
            getIdToken { result ->
                if (!continuation.isActive) return@getIdToken
                result.fold(
                    onSuccess = continuation::resume,
                    onFailure = continuation::resumeWithException,
                )
            }
        }

        if (auth.currentUser != null) {
            loadToken()
        } else {
            ensureSignedIn { result ->
                if (!continuation.isActive) return@ensureSignedIn
                result.fold(
                    onSuccess = { loadToken() },
                    onFailure = continuation::resumeWithException,
                )
            }
        }
    }
}
