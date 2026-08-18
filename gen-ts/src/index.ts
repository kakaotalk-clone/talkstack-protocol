// buf generate 로 src/generated 아래에 생성된 타입을 재수출합니다.
// 소비 측은 항상 '@kakaoclone/protocol' 에서만 import 합니다.
//
// ★ `export *` 를 두 번 하면 안 됩니다.
//   ts-proto 는 파일마다 DeepPartial·Exact·MessageFns·protobufPackage 같은
//   헬퍼를 똑같은 이름으로 내보냅니다. 둘 다 별표로 재수출하면 이름이 충돌해
//   TS2308 로 빌드가 깨집니다. 메시지 타입만 골라 내보냅니다.

export * from './generated/ws_frame.js';

export {
  ChatMessage,
  Error,
  MessageType,
  RoomType,
  messageTypeFromJSON,
  messageTypeToJSON,
  roomTypeFromJSON,
  roomTypeToJSON,
} from './generated/common.js';

// events.proto / chat_service.proto / room_service.proto 는 서버 전용이라
// 웹 번들에 포함하지 않습니다.
