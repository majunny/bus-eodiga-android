package kr.buswhere.app.data

import kr.buswhere.app.model.RideRequest
import kotlinx.coroutines.flow.Flow

/** 호출 저장소 계약입니다. Firebase 구현체를 추가해도 UI 코드는 변경되지 않습니다. */
interface RideRequestRepository {
    suspend fun createRequest(request: RideRequest): String

    suspend fun cancelRequest(requestId: String)

    fun observeRequest(requestId: String): Flow<RideRequest>
}

/** Python 배차 서버가 제공해야 하는 최소 API 계약을 문서화한 인터페이스입니다. */
interface DispatchApi {
    suspend fun submitRequest(requestId: String)

    suspend fun cancelDispatch(requestId: String)
}
