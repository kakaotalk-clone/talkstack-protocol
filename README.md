# talkstack-protocol

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

## 자바 클래스는 어디서 오나 — 커밋된 소스가 없습니다

이 리포에 손으로 쓴 자바 코드는 `common-java` 뿐입니다. `gen-java` 에는 **`src` 디렉터리가
아예 없고** 빌드 설정만 있습니다.

```
schema/ws_frame.proto                          ← git 에 있음 (사람이 씀)
        │
        │  ./gradlew build   →  protoc 실행
        ▼
gen-java/build/generated/.../ws/WsFrame.java   ← git 에 없음 (3,829줄)
        │
        │  ./gradlew publishToMavenLocal
        ▼
~/.m2/.../gen-java-1.0.0-SNAPSHOT.jar          ← 클래스 파일 206개
        │
        ▼
소비 리포에서  import com.kakaoclone.protocol.ws.WsFrame;
```

`message` 하나당 클래스가 **2개**(본체 + `~OrBuilder` 인터페이스) 나오므로,
`.proto` 5개에서 클래스 파일 206개가 만들어집니다.

플러그인이 두 종류입니다.

| 플러그인 | 만드는 것 | 예 |
|---|---|---|
| `protoc` 기본 | 메시지 클래스 | `WsFrame`, `History`, `Ack` |
| `protoc-gen-grpc-java` | **서비스 스텁** | `ChatServiceGrpc` (`Stub` + `ImplBase`) |

`protoc` 를 따로 설치할 필요가 없습니다. Gradle 이 `artifact` 로 지정된 실행 파일을
Maven 저장소에서 받아 씁니다 — 새 PC 에서 클론해도 `./gradlew build` 한 번이면 됩니다.

> IntelliJ 에서 `WsFrame` 에 `Ctrl+B` 를 누르면 `build/generated/...` 로 갑니다.
> **거기서 고치면 다음 빌드에 사라집니다.** 고쳐야 할 것은 항상 `schema/*.proto` 입니다.

`.proto` 를 고쳤는데 다른 리포에 반영이 안 된다면 십중팔구
**`publishToMavenLocal` 을 안 돌린 것**입니다.

## 직렬화 — JSON 과 binary 를 협상합니다

WebSocket 전송은 **핸드셰이크에서 표현을 고릅니다.** 스키마는 그대로이고 표현만 다릅니다.

| 서브프로토콜 | 형식 | 쓰는 곳 |
|---|---|---|
| `talkstack-chat-v1b` | binary (`toByteArray()`) | 현재 웹 클라이언트 |
| `talkstack-chat-v1` | JSON (`JsonFormat.printer()`) | 구버전 클라이언트, 디버깅 |

버전을 v2 로 올리지 않고 접미사 `b` 만 붙인 이유는 **필드가 바뀐 게 아니기 때문**입니다.
v2 로 올리면 잘못된 신호가 됩니다.

서버는 클라이언트가 제안한 순서대로 지원하는 첫 번째를 고르므로, 신·구 클라이언트가
같은 서버에서 공존합니다. 브라우저는 강제 업데이트가 불가능하고 Service Worker 에 캐시된
구버전이 며칠씩 살아 있어서, 서버가 일방적으로 바꿀 수 없습니다.

실측: 같은 SEND 프레임 **177B → 99B (44% 감소)**, 실제 메시지 트래픽에서 44.6%.
하트비트만 비교하면 78% 가 나오는데 **과장입니다** — PING 은 본문이 없어 JSON 이 거의
필드 이름 값(18B)이기 때문입니다.

### 읽을 때 걸리는 것 두 가지

```jsonc
{"v":1,"t":"READ_UPDATED","readUpdated":{"roomId":"42"}}
//                                        minReadSeq 가 0 이면 키 자체가 없음
```

`int64` 는 JSON 에서 **문자열**(`"42"`)이고, proto3 JSON 은 **기본값 필드를 아예 생략**합니다.
클라이언트는 반드시 `Number(x)` / `?? 0` / `?? false` 로 받아야 합니다.
binary 로 바꿔도 같은 매핑을 거치므로 동일하게 적용됩니다.

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

cd ..\..\talkstack-web
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
