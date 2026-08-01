# BUS어디가 Android 앱

고령자와 교통약자를 위한 DRT 호출 Android 앱입니다. Kotlin과 Jetpack Compose로 작성되며 앱 ID는 `kr.buswhere.app`입니다.

## 현재 시연 흐름

```text
홈 → 탑승 방식 → 최근 정류장/지도/GPS → 이동 지원 → 목적지
    → 호출 확인 → 배차 대기 → 차량 배정 → 탑승 → 도착
```

GPS는 실제 Android 위치 권한을 요청하고 현재 좌표를 확인합니다. 서울에서 실행해도 GPS 성공 여부를 확인할 수 있지만, 울산 경계 밖이면 호출 위치로 확정하지 않고 최근 정류장 또는 지도 선택을 안내합니다.

지도는 MapLibre Android SDK를 사용합니다. 개발 시에는 OSM 표준 래스터 지도를 표시하고, 운영 시 Render에서 제공할 OSM 스타일 URL로 교체합니다.
지도 스타일 주소가 지정되지 않으면 OSM 표준 래스터 타일을 직접 사용합니다. 이는 해커톤 시연용이며 운영 서비스에서는 자체 타일 또는 허가된 제공자를 사용해야 합니다.

프로젝트 또는 사용자 `gradle.properties`에 다음 값을 추가하면 앱 코드를 변경하지 않고 지도 스타일 서버를 바꿀 수 있습니다.

```properties
MAP_STYLE_URL=https://your-render-map-service.example.com/styles/bus-eodiga/style.json
API_BASE_URL=https://bus-eodiga-api.onrender.com/
```

기본 API 주소는 배포된 Render 서버 `https://bus-eodiga-api.onrender.com/`입니다. 로컬 OSMnx 서버를 Android 에뮬레이터에서 시험할 때는 `API_BASE_URL=http://10.0.2.2:8000/`으로 덮어쓸 수 있습니다. 실제 휴대전화에서 로컬 서버를 사용할 때는 노트북과 같은 Wi-Fi에 연결하고 `API_BASE_URL=http://노트북의-LAN-IP:8000/`으로 빌드해야 합니다.

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
