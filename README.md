# kakao-protocol

멀티레포 6개가 공유하는 **계약 단일 원천(SSOT)**.
`schema/*.proto` 를 고치면 Java 와 TypeScript 타입이 자동 생성됩니다.

```
schema/*.proto  ──┬──▶ gen-java   ──▶ Maven  com.kakaoclone:protocol
   (사람이        │
    고치는        └──▶ gen-ts     ──▶ npm    @kakaoclone/protocol
    유일한 곳)
common-java     ──────────────────▶ Maven  com.kakaoclone:common
```

| 모듈 | 산출물 | 소비처 |
|---|---|---|
| `gen-java` | protobuf 메시지 + gRPC 스텁 | gateway, chat, api |
| `gen-ts` | TypeScript 타입 | web |
| `common-java` | RedisKeys, LuaScripts, ErrorCode, JwtProvider | gateway, chat, api |

---

## 스키마 구성

| 파일 | 용도 |
|---|---|
| `common.proto` | 여러 스키마가 공유하는 값 타입 (`ChatMessage`, `MessageType`) |
| `ws_frame.proto` | 브라우저 ⇄ Gateway WebSocket 프레임 |
| `chat_service.proto` | Gateway → Chat (gRPC) |
| `room_service.proto` | Chat → API (gRPC) — 방 멤버 조회 |
| `events.proto` | Kafka 이벤트 |

---

## 호환성 규칙 (반드시 지킬 것)

브라우저 클라이언트는 **강제 업데이트가 불가능합니다.** Service Worker 에 캐시된 구버전이
며칠씩 살아 있으므로 서버는 항상 하위 호환을 유지해야 합니다.

| 규칙 | |
|---|---|
| 필드 **추가만** 허용 | 삭제·타입 변경·번호 변경 금지 |
| 삭제해야 하면 `reserved` | `reserved 5;` `reserved "old_field";` 로 번호를 영구 봉인 |
| enum 은 0번을 `*_UNSPECIFIED` 로 | proto3 기본값이 0이라 필수 |
| `WsFrame.v` 로 버전 표기 | 서버는 N-1 버전까지 수용 |
| 필드는 **optional 로 취급** | proto3 는 기본값과 미설정을 구분하지 않음 |

`buf breaking` 으로 CI 에서 자동 검사합니다.

```bash
npx buf breaking --against '.git#branch=main'
```

---

## 직렬화 — JSON 으로 시작해 binary 로 간다

WebSocket 전송은 **Protobuf JSON 매핑**으로 시작합니다.

| 단계 | 형식 | 이유 |
|---|---|---|
| Phase 1~3 | JSON (`JsonFormat.printer()`) | 브라우저 DevTools 에서 프레임이 그대로 읽힘 |
| Phase 4 | binary (`toByteArray()`) | 스키마 변경 없이 페이로드 60~70% 감소 |

`.proto` 는 처음부터 쓰되 직렬화만 나중에 교체하는 구조입니다.
Phase 4 에서 한 줄 바꾸고 대역폭 그래프를 비교하는 것이 학습 포인트입니다.

---

## 로컬 개발 (원격 저장소 없이)

원격이 없으므로 GitHub Packages 대신 `mavenLocal()` 과 `npm link` 를 씁니다.
버전은 `1.0.0-SNAPSHOT` 고정이라 태그 없이 계속 덮어쓸 수 있습니다.

### Java 소비 리포 (gateway / chat / api)

```powershell
# 1) 발행
.\gradlew.bat publishToMavenLocal

# 2) 소비 측 build.gradle.kts 의 repositories 맨 앞에 mavenLocal() 이 있는지 확인
#    implementation("com.kakaoclone:gen-java:1.0.0-SNAPSHOT")
#    implementation("com.kakaoclone:common-java:1.0.0-SNAPSHOT")
```

### TypeScript 소비 리포 (web)

```powershell
cd gen-ts
npm install
npm run build
npm link

cd ..\..\kakao-web
npm link @kakaoclone/protocol
```

> `gen-ts` 는 `buf` 를 npm 의존성으로 가지므로 protoc 를 따로 설치할 필요가 없습니다.

---

## 빌드

```powershell
.\gradlew.bat build              # 코드 생성 + 컴파일 + 테스트
.\gradlew.bat publishToMavenLocal
```

생성된 Java 코드는 `gen-java/build/generated/source/proto/main/` 에 있습니다.

---

## 원격 저장소 등록 시

`.github/workflows/publish.yml` 이 이미 작성되어 있습니다.
원격을 붙인 뒤 태그를 밀면 Maven + npm 이 동시에 배포됩니다.

```bash
git tag v1.0.0 && git push --tags
```

그 시점에 소비 리포의 `mavenLocal()` 을 GitHub Packages 로 바꾸고
버전을 `1.0.0` 처럼 고정하면 됩니다.
