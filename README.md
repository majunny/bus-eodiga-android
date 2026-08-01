# BUS어디가 Android 앱

고령자와 교통약자를 위한 DRT 호출 Android 앱입니다. Kotlin과 Jetpack Compose로 작성되며 앱 ID는 `kr.buswhere.app`입니다.

## 현재 시연 흐름

```text
홈 → GPS/시연 모드/정류장 검색 → 이동 지원 → 목적지
    → 호출 확인 → 배차 대기 → 차량 출발 → 탑승 → 목적지 도착
```

GPS는 실제 Android 위치 권한을 요청하고 현재 좌표를 확인합니다. 서울에서 실행하면 울산 서비스 지역 밖임을 안내하며, 울산 시연 모드 또는 정류장명 검색을 사용합니다. 지도 타일 대신 움직이는 버스 노선 시뮬레이션으로 운행 진행률과 남은 시간을 안정적으로 보여줍니다.

프로젝트 또는 사용자 `gradle.properties`에 다음 값을 추가하면 앱 코드를 변경하지 않고 API 서버를 바꿀 수 있습니다.

```properties
API_BASE_URL=https://bus-eodiga-api.onrender.com/
```

기본 API 주소는 배포된 Render 서버 `https://bus-eodiga-api.onrender.com/`입니다. 로컬 OSMnx 서버를 Android 에뮬레이터에서 시험할 때는 `API_BASE_URL=http://10.0.2.2:8000/`으로 덮어쓸 수 있습니다. 실제 휴대전화에서 로컬 서버를 사용할 때는 노트북과 같은 Wi-Fi에 연결하고 `API_BASE_URL=http://노트북의-LAN-IP:8000/`으로 빌드해야 합니다.

경로 확인은 Render의 `/api/find_nearest`를 사용합니다. 버스 호출과 조회·취소는 Firebase 익명 사용자의 ID Token을 `Authorization: Bearer` 헤더에 담아 `/v1/ride-requests` API로 처리합니다. 호출 생성에는 UUID 기반 `Idempotency-Key`를 사용해 버튼 재시도 시 중복 저장을 방지합니다.

출발 정류장은 앱에 고정하지 않습니다. 울산광역시 정류소 위치 정보 3,616개를 제공하는 Render의 `/v1/bus-stops`와 `/v1/bus-stops/nearby`를 사용합니다. GPS 선택 시 반경 2km의 정류장을 거리순으로 불러와 가장 가까운 정류장을 선택하며, 정류장명 검색으로 변경할 수 있습니다.

서울 등 울산 서비스 지역 밖에서 발표할 때는 탑승 위치 화면의 `울산 시연 모드`를 사용합니다. 위치 입력만 울산역 좌표로 바꾸며 실제 정류소 API, OSM 도로 경로, Firebase 인증, Render 호출과 Firestore 저장은 그대로 사용합니다. 상단 `도움말`에서 일반 사용 순서와 시연 모드 범위를 확인할 수 있습니다.

## Firebase 등록

Firebase Console에서 Android 앱을 추가할 때 패키지명으로 다음 값을 정확히 입력합니다.

```text
kr.buswhere.app
```

다운로드한 `google-services.json`은 `app/google-services.json`에 놓습니다. 현재 Firebase Authentication과 Firestore SDK가 연결되어 있으며 앱 시작 시 익명 로그인을 시도합니다. Firebase Console의 Authentication → 로그인 방법에서 `익명` 제공자를 활성화해야 합니다.

## 빌드

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Firebase와 배포된 Render API를 연결한 실제 기기가 있을 때 다음 통합 테스트로 호출 생성·조회·취소를 한 번에 검증할 수 있습니다. 테스트가 만든 호출은 마지막에 `CANCELLED`로 정리됩니다.

```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=kr.buswhere.app.RenderBackendInstrumentedTest
```
